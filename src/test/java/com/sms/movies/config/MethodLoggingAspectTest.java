package com.sms.movies.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

class MethodLoggingAspectTest {
  private final MethodLoggingAspect aspect = new MethodLoggingAspect();

  @Test
  void returnsResultFromSuccessfulMethod() throws Throwable {
    ProceedingJoinPoint joinPoint = joinPoint("findMovie");
    Object expected = new Object();
    when(joinPoint.proceed()).thenReturn(expected);

    assertSame(expected, aspect.logMethod(joinPoint));
  }

  @Test
  void rethrowsFailureFromMethod() throws Throwable {
    ProceedingJoinPoint joinPoint = joinPoint("saveMovie");
    IllegalStateException expected = new IllegalStateException("database unavailable");
    when(joinPoint.proceed()).thenThrow(expected);

    IllegalStateException actual =
        assertThrows(IllegalStateException.class, () -> aspect.logMethod(joinPoint));

    assertEquals(expected, actual);
  }

  private ProceedingJoinPoint joinPoint(String methodName) {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(MethodLoggingAspectTest.class);
    when(signature.getName()).thenReturn(methodName);
    return joinPoint;
  }
}
