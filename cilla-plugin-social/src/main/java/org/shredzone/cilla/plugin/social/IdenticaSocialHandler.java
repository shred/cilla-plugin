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
 * Identi.ca social bookmark service.
 *
 * @author Richard "Shred" Körber
 */
@Component
@SocialHandler
public class IdenticaSocialHandler {
    private static final int MAX_POST_LENGTH = 140;

    /**
     * identi.ca
     */
    @SocialBookmark(icon = "identica.png")
    @Priority(11)
    public String identicaSocialBookmark(
            @PageLink(shortened = true) String pageLink,
            @PageTitle String pageTitle
    ) {
        String post = pageLink + " - " + pageTitle;

        if (post.length() > MAX_POST_LENGTH) {
            post = post.substring(0, MAX_POST_LENGTH);
        }

        try {
            return "http://identi.ca/notice/new?status_textarea=" + URLEncoder.encode(post, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            throw new InternalError("no utf-8");
        }
    }

}
