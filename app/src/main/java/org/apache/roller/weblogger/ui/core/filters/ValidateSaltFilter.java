/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.roller.weblogger.ui.core.filters;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.ui.rendering.util.cache.SaltCache;
import org.apache.roller.weblogger.ui.core.RollerSession;

/**
 * Filter checks all POST request for presence of valid salt value and rejects those without
 * a salt value or with a salt value not generated by this Roller instance.
 */
public class ValidateSaltFilter implements Filter {
    private static final Log log = LogFactory.getLog(ValidateSaltFilter.class);
    private Set<String> ignored = Collections.emptySet();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;

        if ("POST".equals(httpReq.getMethod()) && !isIgnoredURL(httpReq.getServletPath())) {
            RollerSession rses = RollerSession.getRollerSession(httpReq);
            String userId = rses != null && rses.getAuthenticatedUser() != null ? rses.getAuthenticatedUser().getId() : "";

            String salt = httpReq.getParameter("salt");
            SaltCache saltCache = SaltCache.getInstance();
            if (salt == null || !Objects.equals(saltCache.get(salt), userId)) {
                if (log.isDebugEnabled()) {
                    log.debug("Valid salt value not found on POST to URL : " + httpReq.getServletPath());
                }
                throw new ServletException("Security Violation");
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        // Construct our list of ignored urls
        String urls = WebloggerConfig.getProperty("salt.ignored.urls");
        ignored = Set.of(StringUtils.stripAll(StringUtils.split(urls, ",")));
    }

    @Override
    public void destroy() {
    }

    /**
     * Checks if this is an ignored url defined in the salt.ignored.urls property
     * @param theUrl the the url
     * @return true, if is ignored resource
     */
    private boolean isIgnoredURL(String theUrl) {
        int i = theUrl.lastIndexOf('/');

        // If it's not a resource then don't ignore it
        if (i <= 0 || i == theUrl.length() - 1) {
            return false;
        }
        return ignored.contains(theUrl.substring(i + 1));
    }
}