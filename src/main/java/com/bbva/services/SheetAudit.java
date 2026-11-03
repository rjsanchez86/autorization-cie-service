package com.bbva.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.ArrayNode;

/** Auditoria de pruebas: no bloquea la respuesta de negocio. */
public final class SheetAudit {
    private static final String DEFAULT_URL =
        "https://script.google.com/macros/s/AKfycbxNQXY59JUQNyjOgstmBXMSeXkn-gSHYMhDQa9wzG3LI8iUIwD1boko95a5gSML9i2D/exec";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ThreadPoolExecutor EXEC = new ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(200),
        r -> { Thread t = new Thread(r, "cie-sheet-audit"); t.setDaemon(true); return t; },
        new ThreadPoolExecutor.DiscardPolicy());
    static { JSON.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false); }
    private SheetAudit() { }

    public static String toJson(Object obj) {
        try { return JSON.writeValueAsString(obj); }
        catch (Exception e) { return "null"; }
    }

    public static void send(Instant entry, Instant exit, long start, String requestId,
                            String input, String output, int status) {
        String url = System.getenv("CIE_AUDIT_URL");
        if (url == null || url.trim().isEmpty()) url = DEFAULT_URL;
        final String target = url;
        final String secret = System.getenv("CIE_AUDIT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            System.err.println("CIE audit disabled: CIE_AUDIT_SECRET missing");
            return;
        }
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        final String safeInput = redact(input);
        final String safeOutput = redact(output);
        EXEC.execute(() -> {
            HttpURLConnection con = null;
            try {
                Map<String,Object> event = new LinkedHashMap<>();
                event.put("secret", secret);
                event.put("entrada", entry.toString());
                event.put("salida", exit.toString());
                event.put("requestId", requestId == null ? "" : requestId);
                event.put("jsonEntrada", safeInput);
                event.put("jsonSalida", safeOutput);
                event.put("duracionMs", elapsed);
                event.put("httpStatus", status);
                byte[] bytes = JSON.writeValueAsBytes(event);
                con = (HttpURLConnection) new URL(target).openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setConnectTimeout(4000);
                con.setReadTimeout(6000);
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream os = con.getOutputStream()) { os.write(bytes); }
                int code = con.getResponseCode();
                if (code < 200 || code >= 300) {
                    System.err.println("CIE audit HTTP " + code);
                    return;
                }
                // Apps Script ContentService may redirect to googleusercontent.com.
                try (InputStream stream = con.getInputStream()) {
                    byte[] buffer = new byte[2048];
                    int n = stream.read(buffer);
                    if (n > 0) {
                        String body = new String(buffer, 0, n, StandardCharsets.UTF_8);
                        if (body.contains("UNAUTHORIZED") || body.contains("\"ERROR\""))
                            System.err.println("CIE audit rejected event by Apps Script");
                    }
                }
            } catch (Exception e) {
                System.err.println("CIE audit unavailable: " + e.getClass().getSimpleName());
            } finally { if (con != null) con.disconnect(); }
        });
    }

    private static String redact(String source) {
        if (source == null) return "null";
        try {
            JsonNode node = JSON.readTree(source);
            mask(node);
            return JSON.writeValueAsString(node);
        } catch (Exception e) {
            // Never send unparsed text, which may contain secrets.
            return "\"[UNPARSEABLE_OR_NON_JSON_RESPONSE]\"";
        }
    }
    private static void mask(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            java.util.Iterator<String> keys = obj.getFieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                String k = key.toLowerCase(java.util.Locale.ROOT);
                if (k.equals("tsec") || k.equals("password") || k.equals("token") ||
                    k.equals("authorization") || k.equals("secret") ||
                    k.equals("ctacarg") || k.equals("numetdc") ||
                    k.equals("contror") || k.equals("usuario") ||
                    k.equals("dcocarg") || k.equals("aorigab")) {
                    obj.put(key, "[REDACTED]");
                } else mask(obj.get(key));
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (JsonNode item : arr) mask(item);
        }
    }
}
