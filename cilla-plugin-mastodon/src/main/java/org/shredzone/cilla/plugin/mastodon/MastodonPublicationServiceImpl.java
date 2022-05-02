/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2022 Richard "Shred" Körber
 *   https://cilla.shredzone.org
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
package org.shredzone.cilla.plugin.mastodon;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Statuses;
import org.apache.http.HttpStatus;
import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.core.model.Tag;
import org.shredzone.cilla.core.model.User;
import org.shredzone.cilla.core.repository.PageDao;
import org.shredzone.cilla.service.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link MastodonPublicationService}.
 *
 * @author Richard "Shred" Körber
 */
@Service
@Transactional
public class MastodonPublicationServiceImpl implements MastodonPublicationService {
    public static final String PROPKEY_MASTODON_ID = "mastodon.id";
    public static final String PROPKEY_MASTODON_INSTANCE = "mastodon.instance";
    public static final String PROPKEY_MASTODON_TOKEN = "mastodon.token";

    private static final int MAX_TOOT_LENGTH = 500;  // TODO: read from API if possible
    private static final int SHORT_URL_LENGTH = 23;  // TODO: read from API if possible

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Value("${mastodon.masterEnable}") boolean mastodonMasterEnabled;
    private @Value("${mastodon.useTags}") boolean mastodonUseTags;
    private @Value("${mastodon.fixedTags}") String mastodonFixedTags;
    private @Value("${mastodon.separator}") String mastodonSeparator;
    private @Value("${mastodon.threading}") boolean mastodonThreading;
    private @Value("${mastodon.sensitive}") boolean mastodonSensitive;
    private @Value("${mastodon.visibility}") Status.Visibility mastodonVisibility;

    private @Resource MastodonServiceFactory mastodonServiceFactory;
    private @Resource PageDao pageDao;
    private @Resource LinkService linkService;

    private List<String> fixedTags;

    @PostConstruct
    public void setup() {
        fixedTags = Collections.unmodifiableList(splitTags(mastodonFixedTags));
    }

    @Override
    public void publish(Page page) {
        if (!mastodonMasterEnabled) {
            log.info("Mastodon handling is disabled");
            return;
        }

        if (isRegistered(page)) {
            log.warn("Attempt to resubmit an already submitted page at Mastodon (page id {})", page.getId());
            return;
        }

        try {
            MastodonClient client = createMastodonClient(page.getCreator());
            Statuses statuses = new Statuses(client);

            String statusLine = statusToPost(page);

            Long inReplyTo = mastodonThreading ? findInReplyTo(page) : null;
            Status status = statuses
                    .postStatus(statusLine, inReplyTo, null, mastodonSensitive, null, mastodonVisibility)
                    .execute();

            page.getProperties().put(PROPKEY_MASTODON_ID, String.valueOf(status.getId()));

            StringBuilder sb = new StringBuilder("Registered page id ");
            sb.append(page.getId()).append(", Status ID ").append(status.getId());
            if (inReplyTo != null) {
                sb.append(", in reply to Status ID ").append(inReplyTo);
            }
            log.info(sb.toString());
        } catch (Exception ex) {
            log.warn("Failed to submit a Mastodon status for page id " + page.getId(), ex);
        }
    }

    @Override
    public void remove(Page page) {
        if (!mastodonMasterEnabled) {
            log.info("Mastodon handling is disabled");
            return;
        }

        Long statusId = getStatusId(page);

        if (statusId != null) {
            try {
                MastodonClient client = createMastodonClient(page.getCreator());
                Statuses statuses = new Statuses(client);
                statuses.deleteStatus(statusId);

                page.getProperties().remove(PROPKEY_MASTODON_ID);

                log.info("Deleted page id " + page.getId() + ", Mastodon status ID " + statusId);
            } catch (Exception ex) {
                log.warn("Failed to delete a Mastodon status for page id " + page.getId(), ex);
            }
        }
    }

    @Override
    public boolean isRegistered(Page page) {
        Long statusId = getStatusId(page);
        if (statusId == null) {
            return false;
        }

        try {
            MastodonClient client = createMastodonClient(page.getCreator());
            Statuses statuses = new Statuses(client);
            statuses.getStatus(statusId).execute();
        } catch (Mastodon4jRequestException ex) {
            if (ex.getResponse().code() != HttpStatus.SC_NOT_FOUND) {
                log.error("Mastodon returned HTTP status " + ex.getResponse().code() + " for page id " + page.getId(), ex);
            }
            return false;
        } catch (Exception ex) {
            log.warn("Failed to check Mastodon status id " + statusId + " for page id " + page.getId(), ex);
            return false;
        }

        return true;
    }

