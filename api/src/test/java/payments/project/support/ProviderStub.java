package payments.project.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * In-test stub of the payment provider (supplied; do not edit). See docs/contracts/W09-provider-stub.md
 * (Standard) or docs/contracts/W08-provider-stub.md (Fast). Synthetic only: no real provider, no funds.
 *
 * <p>{@code POST /payments} with an {@code Idempotency-Key} header creates at most one payment per key.
 * A repeated key returns the original response with {@code Idempotent-Replayed: true}. Each call takes the
 * next scripted {@link Behaviour} (default ACCEPT). SLOW still creates the payment, but answers only after
 * {@link #slowMillis}, longer than the client timeout the tests configure: exactly the case where a retry
 * without the same key would pay twice. {@code GET /health} answers 200 while {@link #healthy}.
 */
public final class ProviderStub implements AutoCloseable {

    /**
     * ACCEPT and REJECT answer at once. SLOW creates the payment, then answers late. FAIL_500 answers 500
     * without a payment. HANG answers 503 late without a payment.
     */
    public enum Behaviour { ACCEPT, REJECT, SLOW, FAIL_500, HANG }

    /** One received POST /payments call. */
    public record Call(String idempotencyKey, String body) {
    }

    private final HttpServer server;
    private final Deque<Behaviour> script = new ArrayDeque<>();
    private final Map<String, String> paymentsByKey = Collections.synchronizedMap(new LinkedHashMap<>());
    private final List<Call> calls = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean healthy = true;
    private volatile long slowMillis = 1500;
    private int nextPayment = 1;

    public ProviderStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/payments", this::payments);
        server.createContext("/health", exchange -> respond(exchange, healthy ? 200 : 503,
                healthy ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\"}", null));
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Scripts the behaviour of the next calls, in order; later calls ACCEPT. */
    public synchronized ProviderStub then(Behaviour... behaviours) {
        Collections.addAll(script, behaviours);
        return this;
    }

    public synchronized void reset() {
        script.clear();
        paymentsByKey.clear();
        calls.clear();
        healthy = true;
    }

    public void healthy(boolean value) {
        healthy = value;
    }

    public void slowMillis(long value) {
        slowMillis = value;
    }

    /** Payments created (one per distinct idempotency key). */
    public int paymentsCreated() {
        return paymentsByKey.size();
    }

    public List<Call> calls() {
        synchronized (calls) {
            return List.copyOf(calls);
        }
    }

    private void payments(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"method not allowed\"}", null);
            return;
        }
        String key = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        calls.add(new Call(key, body));
        if (key == null || key.isBlank()) {
            respond(exchange, 400, "{\"error\":\"Idempotency-Key header is required\"}", null);
            return;
        }
        String existing = paymentsByKey.get(key);
        if (existing != null) {
            respond(exchange, 200, existing, "true");
            return;
        }
        Behaviour behaviour;
        String response;
        synchronized (this) {
            behaviour = script.isEmpty() ? Behaviour.ACCEPT : script.pollFirst();
            if (behaviour == Behaviour.FAIL_500 || behaviour == Behaviour.HANG) {
                response = null;
            } else {
                String id = String.format("PP-%06d", nextPayment++);
                response = behaviour == Behaviour.REJECT
                        ? "{\"providerPaymentId\":\"" + id + "\",\"status\":\"REJECTED\",\"reason\":\"supplier account closed (synthetic)\"}"
                        : "{\"providerPaymentId\":\"" + id + "\",\"status\":\"ACCEPTED\",\"reason\":null}";
                paymentsByKey.put(key, response);
            }
        }
        if (behaviour == Behaviour.FAIL_500) {
            respond(exchange, 500, "{\"error\":\"provider failure (synthetic)\"}", null);
            return;
        }
        if (behaviour == Behaviour.SLOW || behaviour == Behaviour.HANG) {
            try {
                Thread.sleep(slowMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (behaviour == Behaviour.HANG) {
            respond(exchange, 503, "{\"error\":\"provider unavailable (synthetic)\"}", null);
            return;
        }
        respond(exchange, 200, response, null);
    }

    /** The provider payment id created for a key, or null. */
    public String paymentIdFor(String key) {
        String response = paymentsByKey.get(key);
        if (response == null) {
            return null;
        }
        return (String) Json.object(response).get("providerPaymentId");
    }

    private static void respond(HttpExchange exchange, int status, String body, String replayed) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (replayed != null) {
            exchange.getResponseHeaders().set("Idempotent-Replayed", replayed);
        }
        try {
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } catch (IOException clientGone) {
            // the client timed out and closed the connection; the payment still exists
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
