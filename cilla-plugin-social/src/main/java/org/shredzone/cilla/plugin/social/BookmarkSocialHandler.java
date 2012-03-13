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

import org.shredzone.cilla.plugin.social.annotation.PageLink;
import org.shredzone.cilla.plugin.social.annotation.PageTitle;
import org.shredzone.cilla.plugin.social.annotation.SocialBookmark;
import org.shredzone.cilla.plugin.social.annotation.SocialHandler;
import org.springframework.stereotype.Component;

/**
 * Handler for famous social bookmark services.
 * <p>
 * This list does not claim to be complete. :)
 *
 * @author Richard "Shred" Körber
 */
@Component
@SocialHandler
public class BookmarkSocialHandler {

    /**
     * del.icio.us
     */
    @SocialBookmark(icon = "delicious.png")
    public String deliciousSocialBookmark(
            @PageLink(encoded = true) String pageLink,
            @PageTitle(encoded = true) String pageTitle
    ) {
        return "http://delicious.com/save?url=" + pageLink + "&title=" + pageTitle;
    }

    /**
     * Digg
     */
    @SocialBookmark(icon = "digg.png")
    public String diggSocialBookmark(
            @PageLink(encoded = true) String pageLink,
            @PageTitle(encoded = true) String pageTitle
    ) {
        return "http://digg.com/submit?url=" + pageLink + "&title=" + pageTitle;
    }

    /**
     * Linkarena
     */
    @SocialBookmark(icon = "linkarena.png")
    public String linkarenaSocialBookmark(
            @PageLink(encoded = true) String pageLink,
            @PageTitle(encoded = true) String pageTitle
    ) {
        return "http://linkarena.com/bookmarks/addlink/?url=" + pageLink + "&title=" + pageTitle;
    }

    /**
     * Mr. Wong
     */
    @SocialBookmark(icon = "mrwong.png")
    public String mrWongSocialBookmark(
            @PageLink(encoded = true) String pageLink,
            @PageTitle(encoded = true) String pageTitle
    ) {
        return "http://www.mister-wong.de/index.php?action=addurl&bm_url=" + pageLink + "&bm_description=" + pageTitle;
    }

    /**
     * Reddit
     */
    @SocialBookmark(icon = "reddit.png")
    public String redditSocialBookmark(
            @PageLink(encoded = true) String pageLink,
            @PageTitle(encoded = true) String pageTitle
    ) {
        return "http://www.reddit.com/submit?title=" + pageTitle + "&url=" + pageLink;
    }

    /**
     * Webnews
     */
    @SocialBookmark(icon = "webnews.png")
    public String webnewsSocialBookmark(
            @PageLink(encoded = true) String pageLink,
            @PageTitle(encoded = true) String pageTitle
    ) {
        return "http://www.webnews.de/einstellen?url=" + pageLink + "&title=" + pageTitle;
    }

    /**
     * Yigg
     */
    @SocialBookmark(icon = "yigg.png")
    public String yiggSocialBookmark(
            @PageLink(encoded = true) String pageLink,
            @PageTitle(encoded = true) String pageTitle
    ) {
        return "http://yigg.de/neu?exturl=" + pageLink + "&exttitle=" + pageTitle;
    }

}
