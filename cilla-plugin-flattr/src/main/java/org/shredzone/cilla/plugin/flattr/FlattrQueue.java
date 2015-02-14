/*
 * cilla - Blog Management System
 *
 * Copyright (C) 2014 Richard "Shred" Körber
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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

/**
 * A queue for executing Flattr commands asynchronously.
 *
 * @author Richard "Shred" Körber
 */
@Component
public class FlattrQueue {

    private ExecutorService executor;

    @PostConstruct
    public void setup() {
        executor = Executors.newFixedThreadPool(2);
    }

    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

}
