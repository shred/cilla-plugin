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
package org.shredzone.cilla.plugin.social;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.shredzone.cilla.plugin.social.annotation.PageLink;
import org.shredzone.cilla.plugin.social.annotation.PageTitle;
import org.shredzone.cilla.plugin.social.annotation.SocialBookmark;
import org.shredzone.cilla.plugin.social.annotation.SocialHandler;
import org.shredzone.cilla.web.plugin.annotation.Priority;
import org.springframework.stereotype.Component;

/**
 * Twitter status handler.
 *
 * @author Richard "Shred" Körber
 */
@Component
@SocialHandler
public class TwitterSocialHandler {
    private static final int MAX_TWEET_LENGTH = 140;

    /**
     * twitter
     */
    @SocialBookmark(icon = "twitter.png")
    @Priority(10)
    public String twitterSocialBookmark(
            @PageLink(shortened = true) String pageLink,
            @PageTitle String pageTitle
    ) {
        String tweet = pageLink + " - " + pageTitle;

        if (tweet.length() > MAX_TWEET_LENGTH) {
            tweet = tweet.substring(0, MAX_TWEET_LENGTH);
        }

        try {
            return "http://twitter.com/home?status=" + URLEncoder.encode(tweet, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            throw new InternalError("no utf-8");
        }
    }

}
