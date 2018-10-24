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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A simple writer for sitemap.xml files.
 *
 * @author Richard "Shred" Körber
 * @see <a href="http://www.sitemaps.org">sitemaps.org</a>
 */
public class SitemapWriter extends OutputStreamWriter {
    private static final char CR = '\n';

    private final DecimalFormat priorityFormat;
    private final SimpleDateFormat dateFormat;

    /**
     * Instantiates a new {@link SitemapWriter}.
     *
     * @param out
     *            {@link OutputStream} to write to
     */
    public SitemapWriter(OutputStream out) throws IOException {
        super(out, "utf-8");

        priorityFormat = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Writes the XML header. Must be invoked once at the beginning of the stream.
     */
    public void writeHeader() throws IOException {
        write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CR);
        write("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">" + CR);
    }

    /**
     * Writes an URL entry to the XML file.
     *
     * @param url
     *            URL of the page
     * @param lastmod
     *            Last modification date, or {@code null} if unknown
     * @param changeFreq
     *            Change frequency, or {@code null} if unknown
     * @param priority
     *            Priority of the page in the sitemap (between 0.0 and 1.0), or
     *            {@code null} for default priority
     */
    public void writeUrl(String url, Date lastmod, Frequency changeFreq, BigDecimal priority)
    throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url must be set");
        }

        if (priority != null &&
                (priority.compareTo(BigDecimal.ZERO) < 0 || priority.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("priority out of range: " + priority);
        }

        write("<url>");

        write("<loc>");
        write(url);
        write("</loc>");

        if (lastmod != null) {
            write("<lastmod>");
            write(dateFormat.format(lastmod));
            write("</lastmod>");
        }

        if (changeFreq != null) {
            write("<changefreq>");
            write(changeFreq.name().toLowerCase());
            write("</changefreq>");
        }

        if (priority != null) {
            write("<priority>");
            write(priorityFormat.format(priority));
            write("</priority>");
        }

        write("</url>" + CR);
    }

    /**
     * Writes the XML footer. Must be invoked once before the stream is closed.
     */
    public void writeFooter() throws IOException {
        write("</urlset>" + CR);
    }

}
