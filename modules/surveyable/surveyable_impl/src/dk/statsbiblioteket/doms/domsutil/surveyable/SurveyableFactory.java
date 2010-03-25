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

import dk.statsbiblioteket.doms.webservices.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;


/** Factory for getting the surveyable singleton.
 * The choice of singleton is defined by configuration parameter
 * <code>dk.statsbiblioteket.doms.domsutil.surveyable.surveyableClass</code>.
 * Default is dk.statsbiblioteket.doms.domsutil.surveyable.SurveyableCombiner. */
@QAInfo(author = "kfc",
        reviewers = "jrg",
        comment = "",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED)
public class SurveyableFactory {
    /** The package prefix for parameter names. */
    private static final String CONFIGURATION_PACKAGE_NAME
            = "dk.statsbiblioteket.doms.domsutil.surveyable";

    /** Parameter for class for surveyable. */
    public static final String SURVEYABLECLASS_CONFIGURATION_PARAMETER
            = CONFIGURATION_PACKAGE_NAME + ".surveyableClass";

    /** Default implementation class. */
    private static final String DEFAULT_IMPLEMENTATION
            = SurveyableCombiner.class.getName();

    /** Logger for this class. */
    private static Log log = LogFactory.getLog(SurveyableFactory.class);

    /** The surveyable singleton instance. */
    private static Surveyable surveyable;

    /**
     * Get the surveyable singleton instance. As this produces a singleton,
     * a new instance will only be generated on the first call, after this the
     * same instance will be returned. If the configuration that defines the
     * implementing class is changed, though, a new instance of the new class
     * will be produced. This method is synchronized.
     *
     * @return Surveyable singleton instance.
     *
     * @throws SurveyableInstantiationException on trouble instantiating the
     * singleton.
     */
    public static synchronized Surveyable getSurveyable()
            throws SurveyableInstantiationException {
        log.trace("Enter getSurveyable");
        String implementation = ConfigCollection.getProperties().getProperty(
                SURVEYABLECLASS_CONFIGURATION_PARAMETER);
        if (implementation == null || implementation.equals("")) {
            implementation = DEFAULT_IMPLEMENTATION;
        }
        if ((surveyable == null)
                || !surveyable.getClass().getName().equals(implementation)) {
            log.info("Initializing surveyable singleton '"
                    + implementation + "'");
            surveyable = createSurveyable(implementation);
        }
        return surveyable;
    }

    /**
     * Create a surveyable instance. This will create an instance of a
     * surveyable with the given classname, or throw an exception on any trouble
     * doing so.
     *
     * @param implementation The classname of the implementation to initialize
     * @return Surveyable instance.
     *
     * @throws SurveyableInstantiationException on trouble instantiating the
     * surveyable.
     */
    public static Surveyable createSurveyable(String implementation) {
        log.trace("Enter createSurveyable('" + implementation + "')"); 
        try {
            Class surveyableClass = Class.forName(implementation);
            return (Surveyable) surveyableClass.newInstance();
        } catch (Exception e) {
            throw new SurveyableInstantiationException(
                    "Cannot instantiate Surveyable class '"
                    + implementation + "': " + e.getMessage(), e);
        }
    }
}
