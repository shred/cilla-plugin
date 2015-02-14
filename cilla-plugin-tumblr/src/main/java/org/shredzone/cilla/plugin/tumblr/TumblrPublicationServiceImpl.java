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
package org.shredzone.cilla.plugin.tumblr;

import static java.util.stream.Collectors.toList;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.shredzone.cilla.core.model.Category;
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

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.TextPost;

/**
 * Default implementation of {@link TumblrPublicationService}.
 *
 * @author Richard "Shred" Körber
 */
@Service
@Transactional
public class TumblrPublicationServiceImpl implements TumblrPublicationService {
    public static final String PROPKEY_TUMBLR_ID = "tumblr.id";
    public static final String PROPKEY_TUMBLR_BLOGNAME = "tumblr.blogname";
    public static final String PROPKEY_TUMBLR_TOKEN = "tumblr.token";
    public static final String PROPKEY_TUMBLR_SECRET = "tumblr.secret";

    private static final Pattern AUTOTAG_SEPARATOR = Pattern.compile(",");

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Value("${tumblr.masterEnable}") boolean tumblrMasterEnabled;
    private @Value("${tumblr.autotags}") String tumblrAutotags;
    private @Value("${tumblr.state}") String tumblrState;

    private @Resource JumblrServiceFactory jumblrServiceFactory;
    private @Resource TextFormatter textFormatter;
    private @Resource PageDao pageDao;
    private @Resource LinkService linkService;

    @Override
    public void publish(Page page) {
        if (!tumblrMasterEnabled) {
            log.info("Tumblr handling is disabled");
            return;
        }

        if (isRegistered(page)) {
            log.warn("Attempt to resubmit an already submitted page at Tumblr (page id {})", page.getId());
            return;
        }

        User creator = page.getCreator();
        String blogName = creator.getProperties().get(PROPKEY_TUMBLR_BLOGNAME);

        if (blogName == null) {
            log.info("User " + creator.getLogin() + " has no tumbler blog name set");
            return;
        }

        try {
            JumblrClient client = createJumblrClient(page.getCreator());

            TextPost post = client.newPost(blogName, TextPost.class);
            pageToPost(page, post);
            post.save();

            page.getProperties().put(PROPKEY_TUMBLR_ID, post.getId().toString());
            page.getProperties().put(PROPKEY_TUMBLR_BLOGNAME, post.getBlogName());

            log.info("Registered page id " + page.getId() + ", Tumblr ID " + post.getId()
                            + " at blog " + post.getBlogName() + " with state " + tumblrState);
        } catch (Exception ex) {
            log.warn("Failed to submit a Tumblr for page id " + page.getId(), ex);
        }
    }

    @Override
    public void update(Page page) {
        if (!tumblrMasterEnabled) {
            log.info("Tumblr handling is disabled");
            return;
        }

        if (!isRegistered(page)) {
            log.warn("Attempt to update an unsubmitted page at Tumblr (page id {})", page.getId());
            return;
        }

        JumblrClient client = createJumblrClient(page.getCreator());
        Long postId = getPostId(page);

        if (postId != null) {
            try {
                String blogName = page.getProperties().get(PROPKEY_TUMBLR_BLOGNAME);
                TextPost post = (TextPost) client.blogPost(blogName, postId);
                pageToPost(page, post);
                post.save();

                log.info("Updated page id " + page.getId() + ", Tumblr ID " + post.getId() + " at blog " + post.getBlogName());
            } catch (Exception ex) {
                log.warn("Failed to update a Tumblr for page id " + page.getId(), ex);
            }
        }
    }

    @Override
    public void remove(Page page) {
        if (!tumblrMasterEnabled) {
            log.info("Tumblr handling is disabled");
            return;
        }

        JumblrClient client = createJumblrClient(page.getCreator());
        Long postId = getPostId(page);

        if (postId != null) {
            try {
                String blogName = page.getProperties().get(PROPKEY_TUMBLR_BLOGNAME);
                client.postDelete(blogName, postId);

                page.getProperties().remove(PROPKEY_TUMBLR_ID);
                page.getProperties().remove(PROPKEY_TUMBLR_BLOGNAME);

                log.info("Deleted page id " + page.getId() + ", Tumblr ID " + postId
                    + " at blog " + blogName);
            } catch (Exception ex) {
                log.warn("Failed to delete a Tumblr for page id " + page.getId(), ex);
            }
        }
    }

    @Override
    public boolean isRegistered(Page page) {
        return page.getProperties().containsKey(PROPKEY_TUMBLR_ID);
    }

    /**
     * Creates a new {@link JumblrClient} for the given user.
     *
     * @param user
     *            {@link User} to get a {@link JumblrClient} for
     * @return {@link JumblrClient}
     */
    private JumblrClient createJumblrClient(User user) {
        String token = user.getProperties().get(PROPKEY_TUMBLR_TOKEN);
        String secret = user.getProperties().get(PROPKEY_TUMBLR_SECRET);
        return jumblrServiceFactory.getJumblrClient(token, secret);
    }

    /**
     * Returns the {@link Post} ID of the page at Tumblr.
     *
     * @param page
     *            {@link Page} to get the {@link Post} ID of
     * @return {@link Post} ID, or {@code null} if the page is not published at Tumblr.
     */
    private Long getPostId(Page page) {
        String id = page.getProperties().get(PROPKEY_TUMBLR_ID);
        return (id != null ? new Long(id) : null);
    }

    /**
     * Converts a {@link Page} to a {@link TextPost}.
     *
     * @param page
     *            {@link Page} to convert from
     * @param post
     *            {@link TextPost} to convert into
     */
    private void pageToPost(Page page, TextPost post) {
        try {
            String url = linkService.linkTo().page(page).external().toString();

            StringBuilder sb = new StringBuilder();
            sb.append(textFormatter.format(page.getTeaser()));
            sb.append("<p><a href=").append(URLEncoder.encode(url, "utf8")).append('>').append(url).append("</a></p>");

            post.setDate(page.getPublication());
            post.setTitle(page.getTitle());
            post.setBody(sb.toString());
            post.setState(tumblrState);

            List<String> tags = new ArrayList<>();
            tags.addAll(page.getCategories().stream().map(Category::getName).collect(toList()));
            tags.addAll(page.getTags().stream().map(Tag::getName).collect(toList()));
            if (tumblrAutotags != null && !tumblrAutotags.isEmpty()) {
                tags.addAll(AUTOTAG_SEPARATOR.splitAsStream(tumblrAutotags).map(String::trim).collect(toList()));
            }
            post.setTags(tags);
        } catch (UnsupportedEncodingException ex) {
            // Should never happen, as utf8 is standard
            throw new InternalError(ex);
        }
    }

}
