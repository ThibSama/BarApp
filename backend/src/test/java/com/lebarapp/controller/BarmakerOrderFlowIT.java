package com.lebarapp.controller;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end barmaker order-processing lifecycle over real HTTP against a real
 * PostgreSQL database: anonymous order creation, authenticated queue listing,
 * detail retrieval, sequential item progression, automatic parent-order status
 * synchronization, completed-queue migration, public-tracking consistency and
 * the protected error responses (401/404/409/400).
 */
class BarmakerOrderFlowIT extends AbstractPostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String DEMO_USERNAME = "barmaker";
    private static final String DEMO_PASSWORD = "barapp-demo-2024";

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {
            };

    @BeforeEach
    void useJdkClientForPatchSupport() {
        // The default request factory (HttpURLConnection) cannot issue PATCH;
        // the JDK HttpClient factory can. TestRestTemplate keeps its non-throwing
        // error handler, so 4xx/5xx responses are still returned as entities.
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void fullPreparationLifecycle() {
        String token = loginAndGetToken();

        // 1. Anonymous client creates an order with two drinks.
        Map<String, Object> created = createOrder("{\"items\":["
                + "{\"cocktailId\":1,\"size\":\"M\"},"
                + "{\"cocktailId\":3,\"size\":\"S\"}]}");
        String orderId = created.get("id").toString();
        List<Map<String, Object>> items = itemsOf(created);
        assertThat(items).hasSize(2);
        String firstItemId = items.get(0).get("id").toString();

        // 2. Order appears in the active queue (ORDERED, 0/2 completed).
        Map<String, Object> queued = findInQueue(token, false, orderId);
        assertThat(queued.get("status")).isEqualTo("ORDERED");
        assertThat(((Number) queued.get("itemCount")).intValue()).isEqualTo(2);
        assertThat(((Number) queued.get("completedItemCount")).intValue()).isZero();

        // 3. Detail is retrievable and items are sorted by sequence number.
        ResponseEntity<Map> detail = getWithToken("/api/bar/orders/" + orderId, token);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(itemsOf(detail.getBody())).hasSize(2);

        // 4. First progression moves the parent order to IN_PROGRESS.
        ResponseEntity<Map> afterFirstStep = patchWithToken(
                "/api/bar/order-items/" + firstItemId + "/next-step", token);
        assertThat(afterFirstStep.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterFirstStep.getBody().get("status")).isEqualTo("IN_PROGRESS");
        assertThat(findInQueue(token, false, orderId).get("status")).isEqualTo("IN_PROGRESS");

        // 5. Complete every item; the parent order becomes COMPLETED.
        for (Map<String, Object> item : items) {
            advanceToCompleted(token, item.get("id").toString());
        }
        ResponseEntity<Map> completedDetail = getWithToken("/api/bar/orders/" + orderId, token);
        assertThat(completedDetail.getBody().get("status")).isEqualTo("COMPLETED");
        assertThat(completedDetail.getBody().get("completedAt")).isNotNull();

        // 6. Gone from completed=false, present in completed=true.
        assertThat(idsInQueue(token, false)).doesNotContain(orderId);
        assertThat(idsInQueue(token, true)).contains(orderId);

        // 7. Public tracking endpoint reports the completed state.
        ResponseEntity<Map> publicTracking = restTemplate.getForEntity(
                url("/api/orders/" + orderId), Map.class);
        assertThat(publicTracking.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicTracking.getBody().get("status")).isEqualTo("COMPLETED");

        // 8. Advancing an already completed item is a 409.
        ResponseEntity<Map> conflict = patchWithToken(
                "/api/bar/order-items/" + firstItemId + "/next-step", token);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody().get("code")).isEqualTo("INVALID_PREPARATION_TRANSITION");
    }

    @Test
    void protectedRoutesRejectMissingToken() {
        ResponseEntity<Map> list = restTemplate.getForEntity(url("/api/bar/orders"), Map.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(list.getBody().get("code")).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    void unknownOrderAndItemReturn404() {
        String token = loginAndGetToken();

        ResponseEntity<Map> unknownOrder = getWithToken(
                "/api/bar/orders/" + java.util.UUID.randomUUID(), token);
        assertThat(unknownOrder.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownOrder.getBody().get("code")).isEqualTo("ORDER_NOT_FOUND");

        ResponseEntity<Map> unknownItem = patchWithToken(
                "/api/bar/order-items/" + java.util.UUID.randomUUID() + "/next-step", token);
        assertThat(unknownItem.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownItem.getBody().get("code")).isEqualTo("ORDER_ITEM_NOT_FOUND");
    }

    @Test
    void malformedItemUuidReturns400() {
        String token = loginAndGetToken();
        ResponseEntity<Map> response = patchWithToken(
                "/api/bar/order-items/not-a-uuid/next-step", token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_IDENTIFIER");
    }

    // ---- helpers --------------------------------------------------------

    private void advanceToCompleted(String token, String itemId) {
        ResponseEntity<Map> response;
        do {
            response = patchWithToken("/api/bar/order-items/" + itemId + "/next-step", token);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        } while (!isItemCompleted(response.getBody(), itemId));
    }

    @SuppressWarnings("unchecked")
    private boolean isItemCompleted(Map<String, Object> order, String itemId) {
        return itemsOf(order).stream()
                .filter(i -> itemId.equals(i.get("id").toString()))
                .anyMatch(i -> "COMPLETED".equals(i.get("preparationStatus")));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itemsOf(Map<String, Object> order) {
        return (List<Map<String, Object>>) order.get("items");
    }

    private Map<String, Object> createOrder(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/orders"), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private Map<String, Object> findInQueue(String token, boolean completed, String orderId) {
        return queue(token, completed).stream()
                .filter(o -> orderId.equals(o.get("id").toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("order not found in queue: " + orderId));
    }

    private List<String> idsInQueue(String token, boolean completed) {
        return queue(token, completed).stream().map(o -> o.get("id").toString()).toList();
    }

    private List<Map<String, Object>> queue(String token, boolean completed) {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url("/api/bar/orders?completed=" + completed), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), LIST_OF_MAPS);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<Map> getWithToken(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);
    }

    private ResponseEntity<Map> patchWithToken(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.PATCH,
                new HttpEntity<>(authHeaders(token)), Map.class);
    }

    private String loginAndGetToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"" + DEMO_USERNAME + "\",\"password\":\"" + DEMO_PASSWORD + "\"}";
        ResponseEntity<LoginResponse> response = restTemplate.exchange(
                url("/api/auth/login"), HttpMethod.POST, new HttpEntity<>(body, headers), LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
