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

import com.tumblr.jumblr.JumblrClient;

/**
 * Default implementation of {@link JumblrServiceFactory}.
 *
 */
public class DefaultJumblrServiceFactory implements JumblrServiceFactory {

    private final String consumerKey;
    private final String consumerSecret;

    /**
     * Creates a new default factory with the given consumer credentials.
     *
     * @param key
     *            Consumer Key
     * @param secret
     *            Consumer Secret
     */
    public DefaultJumblrServiceFactory(String key, String secret) {
        this.consumerKey = key;
        this.consumerSecret = secret;
    }

    @Override
    public JumblrClient getJumblrClient(String token, String secret) {
        JumblrClient client = new JumblrClient(consumerKey, consumerSecret);
        client.setToken(token, secret);
        return client;
    }

}
