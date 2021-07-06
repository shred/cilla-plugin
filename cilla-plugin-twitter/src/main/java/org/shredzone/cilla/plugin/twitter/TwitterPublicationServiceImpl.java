/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2017 Richard "Shred" Körber
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
package org.shredzone.cilla.plugin.twitter;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.Tweet;
import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.core.model.Tag;
import org.shredzone.cilla.core.model.User;
import org.shredzone.cilla.core.repository.PageDao;
import org.shredzone.cilla.service.link.LinkService;
import org.shredzone.cilla.web.format.TextFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link TwitterPublicationService}.
 *
 * @author Richard "Shred" Körber
 */
@Service
@Transactional
public class TwitterPublicationServiceImpl implements TwitterPublicationService {
    public static final String PROPKEY_TWITTER_ID = "twitter.id";
    public static final String PROPKEY_TWITTER_TOKEN = "twitter.token";
    public static final String PROPKEY_TWITTER_SECRET = "twitter.secret";

    private static final int MAX_TWEET_LENGTH = 280; // TODO: read from API
    private static final int SHORT_URL_LENGTH = 23;  // TODO: read from API

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Value("${twitter.masterEnable}") boolean twitterMasterEnabled;
    private @Value("${twitter.useTags}") boolean twitterUseTags;
    private @Value("${twitter.fixedTags}") String twitterFixedTags;
    private @Value("${twitter.separator}") String twitterSeparator;
    private @Value("${twitter.threading}") boolean twitterThreading;

    private @Resource TwitterServiceFactory twitterServiceFactory;
    private @Resource TextFormatter textFormatter;
    private @Resource PageDao pageDao;
    private @Resource LinkService linkService;

    private List<String> fixedTags;

    @PostConstruct
    public void setup() {
        fixedTags = Collections.unmodifiableList(splitTags(twitterFixedTags));
    }

    @Override
    public void publish(Page page) {
        if (!twitterMasterEnabled) {
            log.info("Twitter handling is disabled");
            return;
        }

        if (isRegistered(page)) {
            log.warn("Attempt to resubmit an already submitted page at Twitter (page id {})", page.getId());
            return;
        }


        try {
            TwitterClient twitter = createTwitterClient(page.getCreator());

            String statusLine = statusToPost(page);

            String inReplyTo = twitterThreading ? findInReplyTo(page) : null;
            Tweet tweet;
            if (inReplyTo != null) {
                tweet = twitter.postTweet(statusLine, inReplyTo);
            } else {
                tweet = twitter.postTweet(statusLine);
            }

            page.getProperties().put(PROPKEY_TWITTER_ID, tweet.getId());

            StringBuilder sb = new StringBuilder("Registered page id ");
            sb.append(page.getId()).append(", Status ID ").append(tweet.getId());
            if (inReplyTo != null) {
                sb.append(", in reply to Status ID ").append(inReplyTo);
            }
            log.info(sb.toString());
        } catch (Exception ex) {
            log.warn("Failed to submit a Twitter status for page id " + page.getId(), ex);
        }
    }

    @Override
    public void remove(Page page) {
        if (!twitterMasterEnabled) {
            log.info("Twitter handling is disabled");
            return;
        }

        String statusId = getStatusId(page);

        if (statusId != null) {
            try {
                TwitterClient twitter = createTwitterClient(page.getCreator());

                twitter.deleteTweet(statusId);

                page.getProperties().remove(PROPKEY_TWITTER_ID);

                log.info("Deleted page id " + page.getId() + ", Twitter status ID " + statusId);
            } catch (Exception ex) {
                log.warn("Failed to delete a Twitter status for page id " + page.getId(), ex);
            }
        }
    }

    @Override
    public boolean isRegistered(Page page) {
        String statusId = getStatusId(page);
        if (statusId == null) {
            return false;
        }

        try {
            TwitterClient twitter = createTwitterClient(page.getCreator());
            twitter.getTweet(statusId);
        } catch (NoSuchElementException ex) {
            return false;
        } catch (Exception ex) {
            log.warn("Failed to check Twitter status id " + statusId + " for page id " + page.getId(), ex);
            return false;
        }

        return true;
    }

    /**
     * Finds the most recent page with the same subject and a twitter ID. The result can
     * be used to attach this page to an earlier tweet of the related subject.
     *
     * @param page
     *         {@link Page} that is to be tweeted
     * @return Tweet ID of a related earlier page with the same subject, or {@code null}
     * if there is no such page.
     */
    private String findInReplyTo(Page page) {
        String subject = page.getSubject();
        if (subject == null) {
            return null;
        }

        return pageDao.fetchSameSubject(page).stream()
                .map(p -> page.getProperties().get(PROPKEY_TWITTER_ID))
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
     * Creates a new {@link TwitterClient} for the given user.
     *
     * @param user
     *            {@link User} to get a {@link TwitterClient} for
     * @return {@link TwitterClient}
     */
    private TwitterClient createTwitterClient(User user) {
        String token = user.getProperties().get(PROPKEY_TWITTER_TOKEN);
        String secret = user.getProperties().get(PROPKEY_TWITTER_SECRET);
        return twitterServiceFactory.getTwitterClient(token, secret);
    }

    /**
     * Returns the ID of the Twitter status.
     *
     * @param page
     *            {@link Page} to get the status ID of
     * @return Status ID, or {@code null} if the status is not published yet at Twitter.
     */
    private String getStatusId(Page page) {
        return page.getProperties().get(PROPKEY_TWITTER_ID);
    }

    /**
     * Creates a Twitter status for the given page. It consists of the page's description
     * and a link to the page. If there is no page description, the page title is used.
     * The description is truncated so the text and the location fits into one tweet.
     *
     * @param page
     *            {@link Page} to convert
     * @return Status line for twitter
     */
    private String statusToPost(Page page) {
        String body = page.getDescription();
        if (body == null || body.trim().isEmpty()) {
            body = page.getTitle();
        }

        String separator = " ";
        if (twitterSeparator != null && !twitterSeparator.trim().isEmpty()) {
            separator = " " + twitterSeparator.trim() + " ";
        }

        int maxBodyLength = MAX_TWEET_LENGTH - separator.length() - SHORT_URL_LENGTH;
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

        if (twitterUseTags) {
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
