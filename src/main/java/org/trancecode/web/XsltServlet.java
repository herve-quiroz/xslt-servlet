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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import net.sf.saxon.expr.instruct.TerminationException;
import net.sf.saxon.s9api.MessageListener;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.trancecode.logging.Logger;

/**
 * @author Herve Quiroz
 */
public class XsltServlet extends HttpServlet
{
    private static final String PROPERTY_LOGGING_LEVEL = "logging.level";

    public static final String PARAMETER_STYLESHEET_URI = "stylesheet-uri";
    public static final String PARAMETER_SOURCE_URI = "source-uri";

    private static final long serialVersionUID = -4348501590456699777L;
    private static Logger LOG = Logger.getLogger(XsltServlet.class);

    @Override
    public void init()
    {
        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.Logger.getRootLogger().addAppender(
                new ConsoleAppender(new PatternLayout("%-5p %30.30c{2} %m%n")));
        final String levelName = System.getProperty(PROPERTY_LOGGING_LEVEL);
        final Level level;
        if (levelName == null)
        {
            level = Level.INFO;
        }
        else
        {
            level = Level.toLevel(levelName);
        }
        org.apache.log4j.Logger.getLogger("org.trancecode").setLevel(level);
        LOG.debug("{@method} servlet = {}", getServletName());
        LOG.debug("  {} = {}", PARAMETER_STYLESHEET_URI, getInitParameter(PARAMETER_STYLESHEET_URI));
        LOG.debug("  {} = {}", PARAMETER_SOURCE_URI, getInitParameter(PARAMETER_SOURCE_URI));
    }

    private URI getUri(final HttpServletRequest request, final String parameterName)
    {
        final String path = getInitParameter(parameterName);
        Preconditions.checkArgument(path != null, "missing mandatory servlet parameter: %s", parameterName);
        final URL url = XsltServlet.class.getResource(path);
        Preconditions.checkArgument(url != null, "resource not found: %s", path);
        try
        {
            return url.toURI();
        }
        catch (final URISyntaxException e)
        {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException
    {
        LOG.trace("{@method} request = {}", request);
        try
        {
            if ("/favicon.ico".equals(request.getRequestURI()))
            {
                final URL iconUrl = getClass().getResource("/favicon.ico");
                ByteStreams.copy(iconUrl.openStream(), response.getOutputStream());
                return;
            }

            final URI stylesheetUri = getUri(request, PARAMETER_STYLESHEET_URI);
            LOG.trace("  stylesheetUri = {}", stylesheetUri);
            final URI sourceUri = getUri(request, PARAMETER_SOURCE_URI);
            LOG.trace("  sourceUri = {}", sourceUri);
            final Map<String, String> parameters = Maps.newHashMap();
            for (final Entry<Object, Object> property : System.getProperties().entrySet())
            {
                parameters.put(property.getKey().toString(), property.getValue().toString());
            }
            parameters.put("hostname", request.getServerName());
            for (final Enumeration<?> e = request.getParameterNames(); e.hasMoreElements();)
            {
                final String parameterName = e.nextElement().toString();
                if (!parameterName.equals(PARAMETER_SOURCE_URI) && !parameterName.equals(PARAMETER_STYLESHEET_URI))
                {
                    parameters.put(parameterName, request.getParameter(parameterName));
                }
            }
            parameters.put("request.path", request.getPathInfo());
            if (request.getQueryString() != null)
            {
                parameters.put("request.query", request.getQueryString());
            }
            response.setContentType("text/html");
            transform(stylesheetUri, sourceUri, parameters, response.getOutputStream());
        }
        catch (final Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    public static void transform(final URI stylesheetUri, final URI sourceUri, final Map<?, ?> parameters,
            final OutputStream out) throws Exception
    {
        LOG.trace("{@method} stylesheetUri = {} ; sourceUri = {}", stylesheetUri, sourceUri);

        final Processor processor = new Processor(false);
        final XsltCompiler xsltCompiler = processor.newXsltCompiler();
        final StringBuilder errorMessage = new StringBuilder();
        xsltCompiler.setErrorListener(new ErrorListener()
        {
            @Override
            public void warning(final TransformerException exception) throws TransformerException
            {
                errorMessage.append(exception.getMessageAndLocation());
                LOG.warn("{}", exception.getMessageAndLocation());
                throw exception;
            }

            @Override
            public void fatalError(final TransformerException exception) throws TransformerException
            {
                errorMessage.append(exception.getMessageAndLocation());
                LOG.error("{}", exception.getMessageAndLocation());
                throw exception;
            }

            @Override
            public void error(final TransformerException exception) throws TransformerException
            {
                errorMessage.append(exception.getMessageAndLocation());
                LOG.fatal("{}", exception.getMessageAndLocation());
                throw exception;
            }
        });
        final Source stylesheetSource = xsltCompiler.getURIResolver().resolve(stylesheetUri.toString(), "");
        final XsltExecutable xsltExecutable;
        try
        {
            xsltExecutable = xsltCompiler.compile(stylesheetSource);
        }
        catch (final SaxonApiException e)
        {
            throw new IllegalStateException(errorMessage.toString(), e);
        }
        final XsltTransformer xsltTransformer = xsltExecutable.load();
        for (final Map.Entry<?, ?> parameter : parameters.entrySet())
        {
            final String name = parameter.getKey().toString();
            final String value = parameter.getValue().toString();
            LOG.trace("  {} = {}", name, value);
            xsltTransformer.setParameter(new QName(name), new XdmAtomicValue(value));
        }

        final Serializer serializer = new Serializer();
        serializer.setOutputStream(out);
        xsltTransformer.setMessageListener(new MessageListener()
        {
            @Override
            public void message(final XdmNode content, final boolean terminate, final SourceLocator locator)
            {
                LOG.debug("{}", content);
                if (terminate)
                {
                    errorMessage.append(content);
                }
            }
        });
        xsltTransformer.setSource(xsltCompiler.getURIResolver().resolve(sourceUri.toString(), ""));
        xsltTransformer.setDestination(serializer);
        try
        {
            xsltTransformer.transform();
        }
        catch (final SaxonApiException e)
        {
            if (e.getCause() != null && e.getCause() instanceof TerminationException)
            {
                throw new IllegalStateException(errorMessage.toString(), e);
            }

            throw e;
        }
    }
}
