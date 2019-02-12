/*
 * Copyright 2009-2012, Jean-François Lamy
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.data.lifterSort;

import java.util.Comparator;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.lifterSort.LifterSorter.Ranking;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * This comparator sorts athletes within their team
 * 
 * @author jflamy
 * 
 */
public class TeamRankingComparator extends AbstractLifterComparator implements Comparator<Athlete> {
    final private static Logger logger = (Logger) LoggerFactory.getLogger(TeamRankingComparator.class);

    private Ranking rankingType;

    TeamRankingComparator(Ranking rankingType) {
        this.rankingType = rankingType;
    }

    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        compare = compareClub(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareGender(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = comparePointsOrder(lifter1, lifter2);
        if (compare != 0)
            return -compare;

        return compare;
    }

    /**
     * @param lifter1
     * @param lifter2
     * @return
     */
    private int comparePointsOrder(Athlete lifter1, Athlete lifter2) {
        switch (rankingType) {
        case SNATCH:
            return lifter1.getSnatchPoints().compareTo(lifter2.getSnatchPoints());
        case CLEANJERK:
            return lifter1.getCleanJerkPoints().compareTo(lifter2.getCleanJerkPoints());
        case TOTAL:
            final Float totalPoints1 = lifter1.getTotalPoints();
            final Float totalPoints2 = lifter2.getTotalPoints();
            final int compareTo = totalPoints1.compareTo(totalPoints2);
            logger.trace(lifter1 + " " + totalPoints1 + " [" + compareTo + "]" + lifter2 + " " + totalPoints2);
            return compareTo;
        case COMBINED:
            final Float combinedPoints1 = lifter1.getCombinedPoints();
            final Float combinedPoints2 = lifter2.getCombinedPoints();
            final int compareCombined = combinedPoints1.compareTo(combinedPoints2);
            logger.trace(lifter1 + " " + combinedPoints1 + " [" + compareCombined + "]" + lifter2 + " " + combinedPoints2);
            return compareCombined;
		default:
			break;
        }

        return 0;
    }

}
