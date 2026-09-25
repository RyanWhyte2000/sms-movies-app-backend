package com.sms.movies.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

  @Bean
  OpenApiCustomizer errorResponseComponents() {
    return openApi -> {
      Components components = openApi.getComponents();
      if (components == null) {
        components = new Components();
        openApi.setComponents(components);
      }

      components.addSchemas(
          "ApiError",
          new ObjectSchema()
              .addProperty("type", new StringSchema().example("about:blank"))
              .addProperty("title", new StringSchema().example("Not Found"))
              .addProperty("status", new IntegerSchema().example(404))
              .addProperty("detail", new StringSchema().example("Resource was not found."))
              .addProperty("instance", new StringSchema().example("/api/movies/42"))
              .addProperty("correlationId", new StringSchema().example("8b75f170-80b0-4db1-a37f-8f786bfa42bb"))
              .addProperty(
                  "errors",
                  new MapSchema()
                      .description("Field-specific validation messages; present for validation failures.")));

      addResponse(components, "BadRequest", "The request is malformed or fails validation.");
      addResponse(components, "Unauthorized", "Authentication is required or credentials are invalid.");
      addResponse(components, "Forbidden", "The caller is not allowed to perform this operation.");
      addResponse(components, "NotFound", "The requested resource does not exist or is not visible to the caller.");
      addResponse(components, "Conflict", "The request conflicts with an existing record or resource state.");
      addResponse(components, "InternalServerError", "An unexpected server error occurred. Use the correlation ID when reporting it.");
    };
  }

  @Bean
  OperationCustomizer documentedErrorResponses() {
    return (operation, handlerMethod) -> {
      var responses = operation.getResponses();
      if (responses == null) {
        responses = new ApiResponses();
        operation.setResponses(responses);
      }
      String controller = handlerMethod.getBeanType().getSimpleName();
      String method = handlerMethod.getMethod().getName();

      if (mayBeBadRequest(controller, method)) {
        responses.putIfAbsent("400", reference("BadRequest"));
      }
      if (requiresAuthentication(controller, method)) {
        responses.putIfAbsent("401", reference("Unauthorized"));
      }
      if (mayBeForbidden(controller, method)) {
        responses.putIfAbsent("403", reference("Forbidden"));
      }
      if (mayBeMissing(controller, method)) {
        responses.putIfAbsent("404", reference("NotFound"));
      }
      if (mayConflict(controller, method)) {
        responses.putIfAbsent("409", reference("Conflict"));
      }
      responses.putIfAbsent("500", reference("InternalServerError"));
      return operation;
    };
  }

  private static boolean requiresAuthentication(String controller, String method) {
    if (controller.equals("CsrfController") || controller.equals("SharedCollectionController")) {
      return false;
    }
    return !controller.equals("AuthController") || method.equals("login");
  }

  private static boolean mayBeBadRequest(String controller, String method) {
    return !(controller.equals("AccountController")
        || controller.equals("CsrfController")
        || (controller.equals("GenreController") && method.equals("list"))
        || (controller.equals("AuthController") && method.equals("logout")));
  }

  private static boolean mayBeForbidden(String controller, String method) {
    if (controller.equals("MovieController")) {
      return method.equals("create") || method.equals("update") || method.equals("delete");
    }
    if (controller.equals("GenreController") || controller.equals("PersonController")) {
      return method.equals("create");
    }
    if (controller.equals("ExternalRatingController")) return true;
    return switch (controller) {
      case "AuthController" -> true;
      case "CollectionController" ->
          java.util.Set.of("create", "rename", "delete", "add", "remove").contains(method);
      case "RatingController" -> java.util.Set.of("put", "delete").contains(method);
      case "ShareController" -> java.util.Set.of("create", "revoke").contains(method);
      case "RecommendationController", "SearchController", "AnalyticsController" -> true;
      default -> false;
    };
  }

  private static boolean mayBeMissing(String controller, String method) {
    return switch (controller) {
      case "MovieController" -> !method.equals("list") && !method.equals("create");
      case "CollectionController" -> !method.equals("list") && !method.equals("create");
      case "RatingController" -> method.equals("put");
      case "ShareController", "SharedCollectionController", "ExternalRatingController" -> true;
      case "AnalyticsController" -> true;
      default -> false;
    };
  }

  private static boolean mayConflict(String controller, String method) {
    return (controller.equals("AuthController") && method.equals("register"))
        || controller.equals("AnalyticsController")
        || (controller.equals("GenreController") && method.equals("create"))
        || (controller.equals("MovieController") && method.equals("create"));
  }

  private static void addResponse(Components components, String name, String description) {
    components.addResponses(
        name,
        new ApiResponse()
            .description(description)
            .content(
                new Content()
                    .addMediaType(
                        PROBLEM_MEDIA_TYPE,
                        new io.swagger.v3.oas.models.media.MediaType()
                            .schema(new ObjectSchema().$ref("#/components/schemas/ApiError")))));
  }

  private static ApiResponse reference(String name) {
    return new ApiResponse().$ref("#/components/responses/" + name);
  }
}
