package com.ebrahimsheyki.banking.infrastructure.aop.aspect;

import com.ebrahimsheyki.banking.infrastructure.aop.annotation.BankingOperation;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class BankingOperationAspect {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(BankingOperationAspect.class);

  /**
   * Spring AOP invokes this advice for matching methods on proxied Spring-managed beans. No direct
   * Java call site exists by design.
   */
  @SuppressWarnings("unused")
  @Around(
      value = "@annotation(bankingOperation)",
      argNames = "joinPoint,bankingOperation"
  )
  public Object logBankingOperation(
      ProceedingJoinPoint joinPoint,
      BankingOperation bankingOperation
  ) throws Throwable {

    long startTime = System.nanoTime();

    String operation = bankingOperation.value();
    String service = joinPoint.getTarget().getClass().getSimpleName();
    String method = joinPoint.getSignature().getName();
    String actor = currentActor();

    try {
      Object result = joinPoint.proceed();

      LOGGER.atInfo()
          .addKeyValue("event", "banking.operation.completed")
          .addKeyValue("operation", operation)
          .addKeyValue("service", service)
          .addKeyValue("method", method)
          .addKeyValue("actor", actor)
          .addKeyValue("outcome", "SUCCESS")
          .addKeyValue("durationMs", elapsedMillis(startTime))
          .log("Banking operation completed");

      return result;

    } catch (Throwable exception) {

      LoggingEventBuilder logEvent = exception instanceof Error
          ? LOGGER.atError()
          : LOGGER.atWarn();

      logEvent = logEvent
          .addKeyValue("event", "banking.operation.failed")
          .addKeyValue("operation", operation)
          .addKeyValue("service", service)
          .addKeyValue("method", method)
          .addKeyValue("actor", actor)
          .addKeyValue("outcome", "FAILURE")
          .addKeyValue("durationMs", elapsedMillis(startTime))
          .addKeyValue(
              "exceptionType",
              exception.getClass().getName()
          );

      if (exception instanceof Error) {
        logEvent = logEvent.setCause(exception);
      }

      logEvent.log("Banking operation failed");

      throw exception;
    }
  }


  private static String currentActor() {

    Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {

      return "anonymous";
    }

    return authentication.getName();
  }

  private static long elapsedMillis(long startTime) {

    return TimeUnit.NANOSECONDS.toMillis(
        System.nanoTime() - startTime
    );
  }
}
