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

import javax.annotation.Resource;

import org.shredzone.cilla.core.event.EventType;
import org.shredzone.cilla.core.event.annotation.EventListener;
import org.shredzone.cilla.core.event.annotation.OnEvent;
import org.shredzone.cilla.core.model.Page;
import org.springframework.stereotype.Component;

/**
 * Handles page events for creating, updating and deleting tweets.
 *
 * @author Richard "Shred" Körber
 */
@Component
@EventListener
public class TwitterEventListener {

    private @Resource TwitterPublicationService twitterPublicationService;

    /**
     * Publish a Page to Twitter.
     *
     * @param page {@link Page} that has been published
     */
    @OnEvent(EventType.PAGE_PUBLISH)
    public void onPagePublish(Page page) {
        if (page.isPromoted() && !twitterPublicationService.isRegistered(page)) {
            twitterPublicationService.publish(page);
        }
    }

    /**
     * Update a Page at Twitter.
     *
     * @param page {@link Page} that was updated
     */
    @OnEvent(EventType.PAGE_UPDATE)
    public void onPageUpdate(Page page) {
        if (page.isPublishedState() && page.getPublication() != null) {
            boolean isRegistered = twitterPublicationService.isRegistered(page);
            if (page.isPromoted() && !isRegistered) {
                twitterPublicationService.publish(page);
            } else if (page.isPromoted() && isRegistered) {
                // Tweets cannot be updated, so ignore this event. If the user wants to
                // update a tweet, he needs to delete it manually before updating the
                // page.
            } else if (!page.isPromoted() && isRegistered) {
                twitterPublicationService.remove(page);
            }
        }
    }

    /**
     * Remove a Page at Twitter.
     *
     * @param page {@link Page} that has been unpublished or deleted
     */
    @OnEvent({ EventType.PAGE_UNPUBLISH, EventType.PAGE_DELETE })
    public void onPageUnpublish(Page page) {
        if (twitterPublicationService.isRegistered(page)) {
            twitterPublicationService.remove(page);
        }
    }

}
