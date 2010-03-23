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

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Collections;

@QAInfo(author = "kfc",
        reviewers = "jrg",
        comment = "",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED)
/**
 * A surveyable instance that signals survey is not initialised.
 */
public class NoSurveyable implements Surveyable {

    /**
     * This implementation always returns a status that signals uninitialised
     * configuration.
     *
     * @param time Ignored.
     * @return A single status message reporting unintialised configuration.
     */
    public Status getStatusSince(long time) {
        return new Status("Unconfigured", Collections.singletonList(
                new StatusMessage("Surveillance has not been configured",
                                  StatusMessage.Severity.RED,
                                  System.currentTimeMillis(), false)));
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
        return getStatusSince(0L);
    }
}
