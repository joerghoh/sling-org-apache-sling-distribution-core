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
package org.apache.sling.distribution.trigger.impl;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.DistributionTriggerException;
import org.apache.sling.distribution.util.DistributionJcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of a {@link org.apache.sling.distribution.trigger.DistributionTrigger} that listens for 'safe'
 * events and triggers a {@link org.apache.sling.distribution.communication.DistributionRequest} from that.
 */
public abstract class AbstractJcrEventTrigger implements DistributionTrigger {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, JcrEventdistributionTriggerListener> registeredListeners = new ConcurrentHashMap<String, JcrEventdistributionTriggerListener>();

    private final String path;
    private final String serviceUser;

    protected final SlingRepository repository;

    private Session cachedSession;

    public AbstractJcrEventTrigger(SlingRepository repository, String path, String serviceUser) {
        this.repository = repository;
        this.path = path;
        this.serviceUser = serviceUser;
    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        Session session;
        try {
            session = getSession();
            JcrEventdistributionTriggerListener listener = new JcrEventdistributionTriggerListener(requestHandler);
            registeredListeners.put(requestHandler.toString(), listener);
            session.getWorkspace().getObservationManager().addEventListener(
                    listener, getEventTypes(), path, true, null, null, false);
        } catch (RepositoryException e) {
            throw new DistributionTriggerException("unable to register handler " + requestHandler, e);
        }
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        JcrEventdistributionTriggerListener listener = registeredListeners.get(requestHandler.toString());
        if (listener != null) {
            Session session;
            try {
                session = getSession();
                session.getWorkspace().getObservationManager().removeEventListener(listener);
            } catch (RepositoryException e) {
                throw new DistributionTriggerException("unable to unregister handler " + requestHandler, e);
            }
        }
    }

    private class JcrEventdistributionTriggerListener implements EventListener {
        private final DistributionRequestHandler requestHandler;

        public JcrEventdistributionTriggerListener(DistributionRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void onEvent(EventIterator eventIterator) {
            log.info("handling event {}");
            while (eventIterator.hasNext()) {
                Event event = eventIterator.nextEvent();
                try {
                    if (DistributionJcrUtils.isSafe(event)) {
                        DistributionRequest request = processEvent(event);
                        if (request != null) {
                            requestHandler.handle(request);
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("Error while handling event {}", event, e);
                }
            }
        }
    }

    /**
     * process the received event and generates a distribution request
     *
     * @param event an {@link javax.jcr.observation.Event} to be processed
     * @return the {@link org.apache.sling.distribution.communication.DistributionRequest} originated by processing the event,
     * or <code>null</code> if no request could be generated
     * @throws RepositoryException
     */
    protected abstract DistributionRequest processEvent(Event event) throws RepositoryException;

    /**
     * get the binary int event types to be handled by this JCR event listener
     *
     * @return a <code>int</code> as generated by e.g. <code>Event.NODE_ADDED | Event.NODE_MOVED</code>
     */
    protected int getEventTypes() {
        return Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED |
                Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED;
    }

    /**
     * return a newly initiated JCR session to register the {@link javax.jcr.observation.EventListener}
     *
     * @return a {@link javax.jcr.Session}
     * @throws RepositoryException
     */
    protected Session getSession() throws RepositoryException {
        return cachedSession != null ? cachedSession : (cachedSession = repository.loginService(serviceUser, null));
    }


}