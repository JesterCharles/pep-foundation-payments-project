package payments.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import payments.desk.ApprovalRefusedException;
import payments.desk.NotFoundException;
import payments.desk.PaymentDesk;
import payments.domain.PaymentRequest;
import payments.identity.ActorDirectory;
import payments.intake.PaymentRules;
import payments.reports.Reconciler;
import payments.reports.ReconciliationResult;
import payments.reports.StatementLine;

/**
 * JSON HTTP API for the payment desk (Javalin 7).
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source). This is the Javalin route layer the
 * Spring Boot port replaces: handlers parse, delegate to the desk and map results; business rules
 * stay in the desk and domain.
 *
 * <p>IDENTITY IS A STUB, NOT AUTHENTICATION: the X-Actor-Id header is trusted as sent and only
 * looked up in the supplied ActorDirectory. Anyone can send any id. Real authentication is added
 * in the Spring Boot API (see docs/contracts/).
 *
 * <p>Error responses are JSON objects {@code {"error":"..."}} with no stack traces.
 */
public final class PaymentApi {

    /** The stub identity header. */
    public static final String ACTOR_HEADER = "X-Actor-Id";

    /** INFO for submissions and actions (request id, actor, action), WARN for refusals; never payload. */
    private static final Logger log = LoggerFactory.getLogger(PaymentApi.class);

    private PaymentApi() {
    }

    /** body of POST /payment-requests, e.g. {"supplierId":"SUP-0042","invoiceNumber":"INV-7781","amount":"1234.50","currency":"USD"}. */
    public record SubmitBody(String supplierId, String invoiceNumber, String amount, String currency) {
    }

    /** body of POST /payment-requests/{id}/reject and /cancel, e.g. {"reason":"duplicate of PR-12"}. */
    public record ReasonBody(String reason) {
    }

    /**
     * builds the app without starting it.
     *
     * @param statementsDir the folder of supplied statement fixtures read by the baseline report route
     */
    public static Javalin create(PaymentDesk desk, ActorDirectory directory, Path statementsDir) {
        return Javalin.create(config -> {
            registerErrorMapping(config);

            config.routes.post("/payment-requests", ctx -> {
                String actor = actorFrom(ctx, directory);
                SubmitBody body = bodyAs(ctx, SubmitBody.class);
                PaymentRequest created = desk.submit(body.supplierId(), body.invoiceNumber(), body.amount(),
                        body.currency(), actor);
                log.info("request {} submitted by {}", created.getId(), actor);
                ctx.status(201);
                ctx.json(PaymentView.of(created));
            });

            config.routes.get("/payment-requests", ctx -> {
                String status = ctx.queryParam("status");
                List<PaymentView> views = new ArrayList<>();
                for (PaymentRequest request : desk.all()) {
                    if (status == null || status.isBlank() || request.getStatus().name().equalsIgnoreCase(status.trim())) {
                        views.add(PaymentView.of(request));
                    }
                }
                ctx.json(views);
            });

            config.routes.get("/payment-requests/{id}", ctx -> {
                int id = idFrom(ctx);
                PaymentRequest found = desk.find(id);
                if (found == null) {
                    ctx.status(404);
                    ctx.json(Map.of("error", "payment request " + id + " not found"));
                    return;
                }
                ctx.json(PaymentView.of(found));
            });

            registerActionRoutes(config, desk, directory);
            registerReportRoutes(config, desk, statementsDir);
        });
    }

    /**
     * builds and starts the app on the loopback address 127.0.0.1 only. Port 0 picks a free port (see app.port()).
     * Binding the loopback address, not every interface, keeps the service private to this machine and stops
     * another local server on the same port from answering requests meant for this one.
     */
    public static Javalin start(PaymentDesk desk, ActorDirectory directory, Path statementsDir, int port) {
        return create(desk, directory, statementsDir).start("127.0.0.1", port);
    }

    /**
     * Maps exceptions to status codes, centrally. Every error body is {"error": message}.
     */
    static void registerErrorMapping(JavalinConfig config) {
        config.routes.exception(NotFoundException.class, (e, ctx) -> error(ctx, 404, e.getMessage()));
        config.routes.exception(IllegalArgumentException.class, (e, ctx) -> error(ctx, 400, e.getMessage()));
        config.routes.exception(ApprovalRefusedException.class, (e, ctx) -> {
            log.warn("refused: {}", e.getMessage());
            error(ctx, 403, e.getMessage());
        });
        config.routes.exception(IllegalStateException.class, (e, ctx) -> {
            log.warn("conflict: {}", e.getMessage());
            error(ctx, 409, e.getMessage());
        });
        config.routes.exception(Exception.class, (e, ctx) -> {
            log.error("unexpected failure on {} {}", ctx.method(), ctx.path(), e);
            error(ctx, 500, "internal error");
        });
    }

