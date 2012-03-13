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

import javax.annotation.Resource;

import org.shredzone.cilla.plugin.social.annotation.PageLink;
import org.shredzone.cilla.plugin.social.annotation.PageTitle;
import org.shredzone.cilla.plugin.social.annotation.SocialBookmark;
import org.shredzone.cilla.plugin.social.annotation.SocialHandler;
import org.shredzone.commons.view.ViewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Google social bookmarks service.
 *
 * @author Richard "Shred" Körber
 */
@Component
@SocialHandler
public class GoogleSocialHandler {

    private @Value("${site.name}") String siteName;

    private @Resource ViewService viewService;

    /**
     * Google bookmarks
     */
    @SocialBookmark(icon = "google.png")
    public String googleSocialBookmark(
            @PageLink(encoded = true) String pageLink,
            @PageTitle(encoded = true) String pageTitle
    ) {
        return "http://www.google.com/bookmarks/mark?op=add&bkmk=" + pageLink + "&title=" + pageTitle;
    }

    /**
     * Google reader
     */
    @SocialBookmark(icon = "googlereader.png")
    public String googleReaderSocialBookmark(
            @PageLink(encoded = true) String pageLink,
            @PageTitle(encoded = true) String pageTitle
    ) {
        String siteUrl = viewService.getViewContext().getRequestServerUrl();

        try {
            return "http://www.google.com/reader/link?url=" + pageLink
                    + "&title=" + pageTitle
                    + "&srcURL=" + URLEncoder.encode(siteUrl, "utf-8")
                    + "&srcTitle=" + URLEncoder.encode(siteName, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            throw new InternalError("no utf-8");
        }
    }

}
