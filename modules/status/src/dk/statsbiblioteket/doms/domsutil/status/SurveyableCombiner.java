/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package dk.statsbiblioteket.doms.domsutil.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.webservices.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Combines several Surveyable classes into one.
 * The classes to combine are controlled by the parameter
 * dk.statsbiblioteket.doms.domsutil.status.surveyables
 * which is list ;-separated list of factories that produce surveyables.
 */
@QAInfo(author = "kfc",
        reviewers = "jrg",
        state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class SurveyableCombiner implements Surveyable {
    /** List of surveyables to combine into one. */
    private List<Surveyable> surveyables;

    /** Prefix for configuration parameters. */
    private static final String CONFIGURATION_PACKAGE_NAME
            = "dk.statsbiblioteket.doms.domsutil.status";

    /** Parameter for classes to use as surveyables. Separated by ;. */
    private static final String CONFIGURATION_SURVEYABLES_PARAMETER =
            CONFIGURATION_PACKAGE_NAME + ".surveyables";

    /** Log for this class. */
    private Log log = LogFactory.getLog(getClass());

    /**
     * Read configuration for classes to survey, and initialise the classes, to
     * expose them as one surveyable.
     */
    public SurveyableCombiner() {
        log.trace("Enter SurveyableCombiner()");
        Properties config = ConfigCollection.getProperties();
        String classes = config
                .getProperty(CONFIGURATION_SURVEYABLES_PARAMETER);
        if (classes != null && classes.trim().length() != 0) {
            surveyables = new ArrayList<Surveyable>();
            for (String classname : classes.split(";")) {
                log.debug("Initialising class '" + classname
                        + "' for surveillance");
                Surveyable surveyable;
                try {
                    surveyable = SurveyableFactory.createSurveyable(classname);
                } catch (Exception e) {
                    log.error("Unable to initialise class for surveillance: '"
                            + classname + "'", e);
                    surveyable = new NoSurveyable(classname);
                }
                surveyables.add(surveyable);
            }
        } else {
            log.warn("No classes specified for surveillance.");
            surveyables = Collections
                    .singletonList((Surveyable) new NoSurveyable());
        }
    }

    /**
     * Get the combined list of all status messages newer than the given time.
     * An application should use the newest timestamp in the given messages
     * from the last call as input to this method next time it calls it, to
     * ensure not losing messages.
     *
     * Note, the name in the returned status is the name from the first
     * surveyable in the list of combined status objects.
     *
     * @param time Only get messages strictly newer than this timestamp. The
     *             timestamp is measured in milliseconds since
     *             1970-01-01 00:00:00.000Z.
     * @return List of status messages. May be empty, but never null. If no
     *         surveyables are in the list to combine, will return a list of
     *         just one message, which is a message about not being properly
     *         initialised.
     */
    public Status getStatusSince(long time) {
        log.trace("Enter getStatusSince(" + time + ")");
        if (surveyables == null || surveyables.size() == 0) {
            return new Status("Survey configuration error",
                              Collections.singletonList(new StatusMessage(
                                      "Survey configuration error",
                                      StatusMessage.Severity.RED,
                                      System.currentTimeMillis(), false)));
        }
        SortedSet<StatusMessage> messages = new TreeSet<StatusMessage>(
                new StatusMessageComparator());
        for (Surveyable surveyable : surveyables) {
            messages.addAll(surveyable.getStatusSince(time).getMessages());
        }
        return new Status(surveyables.get(0).getStatus().getName(),
                          new ArrayList(messages));
    }

    /**
     * Get all status messages. This behaves exactly like
     * getMessagesSince(0L).
     *
     * @return List of status messages. May be empty, but never null.
     *
     * @see #getStatusSince(long)
     */
    public Status getStatus() {
        log.trace("Enter getStatus()");
        return getStatusSince(0L);
    }

    /** Comparator that orders status messages with regard to date. */
    private static class StatusMessageComparator
            implements Comparator<StatusMessage> {
        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.<p>
         *
         * This comparator is inconsistent with equals.
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         *         first argument is less than, equal to, or greater than the
         *         second.
         */
        public int compare(StatusMessage o1, StatusMessage o2) {
            return (o1.getTime() == o2.getTime() ? 0
                    : (o1.getTime() < o2.getTime() ? -1 : 1));
        }
    }
}
