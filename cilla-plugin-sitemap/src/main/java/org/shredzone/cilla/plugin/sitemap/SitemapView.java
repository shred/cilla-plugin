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

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.shredzone.cilla.core.model.GallerySection;
import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.core.model.Picture;
import org.shredzone.cilla.core.repository.PageDao;
import org.shredzone.cilla.service.link.LinkService;
import org.shredzone.cilla.web.plugin.manager.PriorityComparator;
import org.shredzone.commons.view.annotation.View;
import org.shredzone.commons.view.annotation.ViewHandler;
import org.shredzone.commons.view.exception.ViewException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Views for generating a sitemap.xml.
 *
 * @author Richard "Shred" Körber
 */
@ViewHandler
@Component
public class SitemapView {

    private @Value("${sitemap.skipHidden}") boolean skipHidden;
    private @Value("${sitemap.skipGallery}") boolean skipGallery;

    private @Resource PageDao pageDao;
    private @Resource LinkService linkService;
    private @Resource ApplicationContext applicationContext;

    private List<SitemapInterceptor> interceptors;

    /**
     * Initializes the list of feed view interceptors.
     */
    @PostConstruct
    protected void setup() {
        interceptors = applicationContext.getBeansOfType(SitemapInterceptor.class).values().stream()
                .sorted(new PriorityComparator<>(SitemapInterceptor.class))
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
    }

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
                if (!skipGallery) {
                    writeGallery(writer);
                }
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
        writer.writeUrl(homeUrl, minMaxDates[1], null, BigDecimal.ONE);
    }

    /**
     * Generates a sitemap entry for all published pages
     *
     * @param writer
     *            {@link SitemapWriter} to write to
     */
    private void writePages(SitemapWriter writer) throws IOException {
        for (Page page : pageDao.fetchAllPublic()) {
            if (page.isHidden() && skipHidden) {
                continue;
            }

            if (interceptors.stream().anyMatch(it -> it.isIgnored(page))) {
                continue;
            }

            String pageUrl;
            if (page.getName() != null) {
                pageUrl = linkService.linkTo().param("pagename", page.getName()).absolute().toString();
            } else {
                pageUrl = linkService.linkTo().page(page).absolute().toString();
            }

            AtomicReference<BigDecimal> priority = new AtomicReference<>(null);
            if (page.isHidden()) {
                priority.set(new BigDecimal("0.3"));
            }
            if (page.isSticky()) {
                priority.set(new BigDecimal("0.7"));
            }
            interceptors.forEach(it -> it.priority(page, priority));

            AtomicReference<Date> modification = new AtomicReference<>(page.getModification());
            interceptors.forEach(it -> it.modification(page, modification));

            AtomicReference<Frequency> frequency = new AtomicReference<>(null);
            interceptors.forEach(it -> it.frequency(page, frequency));

            writer.writeUrl(pageUrl, modification.get(), frequency.get(), priority.get());
        }
    }

    /**
     * Generates a sitemap entry for all published gallery images
     *
     * @param writer
     *            {@link SitemapWriter} to write to
     */
    private void writeGallery(SitemapWriter writer) throws IOException {
        for (Page page : pageDao.fetchAllPublic()) {
            if (page.isHidden() && skipHidden) {
                continue;
            }

            if (interceptors.stream().anyMatch(it -> it.isIgnored(page))) {
                continue;
            }

            for (GallerySection section : page.getSections().stream()
                    .filter(GallerySection.class::isInstance)
                    .map(GallerySection.class::cast)
                    .collect(Collectors.toList())) {
                for (Picture pic : section.getPictures()) {
                    if (interceptors.stream().anyMatch(it -> it.isIgnored(pic))) {
                        continue;
                    }

                    AtomicReference<BigDecimal> priority = new AtomicReference<>(null);
                    interceptors.forEach(it -> it.priority(pic, priority));

                    AtomicReference<Date> modification = new AtomicReference<>(page.getModification());
                    interceptors.forEach(it -> it.modification(pic, modification));

                    AtomicReference<Frequency> frequency = new AtomicReference<>(null);
                    interceptors.forEach(it -> it.frequency(pic, frequency));

                    String pictureUrl = linkService.linkTo().section(section).picture(pic).absolute().toString();
                    writer.writeUrl(pictureUrl, modification.get(), frequency.get(), priority.get());
                }
            }
        }
    }

}
