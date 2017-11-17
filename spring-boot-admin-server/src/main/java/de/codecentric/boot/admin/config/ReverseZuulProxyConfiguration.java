/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codecentric.boot.admin.config;

import de.codecentric.boot.admin.event.RoutesOutdatedEvent;
import de.codecentric.boot.admin.registry.ApplicationRegistry;
import de.codecentric.boot.admin.web.client.HttpHeadersProvider;
import de.codecentric.boot.admin.zuul.ApplicationRouteLocator;
import de.codecentric.boot.admin.zuul.filters.pre.ApplicationHeadersFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientFactory;
import org.springframework.cloud.netflix.zuul.RoutesEndpoint;
import org.springframework.cloud.netflix.zuul.ZuulServerAutoConfiguration;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.TraceProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@AutoConfigureAfter({AdminServerWebConfiguration.class})
public class ReverseZuulProxyConfiguration extends ZuulServerAutoConfiguration {

    @Autowired(required = false)
    private TraceRepository traces;

    @Autowired
    private ApplicationRegistry registry;

    @Autowired
    private AdminServerProperties adminServer;

    @Bean
    @Order(0)
    public ApplicationRouteLocator applicationRouteLocator() {
        ApplicationRouteLocator routeLocator = new ApplicationRouteLocator(this.server.getServlet().getServletPrefix(), registry,
                adminServer.getContextPath() + "/api/applications/");
        routeLocator.setEndpoints(adminServer.getRoutes().getEndpoints());
        return routeLocator;
    }

    @Bean
    public ProxyRequestHelper proxyRequestHelper() {
        TraceProxyRequestHelper helper = new TraceProxyRequestHelper();
        if (this.traces != null) {
            helper.setTraces(this.traces);
        }
        helper.setIgnoredHeaders(this.zuulProperties.getIgnoredHeaders());
        helper.setTraceRequestBody(this.zuulProperties.isTraceRequestBody());
        return helper;
    }

    // pre filters
    @Bean
    public PreDecorationFilter preDecorationFilter(RouteLocator routeLocator) {
        return new PreDecorationFilter(routeLocator, this.server.getServlet().getServletPrefix(), zuulProperties,
                proxyRequestHelper());
    }

    @Bean
    public ApplicationHeadersFilter applicationHeadersFilter(ApplicationRouteLocator routeLocator,
                                                             HttpHeadersProvider headersProvider) {
        return new ApplicationHeadersFilter(headersProvider, routeLocator);
    }

    @Bean
    public SimpleHostRoutingFilter simpleHostRoutingFilter() {
        DefaultApacheHttpClientConnectionManagerFactory connectionFactory = new DefaultApacheHttpClientConnectionManagerFactory();
        DefaultApacheHttpClientFactory clientFactory = new DefaultApacheHttpClientFactory();
        return new SimpleHostRoutingFilter(proxyRequestHelper(), zuulProperties, connectionFactory, clientFactory);
    }

    @Bean
    @Override
    public ApplicationListener<ApplicationEvent> zuulRefreshRoutesListener() {
        return new ZuulRefreshListener(zuulHandlerMapping(null));
    }

    @Configuration
    @ConditionalOnClass(RoutesEndpoint.class)
    protected static class RoutesEndpointConfiguration {
        @Bean
        public RoutesEndpoint zuulEndpoint(RouteLocator routeLocator) {
            return new RoutesEndpoint(routeLocator);
        }
    }

    private static class ZuulRefreshListener implements ApplicationListener<ApplicationEvent> {
        private ZuulHandlerMapping zuulHandlerMapping;

        private ZuulRefreshListener(ZuulHandlerMapping zuulHandlerMapping) {
            this.zuulHandlerMapping = zuulHandlerMapping;
        }

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof PayloadApplicationEvent &&
                ((PayloadApplicationEvent<?>) event).getPayload() instanceof RoutesOutdatedEvent) {
                zuulHandlerMapping.setDirty(true);
            }
        }
    }
}
