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

import dk.statsbiblioteket.util.qa.QAInfo;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/** REST and SOAP webservice that exposes a surveyable singleton. */
@WebService
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "jrg",
        reviewers = {"kfc"})
@Path("/surveillance/")
public class SurveyableWebservice implements Surveyable {
    /**
     * Get all status messages newer than the given time.
     * An application should use the newest timestamp in the given messages
     * from the last call as input to this method next time it calls it, to
     * ensure not losing messages.
     *
     * @param time Only get messages strictly newer than this timestamp. The
     *             timestamp is measured in milliseconds since 1970-01-01 00:00:00.000Z.
     * @return List of status messages. May be empty, but never null.
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getStatusSince",
                    targetNamespace = "http://surveyable.domsutil.doms.statsbiblioteket.dk/",
                    className = "dk.statsbiblioteket.doms.domsutil.surveyable.GetStatusSince")
    @ResponseWrapper(localName = "getStatusSinceResponse",
                     targetNamespace = "http://surveyable.domsutil.doms.statsbiblioteket.dk/",
                     className = "dk.statsbiblioteket.doms.domsutil.surveyable.GetStatusSinceResponse")
    @GET
    @Path("getStatusSince/{time}")
    @Produces("application/xml")
    public Status getStatusSince(
            @WebParam(name = "arg0", targetNamespace = "") @PathParam("{time}") long time) {
        return SurveyableFactory.getSurveyable().getStatusSince(time);
    }

    /**
     * Get all status messages. This behaves exactly like
     * getMessagesSince(0L).
     *
     * @return List of status messages. May be empty, but never null.
     *
     * @see #getStatusSince(long)
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getStatus",
                    targetNamespace = "http://surveyable.domsutil.doms.statsbiblioteket.dk/",
                    className = "dk.statsbiblioteket.doms.domsutil.surveyable.GetStatus")
    @ResponseWrapper(localName = "getStatusResponse",
                     targetNamespace = "http://surveyable.domsutil.doms.statsbiblioteket.dk/",
                     className = "dk.statsbiblioteket.doms.domsutil.surveyable.GetStatusResponse")
    @GET
    @Path("getStatus")
    @Produces("application/xml")
    public Status getStatus() {
        return SurveyableFactory.getSurveyable().getStatus();
    }
}
