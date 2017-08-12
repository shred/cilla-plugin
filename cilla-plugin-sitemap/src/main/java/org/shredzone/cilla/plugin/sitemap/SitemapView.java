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
package org.shredzone.cilla.plugin.sitemap;

import java.io.IOException;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.core.repository.PageDao;
import org.shredzone.cilla.service.link.LinkService;
import org.shredzone.commons.view.annotation.View;
import org.shredzone.commons.view.annotation.ViewHandler;
import org.shredzone.commons.view.exception.ViewException;
import org.springframework.stereotype.Component;

/**
 * Views for generating a sitemap.xml.
 *
 * @author Richard "Shred" Körber
 */
@ViewHandler
@Component
public class SitemapView {

    private @Resource PageDao pageDao;
    private @Resource LinkService linkService;

    /**
     * Renders a sitemap of all pages.
     */
    @View(pattern = "/sitemap.xml.gz", name = "sitemap")
    public void sitemapView(HttpServletRequest req, HttpServletResponse resp)
    throws ViewException {
        try {
            resp.setContentType("text/xml");
            resp.setHeader("Content-Encoding", "gzip");

            try (GZIPOutputStream go = new GZIPOutputStream(resp.getOutputStream())) {
                SitemapWriter writer = new SitemapWriter(go);

                writer.writeHeader();
                writeHome(writer);
                writePages(writer);
                writer.writeFooter();
                writer.flush();

                go.finish();
            }
        } catch (IOException ex) {
            throw new ViewException(ex);
        }
    }

    /**
     * Generates a sitemap entry to the home page.
     *
     * @param writer
     *            {@link SitemapWriter} to write to
     */
    private void writeHome(SitemapWriter writer) throws IOException {
        Date[] minMaxDates = pageDao.fetchMinMaxModification();
        String homeUrl = linkService.linkTo().absolute().toString();
        writer.writeUrl(homeUrl, minMaxDates[1], null, 1.0f);
    }

    /**
     * Generates a sitemap entry for all published pages
     *
     * @param writer
     *            {@link SitemapWriter} to write to
     */
    private void writePages(SitemapWriter writer) throws IOException {
        for (Page page : pageDao.fetchAllPublic()) {
            String pageUrl;
            if (page.getName() != null) {
                pageUrl = linkService.linkTo().param("pagename", page.getName()).absolute().toString();
            } else {
                pageUrl = linkService.linkTo().page(page).absolute().toString();
            }

            Float priority = null;
            if (page.isHidden()) {
                priority = 0.3f;
            }
            if (page.isSticky()) {
                priority = 0.7f;
            }

            writer.writeUrl(pageUrl, page.getModification(), null, priority);
        }
    }

}
