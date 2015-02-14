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

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;

import org.shredzone.cilla.core.model.Category;
import org.shredzone.cilla.core.model.Page;
import org.shredzone.cilla.core.model.Tag;
import org.shredzone.cilla.core.model.Token;
import org.shredzone.cilla.core.model.User;
import org.shredzone.cilla.core.repository.PageDao;
import org.shredzone.cilla.core.repository.TokenDao;
import org.shredzone.cilla.plugin.flattr.collector.ClickFuture;
import org.shredzone.cilla.plugin.flattr.collector.CollectingClickExecutor;
import org.shredzone.cilla.service.link.LinkService;
import org.shredzone.cilla.web.format.TextFormatter;
import org.shredzone.flattr4j.FlattrService;
import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.model.LanguageId;
import org.shredzone.flattr4j.model.Submission;
import org.shredzone.flattr4j.model.Thing;
import org.shredzone.flattr4j.model.ThingId;
import org.shredzone.flattr4j.model.UserId;
import org.shredzone.flattr4j.oauth.AccessToken;
import org.shredzone.flattr4j.spring.FlattrServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link FlattrPublicationService}.
 *
 * @author Richard "Shred" Körber
 */
@Service
@Transactional
public class FlattrPublicationServiceImpl implements FlattrPublicationService {
    public static final String FLATTR_SITE_ID = "flattr";
    public static final String PROPKEY_FLATTR_ID = "flattr.id";
    public static final String PROPKEY_FLATTR_OWNER = "flattr.owner";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Value("${flattr.masterEnable?:false}") boolean flattrMasterEnabled;
    private @Value("${flattr.hidden?:false}") boolean flattrHidden;
    private @Value("${flattr.republish?:false}") boolean flattrRepublish;
    private @Value("${flattr.category}") String category;
    private @Value("${flattr.autotags}") String flattrAutotags;

    private @Resource FlattrServiceFactory flattrServiceFactory;
    private @Resource CollectingClickExecutor clickExecutor;
    private @Resource FlattrLanguage flattrLanguage;
    private @Resource TextFormatter textFormatter;
    private @Resource PageDao pageDao;
    private @Resource TokenDao tokenDao;
    private @Resource LinkService linkService;

    @Override
    public void publish(Page page) {
        if (!flattrMasterEnabled) {
            log.info("Flattr handling is disabled");
            return;
        }

        if (isRegistered(page)) {
            log.warn("Attempt to resubmit an already submitted page at flattr (page id {})", page.getId());
            return;
        }

        Submission submission = createSubmissionForPage(page);
        if (submission == null) {
            // A Thing could not be created for this page
            return;
        }

        AccessToken at = getAccessTokenForUser(page.getCreator());
        if (at != null) {
            try {
                FlattrService service = flattrServiceFactory.getFlattrService(at);

                Thing thing = service.getThing(service.create(submission));

                page.setDonateUrl(thing.getLink());
                page.getProperties().put(PROPKEY_FLATTR_ID, thing.getThingId());
                page.getProperties().put(PROPKEY_FLATTR_OWNER, thing.getUserId());

                log.info("Registered page id " + page.getId() + ", Flattr thing " + thing.getThingId());
            } catch (FlattrException ex) {
                log.warn("Failed to submit a Flattr Thing for page id " + page.getId(), ex);
            }
        }
    }

    @Override
    public void update(Page page) {
        if (!flattrMasterEnabled) {
            log.info("Flattr handling is disabled");
            return;
        }

        AccessToken at = getAccessTokenForUser(page.getCreator());
        ThingId thingId = getFlattrThing(page);

        if (thingId != null && at != null) {
            try {
                FlattrService service = flattrServiceFactory.getFlattrService(at);

                Submission submission = createSubmissionForPage(page);
                if (submission == null) {
                    // The update would be invalid
                    log.warn("Failed to update a Flattr Thing for page id " + page.getId()
                            + " because the new content would be invalid.");
                    return;
                }

                // URL cannot be changed if the page is already registered with Flattr
                submission.setUrl(null);

                Thing thing = service.getThing(thingId);
                thing.merge(submission);

                service.update(thing);

                log.info("Updated page id " + page.getId() + ", Flattr thing " + thing.getThingId());
            } catch (FlattrException ex) {
                log.warn("Failed to update a Flattr Thing for page id " + page.getId(), ex);
            }
        }
    }

    @Override
    public void remove(Page page) {
        if (!flattrMasterEnabled) {
            log.info("Flattr handling is disabled");
            return;
        }

        AccessToken at = getAccessTokenForUser(page.getCreator());
        ThingId thingId = getFlattrThing(page);

        if (thingId != null && at != null) {
            try {
                FlattrService service = flattrServiceFactory.getFlattrService(at);
                service.delete(thingId);

                page.setDonateUrl(null);
                page.getProperties().remove(PROPKEY_FLATTR_ID);
                page.getProperties().remove(PROPKEY_FLATTR_OWNER);

                log.info("Deleted page id " + page.getId() + ", Flattr thing " + thingId.getThingId());
            } catch (FlattrException ex) {
                log.warn("Failed to delete a Flattr Thing for page id " + page.getId(), ex);
            }
        }
    }

