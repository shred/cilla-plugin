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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.annotation.Resource;

import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.core.model.User;
import org.shredzone.cilla.core.repository.PageDao;
import org.shredzone.cilla.service.link.LinkService;
import org.shredzone.cilla.web.format.TextFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

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

    private static final int MAX_TWEET_LENGTH = 140;
    private static final String SEPARATOR = " - ";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Value("${twitter.masterEnable}") boolean twitterMasterEnabled;

    private @Resource TwitterServiceFactory twitterServiceFactory;
    private @Resource TextFormatter textFormatter;
    private @Resource PageDao pageDao;
    private @Resource LinkService linkService;

    private int shortUrlLength;
    private Instant shortUrlLengthValidUntil;

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
            Twitter twitter = createTwitterClient(page.getCreator());
            updateShortUrlLength(twitter); // make sure shortUrlLength is up to date

            String statusLine = statusToPost(page);
            Status status = twitter.updateStatus(statusLine);

            page.getProperties().put(PROPKEY_TWITTER_ID, String.valueOf(status.getId()));

            log.info("Registered page id " + page.getId() + ", Status ID " + status.getId());
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

        Long statusId = getStatusId(page);

        if (statusId != null) {
            try {
                Twitter twitter = createTwitterClient(page.getCreator());

                twitter.destroyStatus(statusId);

                page.getProperties().remove(PROPKEY_TWITTER_ID);

                log.info("Deleted page id " + page.getId() + ", Twitter status ID " + statusId);
            } catch (Exception ex) {
                log.warn("Failed to delete a Twitter status for page id " + page.getId(), ex);
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
            Twitter twitter = createTwitterClient(page.getCreator());
            twitter.tweets().showStatus(statusId);
        } catch (TwitterException ex) {
            if (ex.resourceNotFound()) {
                return false;
            }
            log.warn("Failed to check Twitter status id " + statusId + " for page id " + page.getId(), ex);
        }

        return true;
    }

    /**
     * Creates a new {@link Twitter} for the given user.
     *
     * @param user
     *            {@link User} to get a {@link Twitter} for
     * @return {@link Twitter}
     */
    private Twitter createTwitterClient(User user) {
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
    private Long getStatusId(Page page) {
        String id = page.getProperties().get(PROPKEY_TWITTER_ID);
        return (id != null ? new Long(id) : null);
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

        int maxBodyLength = MAX_TWEET_LENGTH - SEPARATOR.length() - shortUrlLength;
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

        String pageUrl = linkService.linkTo().page(page).external().toString();

        return body + SEPARATOR + pageUrl;
    }

    /**
     * Updates {@link #shortUrlLength}, which contains the current length of short URLs.
     * The result is cached, and valid for at least 24 hours. The length for shortening
     * HTTPS links is used.
     *
     * @param twitter
     *            {@link Twitter} instance to use for fetching the value
     */
    private void updateShortUrlLength(Twitter twitter) throws TwitterException {
        if (shortUrlLengthValidUntil == null || shortUrlLengthValidUntil.isBefore(Instant.now())) {
            shortUrlLength = twitter.help().getAPIConfiguration().getShortURLLengthHttps();
            shortUrlLengthValidUntil = Instant.now().plus(1, ChronoUnit.DAYS);
        }
    }

}
