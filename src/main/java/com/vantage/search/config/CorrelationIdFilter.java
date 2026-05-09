package com.vantage.search.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

/**
 * Injects a unique trace ID into the MDC and response headers, enabling
 * request traceability across logs and client-side troubleshooting.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {
    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String incoming = (request instanceof jakarta.servlet.http.HttpServletRequest r)
                ? r.getHeader("X-Trace-Id") : null;
        String traceId = (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();

        try {
            MDC.put(TRACE_ID_KEY, traceId);
            if (response instanceof HttpServletResponse httpResponse) {

                httpResponse.setHeader("X-Trace-Id", traceId);
            }
            chain.doFilter(request, response);
        } finally {
            // Prevent context leakage across reused thread pool threads
            MDC.remove(TRACE_ID_KEY);
        }
    }
}