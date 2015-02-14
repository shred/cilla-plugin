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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.shredzone.cilla.plugin.flattr.FlattrPublicationService;
import org.shredzone.cilla.plugin.flattr.FlattrQueue;
import org.shredzone.flattr4j.async.thing.GetThingsFromCollectionMethod;
import org.shredzone.flattr4j.exception.NotFoundException;
import org.shredzone.flattr4j.model.Thing;
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
    private @Resource FlattrQueue flattrQueue;

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

                thingCollection.clear();
                thingCollection.addAll(processQueue.stream()
                                .map(ClickFuture::getThingId)
                                .collect(Collectors.toList()));

                GetThingsFromCollectionMethod method = new GetThingsFromCollectionMethod(thingCollection);
                List<Thing> things = flattrQueue.submit(method).get();

                for (Thing t : things) {
                    thingResult.put(t.getThingId(), t);
                    thingCollection.remove(new SomeThingId(t));
                }

                if (!thingCollection.isEmpty()) {
                    log.debug("Could not find some flattr counts");
                    thingCollection.forEach(flattrPublicationService::unpublished);
                }

            } catch (ExecutionException ex) {
                Throwable t = ex.getCause();
                if (t instanceof NotFoundException) {
                    log.debug("Could not find all flattr counts", t);
                    for (SomeThingId tid : thingCollection) {
                        flattrPublicationService.unpublished(tid);
                    }
                } else {
                    log.error("Cound not bulk get flattr counts", ex);
                }

            } catch (InterruptedException ex) {
                log.error("Cound not bulk get flattr counts", ex);

            } finally {
                // Make sure all ClickCallables are going to be triggered...
                processQueue.forEach(c -> c.filterResult(thingResult));

                // ...and feed the GC
                thingCollection.clear();
                thingResult.clear();
                processQueue.clear();
            }
        }
    }

}
