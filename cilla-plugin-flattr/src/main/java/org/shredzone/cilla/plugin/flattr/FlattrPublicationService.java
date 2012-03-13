/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2012 Richard "Shred" Körber
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
package org.shredzone.cilla.plugin.flattr;

import org.shredzone.cilla.core.model.Page;
import org.shredzone.flattr4j.model.ThingId;

/**
 * A service for {@link Page} related Flattr operations.
 *
 * @author Richard "Shred" Körber
 */
public interface FlattrPublicationService {

    /**
     * Publish a {@link Page}.
     * <p>
     * If the page's author is registered with Flattr, the page is registered at Flattr,
     * and the Thing's URL and ID is stored in the page.
     *
     * @param page
     *            {@link Page} to publish at Flattr
     */
    void publish(Page page);

    /**
     * Updates a {@link Page}.
     * <p>
     * If the page was previously published, it is updated to reflect the changes.
     * <p>
     * Note that some page properties (especially the page URL) cannot be changed.
     *
     * @param page
     *            {@link Page} to update at Flattr
     */
    void update(Page page);

    /**
     * Removes a {@link Page}.
     * <p>
     * If the page was previously published, it is removed from Flattr, and the Thing's
     * URL and ID is removed from the page.
     *
     * @param page
     *            {@link Page} to remove at Flattr
     */
    void remove(Page page);

    /**
     * Checks if the {@link Page} is registered with Flattr.
     *
     * @param page
     *            {@link Page} to check
     * @return {@code true} if the page is registered
     */
    boolean isRegistered(Page page);

    /**
     * Returns the {@link ThingId} of a page.
     *
     * @param page
     *            {@link Page} to get the {@link ThingId} of
     * @return {@link ThingId}, or {@code null} if this page is not registered with Flattr
     */
    ThingId getFlattrThing(Page page);

    /**
     * Counts the number of Flattr clicks a thing received.
     *
     * @param ThingId
     *            {@link ThingId} to query
     * @return Number of clicks, or 0 if the click counter could not be retrieved
     */
    int clickCount(ThingId thingId);

}
