/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package forwarder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class EventForwarder {

    private static HashMap<String, EventForwarder> registeredFop = new HashMap<>();

    public static void listenToFOP(FieldOfPlay fop) {
        String fopName = fop.getName();
        if (registeredFop.get(fopName) == null) {
            registeredFop.put(fopName, new EventForwarder(fop));
        }
    }

    private EventBus uiEventBus;
    private EventBus fopEventBus;
    private FieldOfPlay fop;
    private String categoryName;
    private List<Athlete> categoryRankings;
    private boolean wideTeamNames;
    private JsonValue leaders;
    private JsonValue groupAthletes;
    private JsonArray sattempts;
    private JsonArray cattempts;
    private Logger logger = (Logger) LoggerFactory.getLogger(EventForwarder.class);
    private String liftsDone;
    private String attempt;
    private String fullName;
    private String groupName;
    private boolean hidden;
    private Integer startNumber;
    private String teamName;
    private Integer weight;
    private JsonObject translationMap;
    private Integer timeAllowed;
    private final String updateKey = StartupUtils.getStringParam("UPDATEKEY");
    private final String url = StartupUtils.getStringParam("REMOTE");

    public EventForwarder(FieldOfPlay emittingFop) {
        this.fop = emittingFop;

        fopEventBus = fop.getFopEventBus();
        fopEventBus.register(this);

        uiEventBus = fop.getUiEventBus();
        uiEventBus.register(this);

        setTranslationMap();
    }

    public Integer getTimeAllowed() {
        return timeAllowed;
    }

    public JsonObject getTranslationMap() {
        return translationMap;
    }

    @Subscribe
    public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
        Competition competition = Competition.getCurrent();
        computeLeaders(competition);
        computeCurrentGroup(competition);
        pushToRemote();
    }

    @Subscribe
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        Athlete a = e.getAthlete();
        Competition competition = Competition.getCurrent();
        computeCurrentGroup(competition);
        setFullName(a.getFullName());
        setTeamName(a.getTeam());
        setStartNumber(a.getStartNumber());
        String formattedAttempt = formatAttempt(a.getAttemptsDone());
        setAttempt(formattedAttempt);
        setWeight(a.getNextAttemptRequestedWeight());
        setTimeAllowed(e.getTimeAllowed());
        String computedName = fop.getGroup() != null
                ? Translator.translate("Scoreboard.GroupLiftType", fop.getGroup().getName(),
                        (fop.isCjStarted() ? Translator.translate("Clean_and_Jerk")
                                : Translator.translate("Snatch")))
                : "";
        setGroupName(
                computedName);
        pushToRemote();
    }

    protected void setTranslationMap() {
        JsonObject translations = Json.createObject();
        Enumeration<String> keys = Translator.getKeys();
        while (keys.hasMoreElements()) {
            String curKey = keys.nextElement();
            if (curKey.startsWith("Scoreboard.")) {
                translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
            }
        }
        setTranslationMap(translations);
    }

    void setAttempt(String formattedAttempt) {
        this.attempt = formattedAttempt;
    }

    void setFullName(String fullName) {
        this.fullName = fullName;
    }

    void setGroupName(String name) {
        this.groupName = name;
    }

    void setHidden(boolean b) {
        this.hidden = b;
    }

    void setLiftsDone(String formattedDone) {
        this.liftsDone = formattedDone;
    }

    void setStartNumber(Integer integer) {
        this.startNumber = integer;
    }

    void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    void setWeight(Integer weight) {
        this.weight = weight;
    }

    private void computeCurrentGroup(Competition competition) {
        List<Athlete> globalRankingsForCurrentGroup = competition.getGlobalCategoryRankingsForGroup(fop.getGroup());
        int liftsDone = AthleteSorter.countLiftsDone(globalRankingsForCurrentGroup);
        setLiftsDone(Translator.translate("Scoreboard.AttemptsDone", liftsDone));
        setGroupAthletes(getAthletesJson(globalRankingsForCurrentGroup, fop.getLiftingOrder()));
    }

    private void computeLeaders(Competition competition) {
        logger.debug("computeLeaders");
        OwlcmsSession.withFop(fop -> {
            Athlete curAthlete = fop.getCurAthlete();
            if (curAthlete != null && curAthlete.getGender() != null) {
                setCategoryName(curAthlete.getCategory().getName());

                categoryRankings = competition.getGlobalTotalRanking(curAthlete.getGender());
                // logger.debug("rankings for current gender {}
                // size={}",curAthlete.getGender(),globalRankingsForCurrentGroup.size());
                categoryRankings = filterToCategory(curAthlete.getCategory(), categoryRankings);
                // logger.debug("rankings for current category {}
                // size={}",curAthlete.getCategory(),globalRankingsForCurrentGroup.size());
                categoryRankings = categoryRankings.stream().filter(a -> a.getTotal() > 0).collect(Collectors.toList());

                if (categoryRankings.size() > 0) {
                    // null as second argument because we do not highlight current athletes in the leaderboard
                    setLeaders(getAthletesJson(categoryRankings, null));
                } else {
                    // no one has totaled, so we show the snatch stats
                    if (!fop.isCjStarted()) {
                        categoryRankings = Competition.getCurrent()
                                .getGlobalSnatchRanking(curAthlete.getGender());
                        categoryRankings = filterToCategory(curAthlete.getCategory(),
                                categoryRankings);
                        categoryRankings = categoryRankings.stream()
                                .filter(a -> a.getSnatchTotal() > 0).collect(Collectors.toList());
                        if (categoryRankings.size() > 0) {
                            setLeaders(getAthletesJson(categoryRankings, null));
                        } else {
                            // nothing to show
                            setLeaders(Json.createNull());
                        }
                    } else {
                        // nothing to show
                        setLeaders(Json.createNull());
                    }
                }
            }
        });

    }

    private List<Athlete> filterToCategory(Category category, List<Athlete> order) {
        return order
                .stream()
                .filter(a -> category != null && category.equals(a.getCategory()))
                .limit(3)
                .collect(Collectors.toList());
    }

    private String formatAttempt(Integer attemptNo) {
        String translate = Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
        return translate;
    }

    private String formatInt(Integer total) {
        if (total == null || total == 0) {
            return "-";
        } else if (total == -1) {
            return "inv.";// invited lifter, not eligible.
        } else if (total < 0) {
            return "(" + Math.abs(total) + ")";
        } else {
            return total.toString();
        }
    }

    private String formatKg(String total) {
        return (total == null || total.trim().isEmpty()) ? "-"
                : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
    }

    private void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank) {
        String category;
        category = curCat != null ? curCat.getName() : "";
        ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
        ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
        ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
        Integer startNumber = a.getStartNumber();
        ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
        ja.put("category", category != null ? category : "");
        getAttemptsJson(a, liftOrderRank);
        ja.put("sattempts", sattempts);
        ja.put("cattempts", cattempts);
        ja.put("total", formatInt(a.getTotal()));
        ja.put("snatchRank", formatInt(a.getSnatchRank()));
        ja.put("cleanJerkRank", formatInt(a.getCleanJerkRank()));
        ja.put("totalRank", formatInt(a.getTotalRank()));
        ja.put("group", a.getGroup() != null ? a.getGroup().getName() : "");
        boolean notDone = a.getAttemptsDone() < 6;
        String blink = (notDone ? " blink" : "");
        if (notDone) {
            ja.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
        }
    }

    /**
     * @param groupAthletes, List<Athlete> liftOrder
     * @return
     */
    private JsonValue getAthletesJson(List<Athlete> groupAthletes, List<Athlete> liftOrder) {
        JsonArray jath = Json.createArray();
        int athx = 0;
        Category prevCat = null;
        long currentId = (liftOrder != null && liftOrder.size() > 0) ? liftOrder.get(0).getId() : -1L;
        long nextId = (liftOrder != null && liftOrder.size() > 1) ? liftOrder.get(1).getId() : -1L;
        List<Athlete> athletes = groupAthletes != null ? Collections.unmodifiableList(groupAthletes)
                : Collections.emptyList();
        for (Athlete a : athletes) {
            JsonObject ja = Json.createObject();
            Category curCat = a.getCategory();
            if (curCat != null && !curCat.equals(prevCat)) {
                // changing categories, put marker before athlete
                ja.put("isSpacer", true);
                jath.set(athx, ja);
                ja = Json.createObject();
                prevCat = curCat;
                athx++;
            }
            getAthleteJson(a, ja, curCat, (a.getId() == currentId)
                    ? 1
                    : ((a.getId() == nextId)
                            ? 2
                            : 0));
            String team = a.getTeam();
            if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
                logger.trace("long team {}", team);
                setWideTeamNames(true);
            }
            jath.set(athx, ja);
            athx++;
        }
        return jath;
    }

    /**
     * Compute Json string ready to be used by web component template
     *
     * CSS classes are pre-computed and passed along with the values; weights are formatted.
     *
     * @param a
     * @param liftOrderRank2
     * @return json string with nested attempts values
     */
    private void getAttemptsJson(Athlete a, int liftOrderRank) {
        sattempts = Json.createArray();
        cattempts = Json.createArray();
        XAthlete x = new XAthlete(a);
        Integer curLift = x.getAttemptsDone();
        int ix = 0;
        for (LiftInfo i : x.getRequestInfoArray()) {
            JsonObject jri = Json.createObject();
            String stringValue = i.getStringValue();
            boolean notDone = x.getAttemptsDone() < 6;
            String blink = (notDone ? " blink" : "");

            jri.put("goodBadClassName", "narrow empty");
            jri.put("stringValue", "");
            if (i.getChangeNo() >= 0) {
                String trim = stringValue != null ? stringValue.trim() : "";
                switch (Changes.values()[i.getChangeNo()]) {
                case ACTUAL:
                    if (!trim.isEmpty()) {
                        if (trim.contentEquals("-") || trim.contentEquals("0")) {
                            jri.put("goodBadClassName", "narrow fail");
                            jri.put("stringValue", "-");
                        } else {
                            boolean failed = stringValue.startsWith("-");
                            jri.put("goodBadClassName", failed ? "narrow fail" : "narrow good");
                            jri.put("stringValue", formatKg(stringValue));
                        }
                    }
                    break;
                default:
                    if (stringValue != null && !trim.isEmpty()) {
                        String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? (" current" + blink)
                                : (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
                        jri.put("goodBadClassName", "narrow request");
                        if (notDone) {
                            jri.put("className", highlight);
                        }
                        jri.put("stringValue", stringValue);
                    }
                    break;
                }
            }

            if (ix < 3) {
                sattempts.set(ix, jri);
            } else {
                cattempts.set(ix % 3, jri);
            }
            ix++;
        }
    }

    private void pushToRemote() {
        // url = "https://httpbin.org/post";
        HttpURLConnection con = null;
        //OWLCMS_PUBLISHER enables this feature
        if (url == null) {
            logger.trace("url is null, configure OWLCMS_REMOTE in the environment");
            return;
        }
        if (updateKey == null) {
            logger.error("updateKey is null, configure OWLCMS_UPDATEKEY in the environment");
            return;
        }
        
        try {
            URL myurl = new URL(url);
            con = (HttpURLConnection) myurl.openConnection();
            logger.warn("updateKey  {}", updateKey);

            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8)) {
                // false only on first key 
                writeKeyValue("updateKey", updateKey, wr, false);
                // all other keys need true for & joiner.
                writeKeyValue("attempt", attempt, wr, true);
                writeKeyValue("categoryName", categoryName, wr, true);
                writeKeyValue("fullName", fullName, wr, true);
                writeKeyValue("groupName", groupName, wr, true);
                writeKeyValue("hidden", String.valueOf(hidden), wr, true);
                writeKeyValue("startNumber", startNumber != null ? startNumber.toString() : null, wr, true);
                writeKeyValue("teamName", teamName, wr, true);
                writeKeyValue("weight", weight != null ? weight.toString() : null, wr, true);
                writeKeyValue("wideTeamNames", String.valueOf(wideTeamNames), wr, true);
                writeKeyValue("groupAthletes", groupAthletes.toJson(), wr, true);
                writeKeyValue("leaders", leaders.toJson(), wr, true);
                writeKeyValue("liftsDone", liftsDone, wr, true);
                writeKeyValue("translationMap", translationMap.toJson(), wr, true);
                writeKeyValue("timeAllowed", timeAllowed != null ? timeAllowed.toString() : null, wr, true);

            }

            StringBuilder content;

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

            logger.warn("response={}", content.toString());

        } catch (ConnectException c) {
            logger.warn("cannot push: {} {}", url, c.getMessage());
        } catch (IOException e) {
            logger.error(LoggerUtils.stackTrace(e));
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private void setCategoryName(String name) {
        this.categoryName = name;
    }

    private void setGroupAthletes(JsonValue athletesJson) {
        this.groupAthletes = athletesJson;

    }

    private void setLeaders(JsonValue athletesJson) {
        this.leaders = athletesJson;
    }

    private void setTimeAllowed(Integer timeAllowed) {
        this.timeAllowed = timeAllowed;

    }

    private void setTranslationMap(JsonObject translations) {
        this.translationMap = translations;
    }

    private void setWideTeamNames(boolean b) {
        wideTeamNames = b;
    }

    private void writeKeyValue(String key, String value, OutputStreamWriter wr, boolean ampersand)
            throws IOException, UnsupportedEncodingException {
        if (value == null) {
            return;
        }
        if (ampersand) {
            wr.write("&");
        }
        wr.write(key);
        wr.write("=");
        wr.write(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
    }

}