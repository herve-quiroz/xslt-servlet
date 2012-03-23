/*
 * Copyright 2010 TranceCode
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.trancecode.web;

import com.google.common.collect.ImmutableMap;

import java.net.URI;

import org.testng.annotations.Test;

/**
 * Tests for {@link XsltServlet}.
 * 
 * @author Herve Quiroz
 */
@Test
public class XsltServletTest
{
    @Test
    public void transform() throws Exception
    {
        final URI stylesheetUri = getClass().getResource("/org/trancecode/web/test/stylesheet.xsl").toURI();
        final URI sourceUri = stylesheetUri;
        XsltServlet.transform(stylesheetUri, sourceUri, ImmutableMap.of("parameter1", "value1"), System.err);
    }
}