    @Override
    public boolean isRegistered(Page page) {
        return page.getProperties().containsKey(PROPKEY_FLATTR_ID);
    }

    @Override
    public ThingId getFlattrThing(Page page) {
        String fid = page.getProperties().get(PROPKEY_FLATTR_ID);
        return (fid != null ? Thing.withId(fid) : null);
    }

    @Override
    public UserId getFlattrThingOwner(Page page) {
        String oid = page.getProperties().get(PROPKEY_FLATTR_OWNER);
        if (oid == null && isRegistered(page)) {
            // Migration is required: older versions did not store flattr owner.
            ThingId tid = getFlattrThing(page);
            try {
                Thing thing = flattrServiceFactory.getOpenService().getThing(tid);
                oid = thing.getUserId();
                page.getProperties().put(PROPKEY_FLATTR_OWNER, oid);
            } catch (FlattrException ex) {
                log.error("Cound not get flattr owner of thing ID " + tid, ex);
            }
        }
        return (oid != null ? org.shredzone.flattr4j.model.User.withId(oid) : null);
    }

    @Override
    @Cacheable(value = "flattrCounts", key = "#thingId.thingId")
    public int clickCount(ThingId thingId) {
        if (thingId != null) {
            try {
                ClickFuture future = new ClickFuture(thingId);
                clickExecutor.submit(future);
                Integer count = future.get();
                return (count != null ? count : 0);
            } catch (InterruptedException | ExecutionException ex) {
                log.error("Failed to get flattr counts for thing ID " + thingId.getThingId(), ex);
            }
        }

        return 0;
    }

    @Override
    public void unpublished(ThingId thingId) {
        for (Page page : pageDao.fetchHavingProperty(PROPKEY_FLATTR_ID, thingId.getThingId())) {
            page.setDonateUrl(null);
            page.getProperties().remove(PROPKEY_FLATTR_ID);
            page.getProperties().remove(PROPKEY_FLATTR_OWNER);
            if (!flattrRepublish) {
                page.setDonatable(false);
            }
            log.info("Unregistered page id " + page.getId() + ", Flattr thing " + thingId);
        }
    }

    /**
     * Gets an {@link AccessToken} for the given user.
     *
     * @param user
     *            {@link User} to get a Flattr {@link AccessToken} for
     * @return {@link AccessToken}, or {@code null} if the user has none
     */
    private AccessToken getAccessTokenForUser(User user) {
        Token token = tokenDao.fetch(user, FLATTR_SITE_ID);
        return (token != null ? new AccessToken(token.getToken()) : null);
    }

    /**
     * Creates a {@link Submission} object for the {@link Page}.
     *
     * @param page
     *            {@link Page} to create a {@link Submission} for
     * @return generated {@link Submission}, or {@code null} if a {@link Submission} could
     *         not be generated because of lacking information or other problems.
     */
    private Submission createSubmissionForPage(Page page) {
        Submission thing = new Submission();
        thing.setCategory(org.shredzone.flattr4j.model.Category.withId(category));

        CharSequence title = prepare(page.getTitle(), 100);
        if (title.length() < 5) {
            // Title would be too short...
            return null;
        }
        thing.setTitle(title.toString());

        CharSequence description = textFormatter.format(page.getTeaser());
        description = prepare(description, 1000);
        if (description.length() < 5) {
            // Description would be too short...
            return null;
        }
        thing.setDescription(description.toString());

        LanguageId language = flattrLanguage.findLanguageId(page.getLanguage().getLocale());
        if (language == null) {
            // No matching language was found...
            return null;
        }
        thing.setLanguage(language);

        page.getCategories().stream().map(Category::getName).forEach(thing::addTag);
        page.getTags().stream().map(Tag::getName).forEach(thing::addTag);

        if (flattrAutotags != null && !flattrAutotags.isEmpty()) {
            Arrays.stream(flattrAutotags.split(",")).map(String::trim).forEach(thing::addTag);
        }

        thing.setHidden(flattrHidden);
        thing.setUrl(linkService.linkTo().page(page).external().toString());

        return thing;
    }

    /**
     * Prepares a {@link CharSequence}. Strips all HTML and limits its length to the given
     * maximum length.
     *
     * @param str
     *            {@link CharSequence} to prepare
     * @param maxlen
     *            maximum length
     * @return prepared {@link CharSequence}
     */
    private CharSequence prepare(CharSequence str, int maxlen) {
        CharSequence result = textFormatter.stripHtml(str.toString().trim());
        if (result.length() > maxlen) {
            return result.subSequence(0, maxlen);
        } else {
            return result;
        }
    }

}
