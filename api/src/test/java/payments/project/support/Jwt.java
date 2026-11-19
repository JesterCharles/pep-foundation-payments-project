package payments.project.support;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Mints HS256 JSON Web Tokens for the supplied security checks (supplied; do not edit).
 *
 * <p>The signing secret is generated when the tests start and passed to your API as the
 * {@code security.jwt.secret} property; no key is ever committed. Claims: {@code sub} (user id),
 * {@code roles} (array of REQUESTER, APPROVER, PAYABLES_CLERK), {@code iat} and {@code exp}.
 */
public final class Jwt {

    /** A fresh random secret (48 URL-safe characters, 288 bits) for one test run. */
    public static final String SECRET = randomSecret();

    private Jwt() {
    }

    public static String token(String subject, List<String> roles) {
        return token(subject, roles, Instant.now().plusSeconds(900), SECRET);
    }

    public static String expired(String subject, List<String> roles) {
        return token(subject, roles, Instant.now().minusSeconds(600), SECRET);
    }

    public static String signedWith(String secret, String subject, List<String> roles) {
        return token(subject, roles, Instant.now().plusSeconds(900), secret);
    }

    /** The same token with its payload changed to name another subject, keeping the original signature. */
    public static String tampered(String token, String otherSubject, List<String> roles) {
        String[] parts = token.split("\\.");
        String forged = base64(payload(otherSubject, roles, Instant.now().plusSeconds(900)));
        return parts[0] + "." + forged + "." + parts[2];
    }

    public static String token(String subject, List<String> roles, Instant expiresAt, String secret) {
        String header = base64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String body = base64(payload(subject, roles, expiresAt));
        String signingInput = header + "." + body;
        return signingInput + "." + sign(signingInput, secret);
    }

    private static String payload(String subject, List<String> roles, Instant expiresAt) {
        String roleList = roles.stream().map(Api::quote).collect(Collectors.joining(","));
        long issued = Math.min(Instant.now().getEpochSecond(), expiresAt.getEpochSecond() - 60);
        return "{\"sub\":" + Api.quote(subject) + ",\"roles\":[" + roleList + "],\"iat\":" + issued
                + ",\"exp\":" + expiresAt.getEpochSecond() + "}";
    }

    private static String sign(String input, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String randomSecret() {
        byte[] bytes = new byte[36];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
