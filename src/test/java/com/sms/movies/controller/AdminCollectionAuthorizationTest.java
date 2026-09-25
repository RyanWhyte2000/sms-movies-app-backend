package com.sms.movies.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class AdminCollectionAuthorizationTest {
  @Test
  void collectionMaintenanceRequiresAdminRole() {
    assertAdminOnly(CollectionController.class);
  }

  @Test
  void shareManagementRequiresAdminRole() {
    assertAdminOnly(ShareController.class);
  }

  @Test
  void publicSharedCollectionReadsDoNotRequireAdminRole() {
    assertEquals(null, SharedCollectionController.class.getAnnotation(PreAuthorize.class));
  }

  private static void assertAdminOnly(Class<?> controller) {
    var authorization = controller.getAnnotation(PreAuthorize.class);
    assertEquals("hasRole('ADMIN')", authorization.value());
  }
}
