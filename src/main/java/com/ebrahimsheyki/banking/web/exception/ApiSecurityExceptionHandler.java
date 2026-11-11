package com.ebrahimsheyki.banking.web.exception;

import com.ebrahimsheyki.banking.infrastructure.observability.logging.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class ApiSecurityExceptionHandler
    implements AuthenticationEntryPoint, AccessDeniedHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ApiSecurityExceptionHandler.class);
  private final JsonMapper jsonMapper;
  private final BearerTokenAuthenticationEntryPoint bearerTokenAuthenticationEntryPoint =
      new BearerTokenAuthenticationEntryPoint();
  private final BearerTokenAccessDeniedHandler bearerTokenAccessDeniedHandler =
      new BearerTokenAccessDeniedHandler();

  public ApiSecurityExceptionHandler(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  @NullMarked
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception
  ) throws IOException {

    logAuthenticationFailure(request, exception);

    bearerTokenAuthenticationEntryPoint.commence(
        request,
        response,
        exception
    );

    writeProblem(
        request,
        response,
        HttpStatus.UNAUTHORIZED,
        "Authentication required",
        "A valid access token is required.",
        "AUTHENTICATION_REQUIRED"
    );
  }

  @Override
  @NullMarked
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException exception
  ) throws IOException {

    logAuthorizationDenied(request, exception);

    bearerTokenAccessDeniedHandler.handle(
        request,
        response,
        exception
    );

    writeProblem(
        request,
        response,
        HttpStatus.FORBIDDEN,
        "Access denied",
        "You do not have permission to perform this operation.",
        "ACCESS_DENIED"
    );
  }

  private static void logAuthenticationFailure(
      HttpServletRequest request,
      AuthenticationException exception
  ) {

    LoggingEventBuilder event = LOGGER.atWarn()
        .addKeyValue("event", "security.authentication.failure")
        .addKeyValue("outcome", "FAILURE")
        .addKeyValue("reason", "AUTHENTICATION_REQUIRED")
        .addKeyValue("httpMethod", request.getMethod())
        .addKeyValue("requestUri", request.getRequestURI())
        .addKeyValue("exceptionType", exception.getClass().getName());

    event.log("Authentication failed");
  }

  private static void logAuthorizationDenied(
      HttpServletRequest request,
      AccessDeniedException exception
  ) {

    LoggingEventBuilder event = LOGGER.atWarn()
        .addKeyValue("event", "security.authorization.denied")
        .addKeyValue("outcome", "DENIED")
        .addKeyValue("reason", "ACCESS_DENIED")
        .addKeyValue("actor", currentActor())
        .addKeyValue("httpMethod", request.getMethod())
        .addKeyValue("requestUri", request.getRequestURI())
        .addKeyValue("exceptionType", exception.getClass().getName());

    event.log("Authorization denied");
  }

  private static String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {

      return "anonymous";
    }

    return authentication.getName();
  }

  private void writeProblem(HttpServletRequest request, HttpServletResponse response,
      HttpStatus status, String title, String detail, String code) throws IOException {
    if (response.isCommitted()) {
      return;
    }
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code);

    Object requestId = request.getAttribute(
        RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE
    );

    if (requestId instanceof String id && !id.isBlank()) {
      problem.setProperty("requestId", id);
    }

    jsonMapper.writeValue(response.getOutputStream(), problem);
  }
}
