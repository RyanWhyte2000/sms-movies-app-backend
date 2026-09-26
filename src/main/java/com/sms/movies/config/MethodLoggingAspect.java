package com.sms.movies.config;

import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** Logs entry, exit, elapsed time, and failures for application-layer methods. */
@Aspect
@Component
public class MethodLoggingAspect {
  private static final Logger log = LoggerFactory.getLogger(MethodLoggingAspect.class);

  @Around(
      "(execution(public * com.sms.movies.controller..*(..))"
          + " || execution(public * com.sms.movies.service..*(..))"
          + " || execution(public * com.sms.movies.repository..*(..))"
          + " || execution(public * com.sms.movies.security..*(..))"
          + " || execution(public * com.sms.movies.exception..*(..))"
          + " || execution(public * com.sms.movies.config..*(..)))"
          + " && !within(com.sms.movies.config.MethodLoggingAspect)")
  public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    String correlationId = Optional.ofNullable(MDC.get("correlationId")).orElse("none");
    String invocationId = UUID.randomUUID().toString();
    long startedAt = System.nanoTime();

    log.info(
        "ENTER method={} correlationId={} invocationId={}", method, correlationId, invocationId);
    try {
      Object result = joinPoint.proceed();
      log.info(
          "EXIT method={} correlationId={} invocationId={} durationMs={}",
          method,
          correlationId,
          invocationId,
          elapsedMilliseconds(startedAt));
      return result;
    } catch (Throwable failure) {
      log.error(
          "THROW method={} correlationId={} invocationId={} durationMs={}",
          method,
          correlationId,
          invocationId,
          elapsedMilliseconds(startedAt),
          failure);
      throw failure;
    }
  }

  private long elapsedMilliseconds(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000;
  }
}
