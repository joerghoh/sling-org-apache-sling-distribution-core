/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.distribution.impl;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.agent.spi.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.apache.sling.distribution.common.DistributionException;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of Distributor interface that dispatches the request to available agents.
 */
@Component(service=Distributor.class)
public class DefaultDistributor implements Distributor {

    private final Logger log = LoggerFactory.getLogger(getClass());


    @Reference
    private DistributionComponentProvider componentProvider;

    @NotNull
    public DistributionResponse distribute(@NotNull String agentName, @NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest) {
        DistributionAgent agent = componentProvider.getService(DistributionAgent.class, agentName);

        if (agent == null) {
            return new SimpleDistributionResponse(DistributionRequestState.NOT_EXECUTED, "Agent is not available");
        }

        try {
            return agent.execute(resourceResolver, distributionRequest);
        } catch (DistributionException e) {
            log.error("cannot execute", e);
            return new SimpleDistributionResponse(DistributionRequestState.DROPPED, "Cannot execute request " + e.getMessage());
        }
    }
}
