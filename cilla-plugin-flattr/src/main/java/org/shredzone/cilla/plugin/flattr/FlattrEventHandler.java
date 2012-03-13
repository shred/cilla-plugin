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

import javax.annotation.Resource;

import org.shredzone.cilla.core.event.EventType;
import org.shredzone.cilla.core.event.annotation.EventHandler;
import org.shredzone.cilla.core.event.annotation.OnEvent;
import org.shredzone.cilla.core.model.Page;
import org.springframework.stereotype.Component;

/**
 * Handles page events for creating, updating and deleting Flattr things.
 *
 * @author Richard "Shred" Körber
 */
@Component
@EventHandler
public class FlattrEventHandler {
    private @Resource FlattrPublicationService flattrPublicationService;

    /**
     * Publish a Page to Flattr.
     *
     * @param page {@link Page} that has been published
     */
    @OnEvent(event = EventType.PAGE_PUBLISH)
    public void publish(Page page) {
        if (page.isDonatable() && !flattrPublicationService.isRegistered(page)) {
            flattrPublicationService.publish(page);
        }
    }

    /**
     * Update a Page at Flattr.
     *
     * @param page {@link Page} that was updated
     */
    @OnEvent(event = EventType.PAGE_UPDATE)
    public void update(Page page) {
        if (page.isPublishedState()) {
            boolean hasFlattrId = flattrPublicationService.isRegistered(page);
            if (page.isDonatable() && !hasFlattrId) {
                flattrPublicationService.publish(page);
            } else if (page.isDonatable() && hasFlattrId) {
                flattrPublicationService.update(page);
            } else if (!page.isDonatable() && hasFlattrId) {
                flattrPublicationService.remove(page);
            }
        }
    }

    /**
     * Remove a Page at Flattr.
     *
     * @param page {@link Page} that has been unpublished or deleted
     */
    @OnEvent(event = { EventType.PAGE_UNPUBLISH, EventType.PAGE_DELETE })
    public void unpublish(Page page) {
        if (flattrPublicationService.isRegistered(page)) {
            flattrPublicationService.remove(page);
        }
    }

}
