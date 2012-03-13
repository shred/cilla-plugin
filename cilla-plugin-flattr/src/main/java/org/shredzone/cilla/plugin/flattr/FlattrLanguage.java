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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Resource;

import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.model.Language;
import org.shredzone.flattr4j.model.LanguageId;
import org.shredzone.flattr4j.spring.FlattrServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Collects Flattr languages.
 *
 * @author Richard "Shred" Körber
 */
@Component
public class FlattrLanguage {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Value("${flattr.languagecache.ttl}") long cacheLifetime;

    private @Resource FlattrServiceFactory flattrServiceFactory;

    private Set<String> languageSet = new HashSet<String>();
    private Date cacheExpiry;

    /**
     * Finds a Flattr {@link LanguageId} for the given {@link Locale}. The result is
     * cached. For generic locales (like "es"), an attempt is made to find a matching
     * common Flattr language ("es_ES").
     *
     * @param locale
     *            {@link Locale} to find a {@link LanguageId} for
     * @return {@link LanguageId} or {@code null} if this locale is not supported by
     *         Flattr
     */
    public LanguageId findLanguageId(Locale locale) {
        // Is there a locale and language set?
        if (locale == null || locale.getLanguage() == null || locale.getLanguage().isEmpty()) {
            return null;
        }

        // Fetch/update the language set
        updateLanguageSet();

        // Variant is to be ignored completely

        // Check the language/country code (e.g. "en_US") for a perfect match
        if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
            String result = locale.getLanguage() + '_' + locale.getCountry();
            if (languageSet.contains(result)) {
                return Language.withId(result);
            }
        }

        // Only check the language code (e.g. "fr")
        String language = locale.getLanguage();

        // Check if there is a common transformation available
        String common = transformCommonCodes(language);
        if (languageSet.contains(common)) {
            return Language.withId(common);
        }

        // Find the next language code that starts with the language
        language += '_';
        for (String test : languageSet) {
            if (test.startsWith(language)) {
                return Language.withId(test);
            }
        }

        // The language is not supported by Flattr
        return null;
    }

    /**
     * This method converts a plain language code into a more common language/country code
     * that is known to Flattr. Rationale: If Flattr offers two or more language/country
     * codes for the same language, the system cannot know which of them is
     * "more commonly spoken".
     *
     * @param code
     *            Language code to transform
     * @return Transformed language/country code, or just the code that was put in
     */
    protected String transformCommonCodes(String code) {
        // This code just contains my guesses of the more commonly used language/country
        // code combination. If my guess was wrong, please accept my apologize and send
        // a patch with the correct code. Thank you!

        if ("es".equals(code)) {
            return "es_ES";
        }

        return code;
    }

    /**
     * Updates the cache with the Flattr languages. A new set of languages is polled if
     * the cache is empty or expired.
     * <p>
     * This method is threadsafe.
     */
    protected void updateLanguageSet() {
        synchronized (this) {
            Date now = new Date();
            if (cacheExpiry != null && cacheExpiry.after(now)) {
                // The languageSet cache is still valid...
                return;
            }

            List<Language> languageList;
            try {
                languageList = flattrServiceFactory.getOpenService().getLanguages();
                languageSet.clear();
                for (Language lang : languageList) {
                    languageSet.add(lang.getLanguageId());
                }
                cacheExpiry = new Date(now.getTime() + (cacheLifetime * 1000L));
            } catch (FlattrException ex) {
                log.warn("Failed to update Flattr language list", ex);
            }
        }
    }

}
