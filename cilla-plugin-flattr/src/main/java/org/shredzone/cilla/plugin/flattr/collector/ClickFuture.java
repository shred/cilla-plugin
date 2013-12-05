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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.shredzone.flattr4j.model.Flattr;
import org.shredzone.flattr4j.model.Thing;
import org.shredzone.flattr4j.model.ThingId;

/**
 * Contains a callable for the click counter of a single Thing.
 *
 * @author Richard "Shred" Körber
 */
public class ClickFuture implements Future<Integer> {
    private final ThingId thingId;
    private Integer result;
    private boolean completed = false;
    private boolean cancelled = false;

    /**
     * Creates a new {@link ClickFuture} for a single {@link ThingId}.
     *
     * @param thingId
     *            {@link ThingId} to count the clicks of
     */
    public ClickFuture(ThingId thingId) {
        this.thingId = thingId;
    }

    /**
     * Prepares the bulk request to Flattr.
     *
     * @param things
     *            {@link Collection} of {@link ThingId} to add the own thing id to
     */
    public void prepareRequest(Collection<ThingId> things) {
        things.add(thingId);
    }

    /**
     * Filters the result returned by {@link Flattr}. Invoking this method also triggers
     * the {@link #call()} method, so the result is returned to the invoker.
     *
     * @param things
     *            Map of Thing ids and their respective {@link Thing} objects
     */
    public synchronized void filterResult(Map<String, Thing> things) {
        Thing thing = things.get(thingId.getThingId());
        if (thing != null) {
            result = thing.getClicks();
        }
        completed = true;
        notifyAll();
    }

    @Override
    public synchronized Integer get() throws InterruptedException, ExecutionException {
        while (!isDone()) {
            wait();
        };
        return result;
    }

    @Override
    public synchronized Integer get(long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException {
        while (!isDone()) {
            unit.timedWait(this, timeout);
        };
        return result;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (completed) {
            return false;
        }
        cancelled = true;
        notifyAll();
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return completed || cancelled;
    }

}
