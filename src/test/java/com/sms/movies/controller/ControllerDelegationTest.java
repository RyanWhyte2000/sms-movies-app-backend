package com.sms.movies.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sms.movies.dto.*;
import com.sms.movies.dto.AccountDtos.*;
import com.sms.movies.dto.CollectionDtos.*;
import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.dto.RatingDtos.*;
import com.sms.movies.exception.ApiException;
import com.sms.movies.security.CurrentUser;
import com.sms.movies.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CsrfToken;

class ControllerDelegationTest {
  private static final UUID USER = UUID.randomUUID();
  private static final UUID ID = UUID.randomUUID();
  private static final PageResponse EMPTY = new PageResponse<>(List.of(), 0, 20, 0, 0);

  private static CurrentUser current() {
    var current = mock(CurrentUser.class);
    when(current.id()).thenReturn(USER);
    return current;
  }

  @Test
  void simpleControllersDelegateAndPreserveHttpSemantics() {
    var current = current();
    var user = new UserResponse(USER, "user@example.com", "User", Instant.now());
    when(current.get()).thenReturn(user);
    assertEquals(user, new AccountController(current).me());

    var analytics = mock(AnalyticsService.class);
    var event = new com.sms.movies.dto.AnalyticsDtos.EventRequest(
        UUID.randomUUID(), com.sms.movies.dto.AnalyticsDtos.ClientEvent.SEARCH_RESULT_SELECTED,
        ID, null, true);
    assertEquals(202, new AnalyticsController(analytics, current).accept(event).getStatusCode().value());
    verify(analytics).accept(USER, event);

    var csrf = mock(CsrfToken.class);
    when(csrf.getHeaderName()).thenReturn("X-CSRF");
    when(csrf.getToken()).thenReturn("token");
    assertEquals("token", new CsrfController().token(csrf).token());
  }

  @Test
  void collectionControllerCoversAllEndpoints() {
    var service = mock(CollectionService.class);
    var current = current();
    var controller = new CollectionController(service, current);
    var response = new CollectionResponse(ID, "List", Instant.now(), Instant.now());
    when(service.list(USER, 0, 20)).thenReturn(EMPTY);
    when(service.create(USER, "List")).thenReturn(response);
    when(service.get(USER, ID, 0, 20)).thenReturn(new CollectionDetails(response, EMPTY));
    when(service.rename(USER, ID, "New")).thenReturn(response);

    assertSame(EMPTY, controller.list(0, 20));
    assertEquals(201, controller.create(new CollectionDtos.NameRequest("List")).getStatusCode().value());
    assertNotNull(controller.get(ID, 0, 20));
    assertEquals(response, controller.rename(ID, new CollectionDtos.NameRequest("New")));
    assertEquals(204, controller.delete(ID).getStatusCode().value());
    assertEquals(204, controller.add(ID, UUID.randomUUID()).getStatusCode().value());
    assertEquals(204, controller.remove(ID, UUID.randomUUID()).getStatusCode().value());
  }

  @Test
  void catalogueControllersCoverRoutes() {
    var catalog = mock(CatalogService.class);
    var movies = mock(MovieService.class);
    var current = current();
    when(current.idOrNull()).thenReturn(USER);
    when(catalog.search(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(EMPTY);
    var details = new MovieDetails(
        ID, "Film", "D", "http://poster", "Director", "Drama", LocalDate.now(), 90,
        List.of(), List.of(), List.of(), null);
    when(catalog.details(ID, USER)).thenReturn(details);
    var request = new MovieRequest("Film", "D", "http://poster", "Director", "Drama",
        LocalDate.now(), 90, List.of(), List.of(), List.of());
    var movie = new MovieResponse(ID, "Film", "D", "http://poster", "Director", "Drama", LocalDate.now(), 90);
    when(movies.create(request)).thenReturn(movie);
    when(movies.update(ID, request)).thenReturn(movie);
    var controller = new MovieController(movies, catalog, current);

    assertSame(EMPTY, controller.list("q", null, null, null, ID, ID, 0, 20));
    assertSame(details, controller.get(ID));
    assertEquals(201, controller.create(request).getStatusCode().value());
    assertEquals(movie, controller.update(ID, request));
    assertEquals(204, controller.delete(ID).getStatusCode().value());

    var item = new NamedItem(ID, "Drama");
    when(catalog.genres()).thenReturn(List.of(item));
    when(catalog.createGenre("Drama")).thenReturn(item);
    var genres = new GenreController(catalog);
    assertEquals(List.of(item), genres.list());
    assertEquals(item, genres.create(new GenreController.NameRequest("Drama")));

    when(catalog.people("a", "CAST", 0, 20)).thenReturn(EMPTY);
    when(catalog.createPerson("Actor")).thenReturn(item);
    var people = new PersonController(catalog);
    assertSame(EMPTY, people.list("a", "CAST", 0, 20));
    assertEquals(item, people.create(new PersonController.NameRequest("Actor")));

    var rating = new ExternalRating("IMDb", 8, 10, 1L, Instant.now());
    var ratingRequest = new ExternalRatingRequest(8.0, 10.0, 1L);
    when(catalog.putExternal(USER, ID, "IMDb", ratingRequest)).thenReturn(rating);
    assertEquals(rating, new ExternalRatingController(catalog, current).put(ID, "IMDb", ratingRequest));
  }

  @Test
  void ratingDiscoveryAuditAndShareControllersCoverRoutes() {
    var current = current();
    var ratings = mock(RatingService.class);
    var rating = new RatingResponse(ID, 4, Instant.now(), Instant.now());
    when(ratings.list(USER, 0, 20)).thenReturn(EMPTY);
    when(ratings.put(USER, ID, 4)).thenReturn(rating);
    var ratingController = new RatingController(ratings, current);
    assertSame(EMPTY, ratingController.list(0, 20));
    assertEquals(rating, ratingController.put(ID, new RatingRequest(4)));
    assertEquals(204, ratingController.delete(ID).getStatusCode().value());

    var discovery = mock(DiscoveryService.class);
    var discovered = new DiscoveryResponse("engine", false, "ok", List.of());
    when(discovery.search(USER, "space", 10)).thenReturn(discovered);
    when(discovery.recommendations(USER, "space", 5)).thenReturn(discovered);
    assertSame(discovered, new SearchController(discovery, current).discover(new SearchRequest("space", null)));
    assertSame(discovered, new RecommendationController(discovery, current)
        .discover(new RecommendationRequest("space", 5)));

    var audit = mock(AuditService.class);
    when(audit.list(USER, null, null, null, 0, 20)).thenReturn(EMPTY);
    var auditController = new AuditController(audit, current);
    assertSame(EMPTY, auditController.list(null, null, null, 0, 20));
    assertThrows(ApiException.class,
        () -> auditController.list(null, Instant.now(), Instant.now().minusSeconds(1), 0, 20));

    var shares = mock(ShareService.class);
    var created = new CreatedShare(ID, "token", "/shared/token", null);
    when(shares.create(USER, ID, null)).thenReturn(created);
    when(shares.list(USER, ID)).thenReturn(List.of());
    var shareController = new ShareController(shares, current);
    assertEquals(201, shareController.create(ID, null).getStatusCode().value());
    assertTrue(shareController.list(ID).isEmpty());
    assertEquals(204, shareController.revoke(ID, UUID.randomUUID()).getStatusCode().value());

    var shared = new SharedCollection("List", EMPTY);
    when(shares.read("token", 0, 20)).thenReturn(shared);
    var response = new SharedCollectionController(shares).read("token", 0, 20);
    assertEquals(shared, response.getBody());
    assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
  }
}
