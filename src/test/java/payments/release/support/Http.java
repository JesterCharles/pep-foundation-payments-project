package payments.release.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Real-HTTP helper for the released API tests (supplied; do not edit). Talks to localhost only.
 */
public final class Http {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final int port;

    public Http(int port) {
        this.port = port;
    }

    /** One HTTP answer. */
    public record Response(int status, String body) {

        /** The body as a JSON object; fails the test with the raw body when it is not one. */
        public Map<String, Object> json() {
            try {
                return MiniJson.object(body);
            } catch (IllegalArgumentException notJson) {
                throw new AssertionError("expected a JSON object body (status " + status + ") but got: " + body);
            }
        }

        @Override
        public String toString() {
            return status + " " + body;
        }
    }

    public Response get(String path, String actor) {
        return send("GET", path, actor, null);
    }

    public Response post(String path, String actor, String jsonBody) {
        return send("POST", path, actor, jsonBody);
    }

    public Response send(String method, String path, String actor, String jsonBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10));
        if (actor != null) {
            builder.header("X-Actor-Id", actor);
        }
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        try {
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new AssertionError(method + " " + path + " failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted", e);
        }
    }

    /** JSON for a submit body. */
    public static String submitBody(String supplierId, String invoiceNumber, String amount, String currency) {
        return "{\"supplierId\":" + quote(supplierId) + ",\"invoiceNumber\":" + quote(invoiceNumber)
                + ",\"amount\":" + quote(amount) + ",\"currency\":" + quote(currency) + "}";
    }

    /** JSON for a reason body. */
    public static String reasonBody(String reason) {
        return "{\"reason\":" + quote(reason) + "}";
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
