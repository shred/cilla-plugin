/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2014 Richard "Shred" Körber
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
package org.shredzone.cilla.plugin.tumblr;

import org.shredzone.cilla.core.model.Page;

/**
 * A service for {@link Page} related Tumblr operations.
 *
 * @author Richard "Shred" Körber
 */
public interface TumblrPublicationService {

    /**
     * Publish a {@link Page}.
     * <p>
     * If the page's author is registered with Tumblr, the page is registered at Tumblr,
     * and the URL and ID is stored in the page.
     *
     * @param page
     *            {@link Page} to publish at Tumblr
     */
    void publish(Page page);

    /**
     * Updates a {@link Page}.
     * <p>
     * If the page was previously published, it is updated to reflect the changes.
     *
     * @param page
     *            {@link Page} to update at Tumblr
     */
    void update(Page page);

    /**
     * Removes a {@link Page}.
     * <p>
     * If the page was previously published, it is removed from Tumblr.
     *
     * @param page
     *            {@link Page} to remove at Tumblr
     */
    void remove(Page page);

    /**
     * Checks if the {@link Page} is registered with Tumblr.
     *
     * @param page
     *            {@link Page} to check
     * @return {@code true} if the page is registered
     */
    boolean isRegistered(Page page);

}
