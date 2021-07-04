/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2017 Richard "Shred" Körber
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
package org.shredzone.cilla.plugin.twitter;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.signature.TwitterCredentials;

/**
 * Default implementation of {@link TwitterServiceFactory}.
 *
 */
public class DefaultTwitterServiceFactory implements TwitterServiceFactory {

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
    public DefaultTwitterServiceFactory(String key, String secret) {
        this.consumerKey = key;
        this.consumerSecret = secret;
    }

    @Override
    public TwitterClient getTwitterClient(String token, String secret) {
        TwitterCredentials credentials = TwitterCredentials.builder()
                .accessToken(token)
                .accessTokenSecret(secret)
                .apiKey(consumerKey)
                .apiSecretKey(consumerSecret)
                .build();
        return new TwitterClient(credentials);
    }

}
