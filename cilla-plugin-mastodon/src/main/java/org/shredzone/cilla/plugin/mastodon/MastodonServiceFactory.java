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

import com.sys1yagi.mastodon4j.MastodonClient;

/**
 * A factory for readily configured {@link MastodonClient}.
 *
 * @author Richard "Shred" Körber
 */
public interface MastodonServiceFactory {

    /**
     * Creates a new {@link MastodonClient} instance.
     *
     * @param instance
     *            Mastodon instance to connect to
     * @param token
     *            Access token for that instance
     * @return {@link MastodonClient} instance. Each invocation creates a new instance.
     */
    MastodonClient getMastodonClient(String instance, String token);

}
