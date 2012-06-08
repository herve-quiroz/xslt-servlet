/*
 * Copyright 2012 TranceCode
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

import java.util.ServiceLoader;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * @author Herve Quiroz
 */
public final class Launcher
{
    public static void main(final String[] args) throws Exception
    {
        final Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        for (final ServletMapping mapping : ServiceLoader.load(ServletMapping.class))
        {
            final ServletHolder holder = new ServletHolder(new XsltServlet());
            holder.setInitParameter(XsltServlet.PARAMETER_SOURCE_URI, mapping.getSourcePath());
            holder.setInitParameter(XsltServlet.PARAMETER_STYLESHEET_URI, mapping.getStylesheetPath());
            context.addServlet(holder, mapping.getContextPath());
        }
        server.start();
        server.join();
    }
}
