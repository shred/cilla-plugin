/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2013 Richard "Shred" Körber
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
package org.shredzone.cilla.plugin.flattr.collector;

import org.shredzone.flattr4j.model.ThingId;

/**
 * A simple {@link ThingId} implementation that can be used in maps etc.
 *
 * @author Richard "Shred" Körber
 */
public class SomeThingId implements ThingId {

    private final String thingId;

    public SomeThingId(ThingId thingId) {
        this.thingId = thingId.getThingId();
    }

    @Override
    public String getThingId() {
        return thingId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ThingId)) {
            return false;
        }

        return thingId.equals(((ThingId) obj).getThingId());
    }

    @Override
    public int hashCode() {
        return thingId.hashCode();
    }

    @Override
    public String toString() {
        return thingId;
    }

}
