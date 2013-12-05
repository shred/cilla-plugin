/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2013 Richard "Shred" Körber
 *   http://cilla.shredzone.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.shredzone.cilla.plugin.flattr.collector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.shredzone.cilla.plugin.flattr.FlattrPublicationService;
import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.exception.NotFoundException;
import org.shredzone.flattr4j.model.Thing;
import org.shredzone.flattr4j.spring.FlattrServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * An executor that collects {@link ClickFuture}. After a certain delay, all click
 * requests are bulk sent to Flattr and the click counts are returned via the
 * {@link Future}.
 *
 * @author Richard "Shred" Körber
 */
@Component
public class CollectingClickExecutor extends Thread {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Value("${flattr.collect.delay}") long collectDelay;
    private @Value("${flattr.collect.maxitems}") int collectMaxItems;

    private @Resource FlattrPublicationService flattrPublicationService;
    private @Resource FlattrServiceFactory flattrServiceFactory;

    private final Queue<ClickFuture> queue = new ArrayDeque<>();
    private final Queue<ClickFuture> processQueue = new ArrayDeque<>();

    @PostConstruct
    public void setup() {
        setDaemon(true);
        setName("Flattr click count collector");
        start();
    }

    /**
     * Submits a {@link ClickFuture}. It will be processed after at least a few seconds.
     *
     * @param callable
     *            {@link ClickFuture} to be processed
     * @return {@link Future} containing the click count for the {@link ClickFuture}
     */
    public synchronized void submit(ClickFuture callable) {
        queue.add(callable);
        notifyAll();
    }

    @Override
    public synchronized void run() {
        final List<SomeThingId> thingCollection = new ArrayList<>();
        final Map<String, Thing> thingResult = new HashMap<>();

        while (true) {
            try {
                synchronized (this) {
                    while (queue.isEmpty()) {
                        wait();
                    };
                }

                if (collectDelay > 0) {
                    Thread.sleep(collectDelay);
                }

                synchronized (this) {
                    while (!queue.isEmpty() && processQueue.size() < collectMaxItems) {
                        processQueue.add(queue.remove());
                    }
                }

                for (ClickFuture c : processQueue) {
                    c.prepareRequest(thingCollection);
                }

                for (Thing t : flattrServiceFactory.getOpenService().getThings(thingCollection)) {
                    thingResult.put(t.getThingId(), t);
                    thingCollection.remove(new SomeThingId(t));
                }

                if (!thingCollection.isEmpty()) {
                    log.debug("Could not find some flattr counts");
                    for (SomeThingId tid : thingCollection) {
                        flattrPublicationService.unpublished(tid);
                    }
                }

            } catch (NotFoundException ex) {
                log.debug("Could not find all flattr counts", ex);
                for (SomeThingId tid : thingCollection) {
                    flattrPublicationService.unpublished(tid);
                }

            } catch (FlattrException ex) {
                log.error("Cound not bulk get flattr counts", ex);

            } catch (InterruptedException ex) {
                // ignore and continue...

            } finally {
                // Make sure all ClickCallables are going to be triggered...
                for (ClickFuture c : processQueue) {
                    c.filterResult(thingResult);
                }

                // ...and feed the GC
                thingCollection.clear();
                thingResult.clear();
                processQueue.clear();
            }
        }
    }

}
