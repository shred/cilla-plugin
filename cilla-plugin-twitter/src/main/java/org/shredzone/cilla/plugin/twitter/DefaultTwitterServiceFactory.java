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

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

/**
 * Default implementation of {@link TwitterServiceFactory}.
 *
 */
public class DefaultTwitterServiceFactory implements TwitterServiceFactory {

    private final TwitterFactory factory = new TwitterFactory();

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
    public Twitter getTwitterClient(String token, String secret) {
        AccessToken accessToken = new AccessToken(token, secret);

        Twitter twitter = factory.getInstance();
        twitter.setOAuthConsumer(consumerKey, consumerSecret);
        twitter.setOAuthAccessToken(accessToken);
        return twitter;
    }

}
