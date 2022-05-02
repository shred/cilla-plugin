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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.shredzone.cilla.plugin.mastodon.MastodonPublicationServiceImpl.*;

import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link MastodonPublicationServiceImpl}.
 */
public class MastodonPublicationServiceImplTest {

    private static final int MAXLEN = 40;

    @Test
    public void splitTagsTest() {
        List<String> l1 = splitTags(null);
        assertThat(l1, is(empty()));

        List<String> l2 = splitTags("");
        assertThat(l2, is(empty()));

        List<String> l3 = splitTags("  bla  ");
        assertThat(l3, contains("bla"));

        List<String> l4 = splitTags("  bla  foo bar ");
        assertThat(l4, contains("bla", "foo", "bar"));

        List<String> l5 = splitTags("#bla, #foo, #bar");
        assertThat(l5, contains("bla", "foo", "bar"));

        List<String> l6 = splitTags("Cocktails Piña-Colada Caipirinha");
        assertThat(l6, contains("Cocktails", "Piña-Colada", "Caipirinha"));
    }

    @Test
    public void taginizerTst() {
        String t1 = taginize("Lorem Ipsum Dolor", emptyList(), MAXLEN);
        assertThat(t1, is("Lorem Ipsum Dolor"));
        assertThat(t1.length(), lessThanOrEqualTo(MAXLEN));

        String t2 = taginize("Lorem Ipsum Dolor", asList("Foo", "Bar"), MAXLEN);
        assertThat(t2, is("Lorem Ipsum Dolor #Foo #Bar"));
        assertThat(t2.length(), lessThanOrEqualTo(MAXLEN));

        String t3 = taginize("Lorem Ipsum Dolor", asList("Foo", "Ipsum", "Bar"), MAXLEN);
        assertThat(t3, is("Lorem #Ipsum Dolor #Foo #Bar"));
        assertThat(t3.length(), lessThanOrEqualTo(MAXLEN));

        String t4 = taginize("Lorem Ipsum Dolor", asList("Foo", "Lorem", "Bar"), MAXLEN);
        assertThat(t4, is("#Lorem Ipsum Dolor #Foo #Bar"));
        assertThat(t4.length(), lessThanOrEqualTo(MAXLEN));

        String t5 = taginize("Lorem Ipsum Dolor", asList("Foo", "Dolor", "Bar"), MAXLEN);
        assertThat(t5, is("Lorem Ipsum #Dolor #Foo #Bar"));
        assertThat(t5.length(), lessThanOrEqualTo(MAXLEN));

        String t6 = taginize("Lorem", asList("Foo", "Lorem", "Bar"), MAXLEN);
        assertThat(t6, is("#Lorem #Foo #Bar"));
        assertThat(t6.length(), lessThanOrEqualTo(MAXLEN));

        String t7 = taginize("Lorem Ipsum Dolor Ipsum", asList("Foo", "Ipsum", "Bar"), MAXLEN);
        assertThat(t7, is("Lorem Ipsum Dolor #Ipsum #Foo #Bar"));
        assertThat(t7.length(), lessThanOrEqualTo(MAXLEN));

        String t8 = taginize("1234567890123456789012345678901234567890", asList("Foo"), MAXLEN);
        assertThat(t8, is("1234567890123456789012345678901234567890"));
        assertThat(t8.length(), lessThanOrEqualTo(MAXLEN));

        String t9 = taginize("1234567890123456789012345678901234567", asList("Foo"), MAXLEN);
        assertThat(t9, is("1234567890123456789012345678901234567"));
        assertThat(t9.length(), lessThanOrEqualTo(MAXLEN));

        String t10 = taginize("123456789012345678901234567890123456", asList("Foo"), MAXLEN);
        assertThat(t10, is("123456789012345678901234567890123456"));
        assertThat(t10.length(), lessThanOrEqualTo(MAXLEN));

        String t11 = taginize("12345678901234567890123456789012345", asList("Foo"), MAXLEN);
        assertThat(t11, is("12345678901234567890123456789012345 #Foo"));
        assertThat(t11.length(), lessThanOrEqualTo(MAXLEN));

        String t12 = taginize("12345678901234567890123456789012345", asList("Blabla", "Foo", "Covfefe"), MAXLEN);
        assertThat(t12, is("12345678901234567890123456789012345 #Foo"));
        assertThat(t12.length(), lessThanOrEqualTo(MAXLEN));

        String t13 = taginize("Lorem Ipsumus Dolor", asList("Foo", "Ipsum", "Bar"), MAXLEN);
        assertThat(t13, is("Lorem Ipsumus Dolor #Foo #Ipsum #Bar"));
        assertThat(t13.length(), lessThanOrEqualTo(MAXLEN));

        String t14 = taginize("Lorem Unipsum Dolor", asList("Foo", "Ipsum", "Bar"), MAXLEN);
        assertThat(t14, is("Lorem Unipsum Dolor #Foo #Ipsum #Bar"));
        assertThat(t14.length(), lessThanOrEqualTo(MAXLEN));

        String t15 = taginize("Lorem Unipsumus Dolor", asList("Foo", "Ipsum", "Bar"), MAXLEN);
        assertThat(t15, is("Lorem Unipsumus Dolor #Foo #Ipsum #Bar"));
        assertThat(t15.length(), lessThanOrEqualTo(MAXLEN));

        String t16 = taginize("Lorem Ipsum-bator Dolor", asList("Foo", "Ipsum", "Bar"), MAXLEN);
        assertThat(t16, is("Lorem #Ipsum-bator Dolor #Foo #Bar"));
        assertThat(t16.length(), lessThanOrEqualTo(MAXLEN));

        String t17 = taginize("Lorem Non-Ipsum Dolor", asList("Foo", "Ipsum", "Bar"), MAXLEN);
        assertThat(t17, is("Lorem Non-#Ipsum Dolor #Foo #Bar"));
        assertThat(t17.length(), lessThanOrEqualTo(MAXLEN));

        String t18 = taginize("Lorem #Ipsum Dolor", asList("Foo", "Ipsum", "Bar"), MAXLEN);
        assertThat(t18, is("Lorem #Ipsum Dolor #Foo #Bar"));
        assertThat(t18.length(), lessThanOrEqualTo(MAXLEN));

        String t19 = taginize("Lorem #Ipsum Dolor", asList("Foo", "Ipsum", "Bar", "Foo", "Bar", "Ipsum"), MAXLEN);
        assertThat(t19, is("Lorem #Ipsum Dolor #Foo #Bar"));
        assertThat(t19.length(), lessThanOrEqualTo(MAXLEN));
    }

}
