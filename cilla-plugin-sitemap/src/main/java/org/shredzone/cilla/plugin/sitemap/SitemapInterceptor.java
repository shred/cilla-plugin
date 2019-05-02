/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2018 Richard "Shred" Körber
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

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.core.model.Picture;
import org.shredzone.cilla.web.plugin.annotation.Priority;

/**
 * An interceptor for sitemap entries.
 * <p>
 * Interceptors are processed in order of their {@link Priority}.
 *
 * @author Richard "Shred" Körber
 */
public interface SitemapInterceptor {

    /**
     * Returns {@code true} if a {@link Page} is to be ignored for the sitemap.
     *
     * @param page
     *            Current {@link Page} to be rendered
     * @return {@code true}: ignore this page, {@code false}: use this page in the
     *         sitemap. The page is ignored if at least one interceptor returns
     *         {@code true}.
     */
    default boolean isIgnored(Page page) {
        return false;
    }

    /**
     * Changes the priority of the page in the sitemap. By default, sticky pages have a
     * "0.7" priority, while hidden pages have a priority of "0.3" (if rendered at all).
     *
     * @param page
     *            {@link Page} to test
     * @param priority
     *            Contains the current priority. Implementations can change this value at
     *            their discretion.
     */
    default void priority(Page page, AtomicReference<BigDecimal> priority) {
        // do nothing by default
    }

    /**
     * Changes the modification date of the page in the sitemap. By default, the page's
     * modification date is used.
     *
     * @param page
     *            {@link Page} to test
     * @param modification
     *            Contains the current modification date. Implementations can change this
     *            value at their discretion.
     */
    default void modification(Page page, AtomicReference<Date> modification) {
        // do nothing by default
    }

    /**
     * Changes the update frequency of the page in the sitemap. By default, no frequency
     * is used.
     *
     * @param page
     *            {@link Page} to test
     * @param frequency
     *            Contains the current update frequency. Implementations can change this
     *            value at their discretion.
     */
    default void frequency(Page page, AtomicReference<Frequency> frequency) {
        // do nothing by default
    }

    /**
     * Returns {@code true} if a {@link Picture} is to be ignored for the sitemap.
     *
     * @param picture
     *            Current {@link Picture} to be rendered
     * @return {@code true}: ignore this picture, {@code false}: use this picture in the
     *         sitemap. The picture is ignored if at least one interceptor returns
     *         {@code true}.
     */
    default boolean isIgnored(Picture picture) {
        return false;
    }

    /**
     * Changes the priority of the picture in the sitemap. By default, pictures have a
     * "0.2" priority.
     *
     * @param picture
     *            Current {@link Picture} to test
     * @param priority
     *            Contains the current priority. Implementations can change this value at
     *            their discretion.
     */
    default void priority(Picture picture, AtomicReference<BigDecimal> priority) {
        // do nothing by default
    }

    /**
     * Changes the modification date of the picture in the sitemap. By default, {@link
     * Picture#getCreateDate()} is used.
     *
     * @param picture
     *         Current {@link Picture} to test
     * @param modification
     *         Contains the current modification date. Implementations can change this
     *         value at their discretion.
     */
    default void modification(Picture picture, AtomicReference<Date> modification) {
        // do nothing by default
    }

    /**
     * Changes the update frequency of the picture in the sitemap. By default, no
     * frequency is used.
     *
     * @param picture
     *         Current {@link Picture} to test
     * @param frequency
     *         Contains the current update frequency. Implementations can change this
     *         value at their discretion.
     */
    default void frequency(Picture picture, AtomicReference<Frequency> frequency) {
        // do nothing by default
    }

}
