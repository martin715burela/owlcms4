/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.athlete;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class AthleteRepository.
 */
public class AthleteRepository {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteRepository.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /**
     * Count filtered.
     *
     * @param lastName    the last name
     * @param group       the group
     * @param ageDivision the age division
     * @param weighedIn   the weighed in
     * @return the int
     */
    public static int countFiltered(String lastName, Group group, Category category, AgeGroup ageGroup, AgeDivision ageDivision,
            Boolean weighedIn) {
        return JPAService.runInTransaction(em -> {
            return doCountFiltered(lastName, group, category, ageGroup, ageDivision, weighedIn, em);
        });
    }

    /**
     * Delete an athlete
     * 
     * @param Athlete the athlete
     */
    public static void delete(Athlete Athlete) {
        JPAService.runInTransaction(em -> {
            em.remove(getById(Athlete.getId(), em));
            return null;
        });
    }

    public static Integer doCountFiltered(String lastName, Group group, Category category, AgeGroup ageGroup, AgeDivision ageDivision,
            Boolean weighedIn, EntityManager em) {
        String selection = filteringSelection(lastName, group, category, ageGroup, ageDivision, weighedIn);
        String qlString = "select count(a.id) from Athlete a " + selection;
        logger.trace("count query = {}", qlString);
        Query query = em.createQuery(qlString);
        setFilteringParameters(lastName, group, category, ageGroup, ageDivision, query);
        int i = ((Long) query.getSingleResult()).intValue();
        return i;
    }

    @SuppressWarnings("unchecked")
    public static List<Athlete> doFindAll(EntityManager em) {
        return em.createQuery("select a from Athlete a").getResultList();
    }

    public static List<Athlete> doFindAllByGroupAndWeighIn(EntityManager em, Group group, Boolean weighedIn) {
        return doFindFiltered(em, (String) null, group, (Category) null, (AgeGroup)null, (AgeDivision) null, weighedIn, -1, -1);
    }

    public static List<Athlete> doFindFiltered(EntityManager em, String lastName, Group group, Category category, AgeGroup ageGroup,
            AgeDivision ageDivision, Boolean weighedIn, int offset, int limit) {
        String qlString = "select a from Athlete a"
                + filteringSelection(lastName, group, category, ageGroup, ageDivision, weighedIn);
        logger.debug("find query = {}", qlString);
        Query query = em.createQuery(qlString);
        setFilteringParameters(lastName, group, category, ageGroup, ageDivision, query);
        if (offset >= 0) {
            query.setFirstResult(offset);
        }
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        @SuppressWarnings("unchecked")
        List<Athlete> resultList = query.getResultList();
        return resultList;
    }

    private static String filteringJoins(Group group, Category category, AgeGroup ageGroup, AgeDivision ageDivision) {
        List<String> fromList = new LinkedList<>();
        if (group != null) {
            fromList.add("join a.group g"); // group is via a relationship, join on id
        }
        if (category != null || ageGroup != null || ageDivision != null) {
            fromList.add("join a.category c"); // group is via a relationship, join on id
        }
        if (fromList.size() == 0) {
            return "";
        } else {
            return String.join(" ", fromList);
        }
    }

    private static String filteringSelection(String lastName, Group group, Category category, AgeGroup ageGroup, AgeDivision ageDivision,
            Boolean weighedIn) {
        String joins = filteringJoins(group, category, ageGroup, ageDivision);
        String where = filteringWhere(lastName, group, category, ageGroup, ageDivision, weighedIn);
        String selection = (joins != null ? " " + joins : "") + (where != null ? " where " + where : "");
        return selection;
    }

    private static String filteringWhere(String lastName, Group group, Category category, AgeGroup ageGroup, AgeDivision ageDivision,
            Boolean weighedIn) {
        List<String> whereList = new LinkedList<>();
        if (ageGroup != null) {
            whereList.add("c.ageGroup = :ageGroup");
        }
        if (ageDivision != null) {
            whereList.add("c.ageGroup.ageDivision = :division");
        }
        if (group != null) {
            whereList.add("g.id = :groupId"); // group is via a relationship, select the joined id.
        }
        if (category != null) {
            whereList.add("c.id = :categoryId"); // category is via a relationship, select the joined id.
        }
        if (lastName != null && lastName.trim().length() > 0) {
            whereList.add("lower(a.lastName) like :lastName");
        }
        if (weighedIn != null) {
            whereList.add(weighedIn ? "a.bodyWeight > 0" : "(a.bodyWeight is null) OR (a.bodyWeight <= 0.1)");
        }
        if (whereList.size() == 0) {
            return null;
        } else {
            String join = String.join(" and ", whereList);
            return join;
        }
    }

    /**
     * @return the list of all athletes
     */

    public static List<Athlete> findAll() {
        return JPAService.runInTransaction(em -> doFindAll(em));
    }

    /**
     * Find all by group and weigh in.
     *
     * @param group     the group
     * @param weighedIn the weighed in
     * @return the list
     */
    public static List<Athlete> findAllByGroupAndWeighIn(Group group, Boolean weighedIn) {
        List<Athlete> findFiltered = findFiltered((String) null, group, (Category) null, (AgeGroup)null, (AgeDivision) null, weighedIn,
                -1, -1);
        logger.debug("findFiltered found {}", findFiltered.size());
        return findFiltered;
    }

    public static Athlete findById(long id) {
        return JPAService.runInTransaction(em -> {
            return getById(id, em);
        });
    }

    /**
     * Find filtered.
     *
     * @param lastName    the last name
     * @param group       the group
     * @param ageDivision the age division
     * @param weighedIn   if weighed (bodyweight > 0)
     * @param offset      the offset
     * @param limit       the limit
     * @return the list
     */
    public static List<Athlete> findFiltered(String lastName, Group group, Category category, AgeGroup ageGroup, AgeDivision ageDivision,
            Boolean weighedIn, int offset, int limit) {
        return JPAService.runInTransaction(em -> {
            return doFindFiltered(em, lastName, group, category, ageGroup, ageDivision, weighedIn, offset, limit);
        });
    }

    /**
     * Gets the by id.
     *
     * @param id the id
     * @param em the em
     * @return the by id
     */
    @SuppressWarnings("unchecked")
    public static Athlete getById(Long id, EntityManager em) {
        Query query = em.createQuery("select a from Athlete a where a.id=:id");
        query.setParameter("id", id);

        return (Athlete) query.getResultList().stream().findFirst().orElse(null);
    }

    /**
     * Save an athlete
     *
     * @param athlete the athlete
     * @return the athlete
     */
    public static Athlete save(Athlete athlete) {
        return JPAService.runInTransaction((em) -> {
            return em.merge(athlete);
        });
    }

    private static void setFilteringParameters(String lastName, Group group, Category category, AgeGroup ageGroup, AgeDivision ageDivision,
            Query query) {
        if (lastName != null && lastName.trim().length() > 0) {
            // starts with
            query.setParameter("lastName", lastName.toLowerCase() + "%");
        }
        if (group != null) {
            query.setParameter("groupId", group.getId()); // group is via a relationship, we join and select on id.
        }
        if (category != null) {
            query.setParameter("categoryId", category.getId()); // category is via a relationship, we join and select on
                                                                // id.
        }
        if (ageGroup != null) {
            query.setParameter("ageGroup", ageGroup);
        }
        if (ageDivision != null) {
            query.setParameter("division", ageDivision);
        }
    }

}
