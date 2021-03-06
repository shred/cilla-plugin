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
package org.shredzone.cilla.plugin.social.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.shredzone.cilla.core.model.Page;

/**
 * Annotates a {@link String} parameter of the {@link SocialBookmark} method. The
 * {@link String} will contain an absolute URL of the {@link Page} to be linked to.
 *
 * @author Richard "Shred" Körber
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PageLink {

    /**
     * If set to {@code true}, a shortened link to the {@link Page} is passed in, if a
     * link shortener service was configured. Defaults to {@code false}, so an absolute
     * link to the blog itself is passed to the method.
     */
    boolean shortened() default false;

    /**
     * If set to {@code true}, the link is URL encoded. Defaults to {@code false}.
     */
    boolean encoded() default false;

}
