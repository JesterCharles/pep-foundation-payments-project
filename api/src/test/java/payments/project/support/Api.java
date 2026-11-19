package payments.project.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real-HTTP client for the supplied milestone checks (supplied; do not edit). Talks to 127.0.0.1 only.
 * An "as" value is either an actor id sent as the X-Actor-Id stub header, or a {@link Who} carrying a
 * bearer token and optionally a (spoofed) X-Actor-Id.
 */
public final class Api {

    /** Credentials for one request: a bearer token and/or an X-Actor-Id header (either may be null). */
    public record Who(String bearer, String actorHeader) {
        public static Who actor(String actorId) {
            return new Who(null, actorId);
        }

        public static Who token(String token) {
            return new Who(token, null);
        }

        public static Who anonymous() {
            return new Who(null, null);
        }
    }

    /** One HTTP response. */
    public record Response(int status, String body, Map<String, List<String>> headers) {

        public Object parsed() {
            return Json.parse(body);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> json() {
            Object value = parsed();
            if (!(value instanceof Map)) {
                throw new AssertionError("expected a JSON object but got: " + this);
            }
            return (Map<String, Object>) value;
        }

        @SuppressWarnings("unchecked")
        public List<Object> list() {
            Object value = parsed();
            if (!(value instanceof List)) {
                throw new AssertionError("expected a JSON array but got: " + this);
            }
            return (List<Object>) value;
        }

        public String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) {
                    return entry.getValue().get(0);
                }
            }
            return null;
        }

        @Override
        public String toString() {
            String shown = body.length() > 600 ? body.substring(0, 600) + "..." : body;
            return "HTTP " + status + " " + shown;
        }
    }

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String base;

    public Api(int port) {
        this.base = "http://127.0.0.1:" + port;
    }

    public Response get(String path, Object as) {
        return send("GET", path, as, null);
    }

    public Response post(String path, Object as, String jsonBody) {
        return send("POST", path, as, jsonBody);
    }

    public Response send(String method, String path, Object as, String jsonBody) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20));
        Who who = as instanceof Who w ? w : Who.actor((String) as);
        if (who.actorHeader() != null) {
            request.header("X-Actor-Id", who.actorHeader());
        }
        if (who.bearer() != null) {
            request.header("Authorization", "Bearer " + who.bearer());
        }
        if (jsonBody != null) {
            request.header("Content-Type", "application/json");
            request.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        try {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException e) {
            throw new AssertionError(method + " " + path + " failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted", e);
        }
    }

    // ---------------------------------------------------------------- JSON bodies

    public static String submitBody(String supplierId, String invoiceNumber, String amount, String currency) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("supplierId", supplierId);
        body.put("invoiceNumber", invoiceNumber);
        body.put("amount", amount);
        body.put("currency", currency);
        return toJson(body);
    }

    public static String reasonBody(String reason) {
        return toJson(Map.of("reason", reason));
    }

    /** Minimal JSON writer for flat maps of strings, numbers, booleans and nulls. */
    public static String toJson(Map<String, ?> values) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append(quote(entry.getKey())).append(':');
            Object value = entry.getValue();
            if (value == null || value instanceof Number || value instanceof Boolean) {
                out.append(value);
            } else {
                out.append(quote(value.toString()));
            }
        }
        return out.append('}').toString();
    }

    public static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
