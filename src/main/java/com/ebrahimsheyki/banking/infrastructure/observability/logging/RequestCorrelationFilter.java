package com.ebrahimsheyki.banking.infrastructure.observability.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class RequestCorrelationFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String REQUEST_ID_MDC_KEY = "requestId";
  public static final String REQUEST_ID_ATTRIBUTE =
      RequestCorrelationFilter.class.getName() + ".REQUEST_ID";

  @Override
  @NullMarked
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String requestId = UUID.randomUUID().toString();
    String previousRequestId = MDC.get(REQUEST_ID_MDC_KEY);
    response.setHeader(REQUEST_ID_HEADER, requestId);
    request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
    MDC.put(REQUEST_ID_MDC_KEY, requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      if (previousRequestId == null) {
        MDC.remove(REQUEST_ID_MDC_KEY);
      } else {
        MDC.put(REQUEST_ID_MDC_KEY, previousRequestId);
      }
    }
  }
}
