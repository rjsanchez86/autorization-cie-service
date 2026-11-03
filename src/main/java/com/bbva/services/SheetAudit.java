package com.bbva.services;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.ArrayNode;
import java.util.Iterator;
import java.util.Map;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/** Auditoria best-effort: nunca interrumpe el servicio de negocio. */
public final class SheetAudit {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cie-sheet-audit"); t.setDaemon(true); return t;
    });
    static { JSON.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false); }
    private SheetAudit() { }
    public static String toJson(Object obj) {
        try { return JSON.writeValueAsString(obj); }
        catch (Exception e) { return "null"; }
    }
    public static void send(Instant entry, Instant exit, long start, String requestId,
                            String input, String output, int status) {
        String url = System.getenv("CIE_AUDIT_URL");
        String secret = System.getenv("CIE_AUDIT_SECRET");
        if (url == null || url.isEmpty() || secret == null || secret.isEmpty()) return;
        final long elapsed = (System.nanoTime() - start) / 1000000L;
        EXEC.execute(() -> {
            HttpURLConnection con = null;
            try {
                java.util.Map<String,Object> event = new java.util.LinkedHashMap<>();
                event.put("secret", secret);
                event.put("entrada", entry.toString());
                event.put("salida", exit.toString());
                event.put("requestId", requestId);
                // JSON embebido como texto, no como objeto de Apps Script
                event.put("jsonEntrada", redact(input));
                event.put("jsonSalida", redact(output));
                event.put("duracionMs", elapsed);
                event.put("httpStatus", status);
                byte[] data = JSON.writeValueAsBytes(event);
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setInstanceFollowRedirects(false);
                con.setRequestMethod("POST"); con.setDoOutput(true);
                con.setConnectTimeout(2000); con.setReadTimeout(3000);
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream os = con.getOutputStream()) { os.write(data); }
                int code = con.getResponseCode();
                // Apps Script ContentService suele responder con redireccion 302.
                // La redireccion se consulta con GET; nunca reenviar el secreto en la URL.
                if (code == 302 || code == 303) {
                    String location = con.getHeaderField("Location");
                    if (location == null || !location.startsWith("https://script.googleusercontent.com/")) {
                        System.err.println("CIE audit redirect inesperado");
                        return;
                    }
                    con.disconnect();
                    con = (HttpURLConnection) new URL(location).openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(2000);
                    con.setReadTimeout(3000);
                    code = con.getResponseCode();
                }
                if (code < 200 || code >= 300) {
                    System.err.println("CIE audit HTTP " + code);
                } else {
                    String body = readBody(con);
                    if (!body.contains("\"status\":\"OK\"")) {
                        System.err.println("CIE audit: respuesta no confirmada por Apps Script");
                    }
                }
            } catch (Exception ex) {
                System.err.println("CIE audit unavailable: " + ex.getClass().getSimpleName());
            } finally { if (con != null) con.disconnect(); }
        });
    }
    private static String readBody(HttpURLConnection con) throws Exception {
        try (InputStream in = con.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) != -1 && out.size() < 8192) {
                out.write(buf, 0, Math.min(n, 8192 - out.size()));
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String redact(String value) {
        if (value == null) return "null";
        try {
            JsonNode tree = JSON.readTree(value);
            if (tree == null) return "null";
            maskTree(tree);
            return JSON.writeValueAsString(tree);
        } catch (Exception ex) {
            // Nunca enviar texto no validado si el enmascaramiento fallo.
            return "[CONTENIDO NO DISPONIBLE]";
        }
    }

    private static void maskTree(JsonNode node) {
        if (node.isArray()) {
            for (JsonNode child : (ArrayNode) node) maskTree(child);
        } else if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = object.getFields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String name = field.getKey().toLowerCase(java.util.Locale.ROOT);
                JsonNode child = field.getValue();
                if (name.matches("tsec|password|passwd|token|authorization|secret|api[_-]?key|cookie|set-cookie")) {
                    object.put(field.getKey(), "[REDACTED]");
                } else if (name.matches("contror|numetdc|ctacarg|dcocarg|aorigab|usuario|iban|cardnumber|pan")) {
                    // Por defecto se oculta completamente: no persistir cuentas ni tarjetas.
                    object.put(field.getKey(), "[REDACTED]");
                } else {
                    maskTree(child);
                }
            }
        }
    }
}