    /** POST /payment-requests/{id}/approve, /reject and /cancel. */
    static void registerActionRoutes(JavalinConfig config, PaymentDesk desk, ActorDirectory directory) {
        config.routes.post("/payment-requests/{id}/approve", ctx -> {
            String actor = actorFrom(ctx, directory);
            int id = idFrom(ctx);
            PaymentRequest updated = desk.approve(id, actor);
            log.info("request {} approved by {}", id, actor);
            ctx.json(PaymentView.of(updated));
        });
        config.routes.post("/payment-requests/{id}/reject", ctx -> {
            String actor = actorFrom(ctx, directory);
            int id = idFrom(ctx);
            ReasonBody body = bodyAs(ctx, ReasonBody.class);
            PaymentRequest updated = desk.reject(id, actor, body.reason());
            log.info("request {} rejected by {}", id, actor);
            ctx.json(PaymentView.of(updated));
        });
        config.routes.post("/payment-requests/{id}/cancel", ctx -> {
            String actor = actorFrom(ctx, directory);
            int id = idFrom(ctx);
            ReasonBody body = bodyAs(ctx, ReasonBody.class);
            PaymentRequest updated = desk.cancel(id, actor, body.reason());
            log.info("request {} cancelled by {}", id, actor);
            ctx.json(PaymentView.of(updated));
        });
    }

    /** GET /reports/reconciliation?statement=NAME (NAME: a fixture file directly inside statementsDir). */
    static void registerReportRoutes(JavalinConfig config, PaymentDesk desk, Path statementsDir) {
        config.routes.get("/reports/reconciliation", ctx -> {
            String name = ctx.queryParam("statement");
            if (name == null || name.isBlank() || name.contains("..") || name.contains("/") || name.contains("\\")) {
                throw new IllegalArgumentException("statement must name a supplied statement file");
            }
            Path file = statementsDir.resolve(name).normalize();
            if (!file.getParent().equals(statementsDir.normalize()) || !Files.isRegularFile(file)) {
                throw new IllegalArgumentException("unknown statement " + name);
            }
            ReconciliationResult result = Reconciler.reconcile(desk.all(), StatementLine.readCsv(file));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("statement", name);
            body.put("matched", lines(result.matched()));
            body.put("amountMismatch", lines(result.amountMismatch()));
            body.put("currencyMismatch", lines(result.currencyMismatch()));
            body.put("unknownReference", lines(result.unknownReference()));
            body.put("duplicateLine", lines(result.duplicateLine()));
            List<Integer> ids = new ArrayList<>();
            long total = 0;
            for (PaymentRequest request : result.paidWithoutLine()) {
                ids.add(request.getId());
                total += request.getAmountCents();
            }
            body.put("paidWithoutLine", summary(ids, total));
            ctx.json(body);
        });
    }

    private static Map<String, Object> lines(List<StatementLine> lines) {
        List<Integer> ids = new ArrayList<>();
        long total = 0;
        for (StatementLine line : lines) {
            ids.add(line.requestId());
            total += line.cents();
        }
        return summary(ids, total);
    }

    private static Map<String, Object> summary(List<Integer> ids, long totalCents) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("count", ids.size());
        summary.put("requestIds", ids);
        summary.put("total", PaymentRules.formatCents(totalCents));
        return summary;
    }

    private static void error(Context ctx, int status, String message) {
        ctx.status(status);
        ctx.json(Map.of("error", message == null || message.isBlank() ? "request failed" : message));
    }

    /**
     * resolves the X-Actor-Id stub header. Not authentication.
     *
     * @throws IllegalArgumentException when the header is missing, blank or not in the directory
     */
    static String actorFrom(Context ctx, ActorDirectory directory) {
        String actor = ctx.header(ACTOR_HEADER);
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("missing " + ACTOR_HEADER + " header");
        }
        actor = actor.trim();
        if (!directory.isKnown(actor)) {
            throw new IllegalArgumentException("unknown actor in " + ACTOR_HEADER + " header");
        }
        return actor;
    }

    /**
     * reads a JSON body. Any malformed or missing body becomes an IllegalArgumentException
     * whose message never repeats the body.
     */
    static <T> T bodyAs(Context ctx, Class<T> type) {
        T value;
        try {
            value = ctx.bodyAsClass(type);
        } catch (Exception malformed) {
            // Exception, not RuntimeException: the JSON mapper can throw a checked parse exception.
            throw new IllegalArgumentException("malformed JSON body");
        }
        if (value == null) {
            throw new IllegalArgumentException("missing JSON body");
        }
        return value;
    }

    /** the {id} path parameter as an int; a non-number becomes an IllegalArgumentException. */
    static int idFrom(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("request id must be a whole number");
        }
    }
}