    /**
     * Finds the most recent page with the same subject and a Mastodon ID. The result can
     * be used to attach this page to an earlier tweet of the related subject.
     *
     * @param page
     *         {@link Page} that is to be tooted
     * @return Toot ID of a related earlier page with the same subject, or {@code null}
     * if there is no such page.
     */
    private Long findInReplyTo(Page page) {
        String subject = page.getSubject();
        if (subject == null) {
            return null;
        }

        try {
            return pageDao.fetchSameSubject(page).stream()
                    .map(p -> page.getProperties().get(PROPKEY_MASTODON_ID))
                    .filter(Objects::nonNull)
                    .reduce((first, second) -> second)
                    .map(Long::parseLong)
                    .orElse(null);
        } catch (NumberFormatException ex) {
            log.error("Could not parse a Mastodon ID while finding the last toot of page ID " + page.getId(), ex);
            return null;
        }
    }

    /**
     * Creates a new {@link MastodonClient} for the given user.
     *
     * @param user
     *            {@link User} to get a {@link MastodonClient} for
     * @return {@link MastodonClient}
     */
    private MastodonClient createMastodonClient(User user) {
        String instance = user.getProperties().get(PROPKEY_MASTODON_INSTANCE);
        String token = user.getProperties().get(PROPKEY_MASTODON_TOKEN);
        return mastodonServiceFactory.getMastodonClient(instance, token);
    }

    /**
     * Returns the ID of the Mastodon status.
     *
     * @param page
     *            {@link Page} to get the status ID of
     * @return Status ID, or {@code null} if the status is not published yet at Mastodon.
     */
    private Long getStatusId(Page page) {
        try {
            String mastodonId = page.getProperties().get(PROPKEY_MASTODON_ID);
            return mastodonId != null ? Long.parseLong(mastodonId) : null;
        } catch (NumberFormatException ex) {
            log.error("Cannot parse property key '" + PROPKEY_MASTODON_ID
                    + "' for page ID " + page.getId(), ex);
            return null;
        }
    }

    /**
     * Creates a Mastodon status for the given page. It consists of the page's description
     * and a link to the page. If there is no page description, the page title is used.
     * The description is truncated so the text and the location fits into one tweet.
     *
     * @param page
     *            {@link Page} to convert
     * @return Status line for Mastodon
     */
    private String statusToPost(Page page) {
        String body = page.getDescription();
        if (body == null || body.trim().isEmpty()) {
            body = page.getTitle();
        }

        String separator = " ";
        if (mastodonSeparator != null && !mastodonSeparator.trim().isEmpty()) {
            separator = " " + mastodonSeparator.trim() + " ";
        }

        int maxBodyLength = MAX_TOOT_LENGTH - separator.length() - SHORT_URL_LENGTH;
        if (body.length() > maxBodyLength) {
            StringBuilder trunc = new StringBuilder(body);
            int truncpos = trunc.lastIndexOf(" ", maxBodyLength - 1);
            if (truncpos < maxBodyLength - 15) {
                truncpos = maxBodyLength - 1;
            }
            trunc.setLength(truncpos);
            trunc.append("\u2026");
            body = trunc.toString();
        }

        if (mastodonUseTags) {
            List<String> tags = new ArrayList<>(fixedTags);
            page.getTags().stream()
                    .map(Tag::getName)
                    .map(String::trim)
                    .map(t -> t.replaceAll("(\\s|#)+", ""))
                    .distinct()
                    .forEach(tags::add);
            body = taginize(body, tags, maxBodyLength);
        }

        String pageUrl = linkService.linkTo().page(page).external().toString();

        return body + separator + pageUrl;
    }

    /**
     * Fills the body with as many tags as possible. If a tag is found as word, it is
     * replaced by the tag. Remaning tags are appended. The given maximum length will not
     * be exceeded.
     *
     * @param body
     *            Body to add tags to
     * @param tags
     *            Tags to add
     * @param maxLen
     *            Maximum body length
     * @return Body, with tags inserted
     */
    public static String taginize(String body, List<String> tags, int maxLen) {
        for (String tag : tags) {
            Pattern pat = Pattern.compile("(^.*\\b(?<!#))(" + Pattern.quote(tag) + ")(\\b.*$)");
            Matcher m = pat.matcher(body);
            if (m.matches() && body.length() + 1 <= maxLen) {
                body = m.replaceFirst("$1#$2$3");
            } else if (Pattern.matches(("(^|.*\\s)#" + Pattern.quote(tag) + "\\b.*$"), body)) {
                // Tag is already part of the body, ignore
            } else if (body.length() + tag.length() + 2 <= maxLen) {
                body += " #" + tag;
            }
        }
        return body;
    }

    /**
     * Splits a string of tags to a list of single tags.
     *
     * @param tagList
     *            List of tags, separated by non-word characters.
     * @return List of separated tags.
     */
    public static List<String> splitTags(String tagList) {
        if (tagList == null || tagList.isEmpty()) {
            return Collections.emptyList();
        }

        return Pattern.compile("[ ,;]+").splitAsStream(tagList)
                .map(String::trim)
                .map(it -> it.replaceAll("^#", ""))
                .filter(it -> !it.isEmpty())
                .distinct()
                .collect(toList());
    }

}
