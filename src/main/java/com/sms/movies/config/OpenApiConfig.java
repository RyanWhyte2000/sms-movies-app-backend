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
      responses.putIfAbsent("400", reference("BadRequest"));
      responses.putIfAbsent("401", reference("Unauthorized"));
      responses.putIfAbsent("403", reference("Forbidden"));
      responses.putIfAbsent("404", reference("NotFound"));
      responses.putIfAbsent("409", reference("Conflict"));
      responses.putIfAbsent("500", reference("InternalServerError"));
      return operation;
    };
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
