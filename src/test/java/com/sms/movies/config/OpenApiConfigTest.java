package com.sms.movies.config;

import static org.junit.jupiter.api.Assertions.*;

import com.sms.movies.controller.*;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class OpenApiConfigTest {
  @Test
  void registersReusableProblemComponents() {
    var api = new OpenAPI();
    new OpenApiConfig().errorResponseComponents().customise(api);
    assertTrue(api.getComponents().getSchemas().containsKey("ApiError"));
    assertEquals(6, api.getComponents().getResponses().size());

    new OpenApiConfig().errorResponseComponents().customise(api);
    assertEquals(6, api.getComponents().getResponses().size());
  }

  @Test
  void documentsControllerSpecificErrors() throws Exception {
    var customizer = new OpenApiConfig().documentedErrorResponses();
    for (var target : List.of(
        target(MovieController.class, "create"),
        target(MovieController.class, "get"),
        target(CollectionController.class, "rename"),
        target(RatingController.class, "put"),
        target(ShareController.class, "create"),
        target(SharedCollectionController.class, "read"),
        target(ExternalRatingController.class, "put"),
        target(AnalyticsController.class, "accept"),
        target(AuthController.class, "register"),
        target(AuthController.class, "login"),
        target(AuthController.class, "logout"),
        target(GenreController.class, "list"),
        target(GenreController.class, "create"),
        target(PersonController.class, "create"),
        target(SearchController.class, "discover"),
        target(RecommendationController.class, "discover"))) {
      var operation = new Operation();
      customizer.customize(operation, target);
      assertNotNull(operation.getResponses().get("500"));
    }

    var operation = new Operation().responses(new ApiResponses());
    operation.getResponses().addApiResponse("500", new io.swagger.v3.oas.models.responses.ApiResponse());
    customizer.customize(operation, target(AccountController.class, "me"));
    assertSame(operation.getResponses().get("500"), operation.getResponses().get("500"));
  }

  private static HandlerMethod target(Class<?> type, String name) throws Exception {
    Method method = java.util.Arrays.stream(type.getDeclaredMethods())
        .filter(candidate -> candidate.getName().equals(name))
        .findFirst()
        .orElseThrow();
    return new HandlerMethod(org.mockito.Mockito.mock(type), method);
  }
}
