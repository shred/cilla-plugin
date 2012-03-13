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
package org.shredzone.cilla.plugin.social.renderer;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.jsp.JspWriter;

import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.plugin.social.manager.SocialHandlerManager;
import org.shredzone.cilla.plugin.social.manager.SocialLink;
import org.shredzone.cilla.web.fragment.annotation.Fragment;
import org.shredzone.cilla.web.fragment.annotation.FragmentRenderer;
import org.springframework.stereotype.Component;

/**
 * A renderer for social bookmarks.
 *
 * @author Richard "Shred" Körber
 */
@Component
@FragmentRenderer
public class SocialFragmentRenderer {

    private @Resource SocialHandlerManager socialHandlerManager;

    @Fragment(name = "social")
    public void socialFragment(Page page, JspWriter out) throws IOException {
        for (SocialLink link : socialHandlerManager.fetchLinksToPage(page)) {
            String escapedTitle = escape(link.getTitle());

            out.append("<a href=\"").append(escape(link.getUrl())).append("\"");
            out.append(" title=\"").append(escapedTitle).append("\" rel=\"nofollow\">");
            out.append("<img src=\"").append(escape(link.getIconUrl())).append("\"");
            out.append(" alt=\"").append(escapedTitle).append("\" />");
            out.append("</a>");
        }
    }

    /**
     * Escapes HTML entities.
     *
     * @param str
     *            String to escape
     * @return escaped string
     */
    private static String escape(String str) {
        return str.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;");
    }

}
