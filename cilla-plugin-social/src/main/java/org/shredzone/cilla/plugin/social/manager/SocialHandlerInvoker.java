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
package org.shredzone.cilla.plugin.social.manager;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;

import javax.annotation.Resource;

import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.plugin.social.annotation.PageLink;
import org.shredzone.cilla.plugin.social.annotation.PageTitle;
import org.shredzone.cilla.plugin.social.annotation.SocialBookmark;
import org.shredzone.cilla.plugin.social.annotation.SocialHandler;
import org.shredzone.cilla.service.link.LinkService;
import org.shredzone.cilla.web.plugin.LinkShortener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * Invokes a {@link SocialHandler}.
 *
 * @author Richard "Shred" Körber
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SocialHandlerInvoker implements Comparable<SocialHandlerInvoker> {

    private @Resource LinkShortener linkShortenerService;
    private @Resource LinkService linkService;

    private Object bean;
    private Method method;
    private int priority;
    private String identifier;
    private String name;
    private String icon;

    private Annotation[] methodAnnotations;

    /**
     * Spring bean offering the {@link SocialHandler}.
     */
    public Object getBean()                     { return bean; }
    public void setBean(Object bean)            { this.bean = bean; }

    /**
     * Method annotated with {@link SocialBookmark} that is to be called by this invoker.
     */
    public Method getMethod()                   { return method; }

    /**
     * Identifier of this social bookmark.
     */
    public String getIdentifier()               { return identifier; }

    /**
     * Priority. {@link SocialBookmark} with a higher priority are rendered before those
     * with a lower priority.
     */
    public void setPriority(int priority)       { this.priority = priority; }
    public int getPriority()                    { return priority; }

    /**
     * Name of the social bookmark service.
     */
    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    /**
     * Icon of the social bookmark service.
     */
    public String getIcon()                     { return icon; }
    public void setIcon(String icon)            { this.icon = icon; }

    /**
     * Sets the {@link Method} to be invoked.
     *
     * @param method
     *            {@link Method} to be invoked
     */
    public void setMethod(Method method) {
        this.method = method;

        Annotation[][] annotations = method.getParameterAnnotations();
        methodAnnotations = new Annotation[annotations.length];

        for (int ix = 0; ix < annotations.length; ix++) {
            for (Annotation sub : annotations[ix]) {
                if (       sub instanceof PageLink
                        || sub instanceof PageTitle) {
                    if (methodAnnotations[ix] != null) {
                        throw new IllegalArgumentException("Conflicting annotations "
                                + sub + " and " + methodAnnotations[ix] + " in social bookmark handler "
                                + bean.getClass().getName() + "#" + method.getName() + "()");
                    }
                    methodAnnotations[ix] = sub;
                }
            }
        }

        SocialBookmark sbAnno = method.getAnnotation(SocialBookmark.class);
        if (sbAnno != null && sbAnno.identifier() != null && !sbAnno.identifier().isEmpty()) {
            identifier = sbAnno.identifier();
        } else {
            String name = method.getName();
            if (name.endsWith("SocialBookmark")) {
                name = name.substring(0, name.length() - "SocialBookmark".length());
            }
            identifier = name;
        }
    }

    /**
     * Invokes the {@link SocialHandler}'s {@link SocialBookmark} method.
     *
     * @param page
     *            {@link Page} to create a {@link SocialLink} for
     * @return {@link SocialLink} that was created
     */
    public SocialLink invoke(Page page) {
        Class<?>[] types = method.getParameterTypes();
        Object[] values = new Object[types.length];

        PageData data = new PageData(page);

        for (int ix = 0; ix < types.length; ix++) {
            values[ix] = evaluateParameter(types[ix], methodAnnotations[ix], data);
        }

        Object result = ReflectionUtils.invokeMethod(method, bean, values);

        if (result == null) {
            return null;
        } else if (result instanceof SocialLink) {
            return (SocialLink) result;
        } else if (result instanceof String) {
            String iconUrl = linkService.linkTo()
                            .view("resource")
                            .param("package", "social")
                            .param("name", icon)
                            .toString();
            return new SocialLink(result.toString(), iconUrl, name);
        } else {
            throw new IllegalArgumentException("Unknown result type " + result.getClass());
        }
    }

    /**
     * Evaluates a single parameter of the method signature.
     *
     * @param type
     *            parameter type
     * @param anno
     *            {@link Annotation} of the parameter
     * @param pageData
     *            {@link PageData} containing all necessary information about a page
     * @return value to be passed to this method parameter
     */
    private Object evaluateParameter(Class<?> type, Annotation anno, PageData pageData) {
        if (anno instanceof PageLink) {
            PageLink pageLinkAnno = (PageLink) anno;
            String result;

            if (pageLinkAnno.shortened()) {
                result = pageData.getShortenedLink();
            } else {
                result = pageData.getLink();
            }

            if (pageLinkAnno.encoded()) {
                result = urlencode(result);
            }

            return result;
        }

        if (anno instanceof PageTitle) {
            PageTitle pageTitleAnno = (PageTitle) anno;

            String result = pageData.getTitle();
            if (pageTitleAnno.encoded()) {
                result = urlencode(result);
            }

            return result;
        }

        if (Page.class.isAssignableFrom(type)) {
            return pageData.getPage();
        }

        throw new IllegalArgumentException("Unknown parameter type " + type.getName());
    }

    /**
     * URL encodes the given string, using utf-8 encoding.
     * <p>
     * This is just a wrapper of {@link URLEncoder#encode(String, String)} with the
     * exception being handled internally.
     *
     * @param url
     *            URL to encode
     * @return encoded URL
     */
    private static String urlencode(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            throw new InternalError("no utf-8");
        }
    }

    @Override
    public int compareTo(SocialHandlerInvoker o) {
        int diff = o.priority - priority;

        if (diff != 0) {
            return diff;
        } else {
            return name.compareTo(o.name);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SocialHandlerInvoker)) {
            return false;
        }

        SocialHandlerInvoker cmp = (SocialHandlerInvoker) obj;
        return cmp.identifier.equals(identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    /**
     * Simple object containing data about the page.
     */
    private class PageData {

        private final Page page;
        private String link = null;
        private String shortenedLink = null;

        /**
         * Instantiates a new {@link PageData} object.
         *
         * @param page
         *            {@link Page} that is referenced.
         */
        public PageData(Page page) {
            this.page = page;
        }

        /**
         * Returns the {@link Page} being referenced.
         */
        public Page getPage() {
            return page;
        }

        /**
         * Returns an absolute link URL to the {@link Page}.
         */
        public String getLink() {
            if (link == null) {
                link = linkService.linkTo().absolute().page(page).toString();
            }
            return link;
        }

        /**
         * Returns a shortened link URL. The {@link Page}'s absolute URL is passed to a
         * link shortener service, and the result is returned. If no shortener service is
         * configured, the full URL is returned instead. The result is cached.
         */
        public String getShortenedLink() {
            if (shortenedLink == null && linkShortenerService != null) {
                shortenedLink = linkShortenerService.shorten(page);
            }
            if (shortenedLink == null) {
                shortenedLink = getLink();
            }
            return shortenedLink;
        }

        /**
         * Returns the title.
         */
        public String getTitle() {
            return page.getTitle();
        }

    }

}
