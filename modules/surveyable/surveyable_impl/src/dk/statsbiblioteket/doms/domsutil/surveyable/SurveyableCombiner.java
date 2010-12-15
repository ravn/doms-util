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

package dk.statsbiblioteket.doms.domsutil.surveyable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.webservices.configuration.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Combines several Surveyable classes into one.
 * The classes to combine are controlled by the parameter
 * dk.statsbiblioteket.doms.domsutil.surveyable.surveyables
 * which is list ;-separated list of factories that produce surveyables.
 */
@QAInfo(author = "kfc",
        reviewers = "jrg",
        state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class SurveyableCombiner implements Surveyable {
    /** List of surveyables to combine into one. */
    private final List<Surveyable> surveyables
            = Collections.synchronizedList(new ArrayList<Surveyable>());

    /** Prefix for configuration parameters. */
    private static final String CONFIGURATION_PACKAGE_NAME
            = "dk.statsbiblioteket.doms.domsutil.surveyable";

    /** Parameter for classes to use as surveyables. Separated by ;. */
    private static final String CONFIGURATION_SURVEYABLES_PARAMETER =
            CONFIGURATION_PACKAGE_NAME + ".surveyables";

    /** Log for this class. */
    private Log log = LogFactory.getLog(getClass());

    /**
     * Initialise surveyables.
     */
    public SurveyableCombiner() {
        log.trace("Enter SurveyableCombiner()");
    }

    /**
     * Read configuration for classes to survey, and initialise the classes.
     * This will reread the configuration on each call, and initialise any new
     * classes, or any classes the failed to initialise during the last call.
     * Classes that are no longer configured, will be removed from the list of
     * surveyed classes. Errors in initialisation will be logged as errors only
     * the first time the initialisation fails.
     */
    private void initializeSurveyables() {
        log.trace("Enter initializeSurveyables()");

        synchronized (surveyables) {
            Set<String> configuredClasses = new HashSet<String>();
            Set<String> surveyedClasses = new HashSet<String>();
            Set<String> dummies = new HashSet<String>();
            Set<String> newClassesToSurvey;
            Set<String> noLongerSurveyed;
            Iterator<Surveyable> i;

            Properties config = ConfigCollection.getProperties();
            String classes
                    = config.getProperty(CONFIGURATION_SURVEYABLES_PARAMETER);
            List<String> configuredClassesParameter;
            log.trace("Read configuration: '" + classes + ".");

            // Get set of classes from configuration
            if (classes == null) {
                classes = "";
            }
            configuredClassesParameter = Arrays.asList(classes.split(";"));
            for (String configuredClass : configuredClassesParameter) {
                configuredClasses.add(configuredClass.trim());
            }
            configuredClasses.remove("");
            configuredClasses.remove(NoSurveyable.class.getName());

            // Get set of classes initialised.
            for (Surveyable s : surveyables) {
                surveyedClasses.add(s.getClass().getName());
            }

            // If configuration is empty, warn the first time and insert dummy
            if (configuredClasses.size() == 0) {
                if (!surveyedClasses.contains(NoSurveyable.class.getName())
                        || (surveyedClasses.size() != 1)) {
                    log.warn("No classes specified for surveillance.");
                    surveyables.clear();
                    surveyables.add(new NoSurveyable());
                }
                return;
            }

            // Remove and remember dummies. Dummies are remembered in order to
            // log errors only the first time initialisations failed.
            i = surveyables.iterator();
            while (i.hasNext()) {
                Surveyable s = i.next();
                if (s.getClass().getName().equals(
                        NoSurveyable.class.getName())) {
                    i.remove();
                    dummies.add(s.getStatus().getName());
                }
            }
            surveyedClasses.remove(NoSurveyable.class.getName());

            // Initialise newly configured classes, or previously failed. Insert
            // dummy on failure.
            newClassesToSurvey = new HashSet<String>(configuredClasses);
            newClassesToSurvey.removeAll(surveyedClasses);
            for (String classname : newClassesToSurvey) {
                log.info("Initializing class '" + classname
                        + "' for surveillance");
                Surveyable surveyable;
                try {
                    surveyable = SurveyableFactory.createSurveyable(classname);
                } catch (Exception e) {
                    if (dummies.contains(classname)) {
                        log.debug("Still unable to initialise class for"
                                + " surveillance: '" + classname + "'", e);
                    } else {
                        log.error("Unable to initialise class for surveillance:"
                                + " '" + classname + "'", e);
                    }
                    surveyable = new NoSurveyable(classname);
                }
                surveyables.add(surveyable);
            }

            // Remove classes to no longer survey
            noLongerSurveyed = new HashSet<String>(surveyedClasses);
            noLongerSurveyed.removeAll(configuredClasses);
            for (String classname : noLongerSurveyed) {
                log.debug("Removing class '" + classname
                        + "' from surveillance");
                i = surveyables.iterator();
                while (i.hasNext()) {
                    Surveyable s = i.next();
                    if (s.getClass().getName().equals(classname)) {
                        i.remove();
                        log.info("Removed class '" + classname
                                + "' from surveillance");
                    }
                }
            }
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

        try {
            Status status = new Status();
            SortedSet<StatusMessage> messages = new TreeSet<StatusMessage>(
                new StatusMessageComparator());

            initializeSurveyables();
            synchronized (surveyables) {
                if (surveyables == null || surveyables.size() == 0) {
                    return getConfigurationErrorStatus("");
                }

                for (Surveyable surveyable : surveyables) {
                    messages.addAll(
                            surveyable.getStatusSince(time).getMessages());
                }
                status.setName(surveyables.get(0).getStatus().getName());
            }
            status.getMessages().addAll(messages);
            return status;
        } catch (Exception e) {
            log.trace("Survey Configuration error", e);
            return getConfigurationErrorStatus(": " + e);
        }
    }

    /**
     * Create status indicating configuration error.
     *
     * @param details Extra information to append to status message. Use empty
     * string for no further details.
     * @return A Status indicating configuration errors.
     */
    private Status getConfigurationErrorStatus(String details) {
        log.trace("Enter getConfigurationErrorStatus('" + details + "')");

        StatusMessage statusMessage = new StatusMessage();
        Status status = new Status();

        statusMessage.setMessage("Survey configuration error" + details);
        statusMessage.setLogMessage(false);
        statusMessage.setTime(System.currentTimeMillis());
        statusMessage.setSeverity(Severity.RED);

        status.setName("Survey configuration error");
        status.getMessages().add(statusMessage);

        return status;
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
