package payments.api;

import java.nio.file.Path;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

import payments.desk.PaymentDesk;
import payments.domain.PaymentRequest;
import payments.identity.ActorDirectory;

/**
 * JSON HTTP API for the payment desk (Javalin 7).
 *
 * <p>Supplied in release 4 as a skeleton; LEARNER-OWNED on arrival. The supplied parts
 * (marked SUPPLIED) start the server, submit and read requests, and resolve the X-Actor-Id
 * header. You add the parts marked TODO(M4) and TODO(M6). Handlers parse, delegate to the
 * desk and map results; business rules stay in the desk and domain.
 *
 * <p>IDENTITY IS A STUB, NOT AUTHENTICATION: the X-Actor-Id header is trusted as sent and only
 * looked up in the supplied ActorDirectory. Anyone can send any id. Real authentication
 * arrives in the Standard/Fast continuation.
 *
 * <p>Error responses are JSON objects {@code {"error":"..."}} with no stack traces.
 */
public final class PaymentApi {

    /** SUPPLIED: the stub identity header. */
    public static final String ACTOR_HEADER = "X-Actor-Id";

    // TODO(M4): add an SLF4J logger:
    //   private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentApi.class);
    //   Log INFO for submissions and approvals (request id, actor, action) and WARN for refusals and
    //   conflicts. Never log request bodies, invoice text or other payload.

    private PaymentApi() {
    }

    /** SUPPLIED: body of POST /payment-requests, e.g. {"supplierId":"SUP-0042","invoiceNumber":"INV-7781","amount":"1234.50","currency":"USD"}. */
    public record SubmitBody(String supplierId, String invoiceNumber, String amount, String currency) {
    }

    /** SUPPLIED: body of POST /payment-requests/{id}/reject and /cancel, e.g. {"reason":"duplicate of PR-12"}. */
    public record ReasonBody(String reason) {
    }

    /**
     * SUPPLIED: builds the app without starting it.
     *
     * @param statementsDir the folder of supplied statement fixtures read by the M6 report route
     */
    public static Javalin create(PaymentDesk desk, ActorDirectory directory, Path statementsDir) {
        return Javalin.create(config -> {
            registerErrorMapping(config);

            config.routes.post("/payment-requests", ctx -> {
                String actor = actorFrom(ctx, directory);
                SubmitBody body = bodyAs(ctx, SubmitBody.class);
                PaymentRequest created = desk.submit(body.supplierId(), body.invoiceNumber(), body.amount(),
                        body.currency(), actor);
                ctx.status(201);
                ctx.json(PaymentView.of(created));
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

    /** SUPPLIED: builds and starts the app. Port 0 picks a free localhost port (see app.port()). */
    public static Javalin start(PaymentDesk desk, ActorDirectory directory, Path statementsDir, int port) {
        return create(desk, directory, statementsDir).start(port);
    }

    /**
     * Maps exceptions to status codes, centrally. Every error body is {"error": message}.
     */
    static void registerErrorMapping(JavalinConfig config) {
        // TODO(M4): register exception handlers with config.routes.exception(...):
        //   NotFoundException        -> 404
        //   IllegalArgumentException -> 400 (bad JSON, bad amount text, bad id, missing/unknown actor)
        //   ApprovalRefusedException -> 403
        //   IllegalStateException    -> 409 (PaymentConflictException: duplicate, invalid move, mismatch)
        //   any other Exception      -> 500 with a generic message
        //   Javalin picks the most specific handler for the thrown class. Never put a stack trace or
        //   the request body into a response. Log WARN for 403 and 409 (request id, actor, rule only).
    }

    /** POST /payment-requests/{id}/approve, /reject and /cancel. */
    static void registerActionRoutes(JavalinConfig config, PaymentDesk desk, ActorDirectory directory) {
        // TODO(M4): replace each placeholder handler. The actor comes from actorFrom(ctx, directory);
        //   reject and cancel read ReasonBody via bodyAs(ctx, ReasonBody.class). Each success answers
        //   200 with PaymentView.of(updated request).
        config.routes.post("/payment-requests/{id}/approve", ctx -> {
            throw new UnsupportedOperationException("TODO M4: POST /payment-requests/{id}/approve");
        });
        config.routes.post("/payment-requests/{id}/reject", ctx -> {
            throw new UnsupportedOperationException("TODO M4: POST /payment-requests/{id}/reject");
        });
        config.routes.post("/payment-requests/{id}/cancel", ctx -> {
            throw new UnsupportedOperationException("TODO M4: POST /payment-requests/{id}/cancel");
        });
    }

    /** GET /reports/reconciliation?statement=NAME. */
    static void registerReportRoutes(JavalinConfig config, PaymentDesk desk, Path statementsDir) {
        // TODO(M6): implement the reconciliation route.
        //   - NAME must be the file name of a supplied fixture directly inside statementsDir
        //     (for example "mixed-discrepancies.csv"). A missing name, any other name, or anything
        //     that could leave the folder ("..", "/", "\") answers 400. Never open arbitrary paths.
        //   - Read the lines with StatementLine.readCsv, reconcile them against desk.all() with
        //     Reconciler.reconcile, and answer 200 with:
        //       {"statement": NAME,
        //        "matched":          {"count": n, "requestIds": [...], "total": "0.00"},
        //        "amountMismatch":   {...}, "currencyMismatch": {...}, "unknownReference": {...},
        //        "duplicateLine":    {...}, "paidWithoutLine":  {...}}
        //     requestIds lists each entry's request id in the result's order; total is the sum of the
        //     statement-line cents (for paidWithoutLine: the requests' amounts) via PaymentRules.formatCents.
        config.routes.get("/reports/reconciliation", ctx -> {
            throw new UnsupportedOperationException("TODO M6: GET /reports/reconciliation");
        });
    }

    /**
     * SUPPLIED: resolves the X-Actor-Id stub header. Not authentication.
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
     * SUPPLIED: reads a JSON body. Any malformed or missing body becomes an IllegalArgumentException
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

    /** SUPPLIED: the {id} path parameter as an int; a non-number becomes an IllegalArgumentException. */
    static int idFrom(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("request id must be a whole number");
        }
    }
}
