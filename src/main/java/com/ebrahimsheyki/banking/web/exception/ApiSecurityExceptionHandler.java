package com.ebrahimsheyki.banking.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class ApiSecurityExceptionHandler
    implements AuthenticationEntryPoint, AccessDeniedHandler {

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
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {
    bearerTokenAuthenticationEntryPoint.commence(request, response, exception);
    writeProblem(request, response, HttpStatus.UNAUTHORIZED, "Authentication required",
        "A valid access token is required.", "AUTHENTICATION_REQUIRED");
  }

  @Override
  @NullMarked
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException exception) throws IOException {
    bearerTokenAccessDeniedHandler.handle(request, response, exception);
    writeProblem(request, response, HttpStatus.FORBIDDEN, "Access denied",
        "You do not have permission to perform this operation.", "ACCESS_DENIED");
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
    jsonMapper.writeValue(response.getOutputStream(), problem);
  }
}
