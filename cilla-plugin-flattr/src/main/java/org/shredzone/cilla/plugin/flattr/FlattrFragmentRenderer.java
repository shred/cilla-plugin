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
package org.shredzone.cilla.plugin.flattr;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.service.link.LinkService;
import org.shredzone.cilla.web.fragment.annotation.Fragment;
import org.shredzone.cilla.web.fragment.annotation.FragmentRenderer;
import org.shredzone.cilla.web.header.DocumentHeader;
import org.shredzone.cilla.web.header.DocumentHeaderObserver;
import org.shredzone.cilla.web.header.tag.CssLinkTag;
import org.shredzone.cilla.web.header.tag.JavaScriptLinkTag;
import org.shredzone.cilla.web.header.tag.LinkTag;
import org.shredzone.commons.view.annotation.PathPart;
import org.shredzone.commons.view.annotation.View;
import org.shredzone.commons.view.annotation.ViewHandler;
import org.shredzone.flattr4j.model.Thing;
import org.shredzone.flattr4j.model.ThingId;
import org.shredzone.flattr4j.model.UserId;
import org.springframework.stereotype.Component;

/**
 * A fragment renderer for rendering a donation link to Flattr.
 * <p>
 * Requires jQuery. It must be included separately.
 *
 * @author Richard "Shred" Körber
 */
@Component
@FragmentRenderer
@ViewHandler
public class FlattrFragmentRenderer implements DocumentHeaderObserver {

    private @Resource LinkService linkService;
    private @Resource FlattrPublicationService flattrPublicationService;

    private CssLinkTag flattrCssTag;
    private JavaScriptLinkTag flattrJsTag;

    /**
     * Generates a donation link to the Flattr web site.
     *
     * @param page
     *            {@link Page} that is flattred
     * @return Fragment HTML
     */
    @Fragment(name = "donate")
    public String flattrFragment(Page page) {
        StringBuilder sb = new StringBuilder();

        ThingId thingId = flattrPublicationService.getFlattrThing(page);
        if (thingId != null) {
            String countUrl = linkService.linkTo()
                            .qualifier("ajax")
                            .param("thingid", thingId.getThingId())
                            .toString();

            sb.append("<div style=\"margin-left:auto\" class=\"flattr-button compact\">");
            sb.append("<a class=\"flattr\" href=\"").append(page.getDonateUrl());
            sb.append("\" data-counter=\"").append(countUrl).append("\">");
            sb.append("<span class=\"flattr-link\">&nbsp;</span></a>");
            sb.append("</div>");
        }

        return sb.toString();
    }

    /**
     * Fetches the flattr click counter of a thing as AJAX request.
     */
    @View(pattern = "/ajax/flattr/${#thingid}-count.html", signature = {"#thingid"}, qualifier = "ajax")
    public void flattrCount(
        @PathPart("#thingid") String thingid,
        HttpServletRequest req, HttpServletResponse resp
    ) throws IOException {
        int count = flattrPublicationService.clickCount(Thing.withId(thingid));
        resp.getWriter().append("<span class=\"flattr-count\"><span>").append(String.valueOf(count)).append("</span></span>");
    }

    /**
     * Adds the flattr css and js to the page's header.
     */
    @Override
    public void onNewDocumentHeader(DocumentHeader header, ServletRequest req) {
        if (flattrCssTag == null) {
            flattrCssTag = new CssLinkTag(linkService.linkTo()
                            .view("resource")
                            .param("package", "flattr")
                            .param("name", "flattr.css")
                            .toString()
            );
        }

        if (flattrJsTag == null) {
            flattrJsTag = new JavaScriptLinkTag(linkService.linkTo()
                            .view("resource")
                            .param("package", "flattr")
                            .param("name", "flattr.js")
                            .toString()
            );
        }

        header.add(flattrCssTag);
        header.add(flattrJsTag);

        Page page = (Page) req.getAttribute("page");
        if (page != null) {
            UserId owner = flattrPublicationService.getFlattrThingOwner(page);
            if (owner != null) {
                String profile = "https://flattr.com/profile/" + owner.getUserId();
                header.add(new LinkTag("me", profile));
            }
        }
    };

}
