/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2022 Richard "Shred" Körber
 *   https://cilla.shredzone.org
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
package org.shredzone.cilla.plugin.mastodon;

import org.shredzone.cilla.core.model.Page;

/**
 * A service for {@link Page} related Mastodon operations.
 *
 * @author Richard "Shred" Körber
 */
public interface MastodonPublicationService {

    /**
     * Publish a {@link Page}.
     * <p>
     * If the page's author is registered with Mastodon, a toot is sent.
     *
     * @param page
     *            {@link Page} to publish at Mastodon
     */
    void publish(Page page);

    /**
     * Removes a {@link Page}.
     * <p>
     * If the page was previously published, it is removed from Mastodon.
     *
     * @param page
     *            {@link Page} to remove at Mastodon
     */
    void remove(Page page);

    /**
     * Checks if the {@link Page} is registered with Mastodon.
     *
     * @param page
     *            {@link Page} to check
     * @return {@code true} if the page is registered
     */
    boolean isRegistered(Page page);

}
