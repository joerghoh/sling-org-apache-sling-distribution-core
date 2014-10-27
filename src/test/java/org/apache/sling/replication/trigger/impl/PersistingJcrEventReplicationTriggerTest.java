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
package org.apache.sling.replication.trigger.impl;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;
import javax.jcr.security.Privilege;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.trigger.ReplicationTriggerRequestHandler;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.trigger.impl.PersistingJcrEventReplicationTrigger}
 */
public class PersistingJcrEventReplicationTriggerTest {

    @Test
    public void testProcessEventWithoutPrivileges() throws Exception {
        String serviceName = "serviceId";
        Session session = mock(Session.class);
        SlingRepository repository = mock(SlingRepository.class);
        when(repository.loginService(serviceName, null)).thenReturn(session);
        String path = "/some/path";
        String nuggetsPath = "/var/nuggets";
        PersistingJcrEventReplicationTrigger persistingJcrEventReplicationTrigger = new PersistingJcrEventReplicationTrigger(
                repository, path, serviceName, nuggetsPath);
        Event event = mock(Event.class);
        ReplicationRequest replicationRequest = persistingJcrEventReplicationTrigger.processEvent(event);
        assertNull(replicationRequest);
    }

    @Test
    public void testProcessEventWithPrivileges() throws Exception {
        String nuggetsPath = "/var/nuggets";
        String serviceName = "serviceId";
        Session session = mock(Session.class);
        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        when(session.getWorkspace()).thenReturn(workspace);
        when(session.hasPermission(nuggetsPath, Privilege.JCR_ADD_CHILD_NODES)).thenReturn(true);

        SlingRepository repository = mock(SlingRepository.class);
        when(repository.loginService(serviceName, null)).thenReturn(session);

        String path = "/some/path";
        PersistingJcrEventReplicationTrigger persistingJcrEventReplicationTrigger = new PersistingJcrEventReplicationTrigger(
                repository, path, serviceName, nuggetsPath);
        ReplicationTriggerRequestHandler handler = mock(ReplicationTriggerRequestHandler.class);
        persistingJcrEventReplicationTrigger.register("handler-id", handler);

        Node nuggetsNode = mock(Node.class);
        Node eventNode = mock(Node.class);
        when(nuggetsNode.addNode(any(String.class))).thenReturn(eventNode);
        when(session.getNode(nuggetsPath)).thenReturn(nuggetsNode);
        Event event = mock(Event.class);
        when(event.getPath()).thenReturn("/some/path/generating/event");
        ReplicationRequest replicationRequest = persistingJcrEventReplicationTrigger.processEvent(event);
        assertNotNull(replicationRequest);
    }
}