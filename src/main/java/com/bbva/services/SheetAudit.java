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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/** Auditoria best-effort: nunca interrumpe el servicio de negocio. */
public final class SheetAudit {
    private static final ObjectMapper JSON = new ObjectMapper();
    // Cola limitada para evitar crecimiento de memoria ante fallas de Apps Script.
    private static final ThreadPoolExecutor EXEC = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(500), r -> {
        Thread t = new Thread(r, "cie-sheet-audit"); t.setDaemon(true); return t;
    }, new ThreadPoolExecutor.AbortPolicy());
    static { JSON.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false); }
    private SheetAudit() { }
    public static String toJson(Object obj) {
        try { return JSON.writeValueAsString(obj); }
        catch (Exception e) { return "null"; }
    }
    public static void send(Instant entry, Instant exit, long start, String rhoTraceId,
                            String input, String output, int status) {
        String url = System.getenv("CIE_AUDIT_URL");
        String secret = System.getenv("CIE_AUDIT_SECRET");
        String deployment = System.getenv("CIE_DEPLOY_ENV");
        final String origenDespliegue = deployment == null || deployment.trim().isEmpty()
                ? "DESCONOCIDO" : deployment.trim();
        System.out.println("[CIE-AUDIT] Origen de despliegue: " + origenDespliegue);
        if (url == null || url.trim().isEmpty()) {
            System.err.println("[CIE-AUDIT] ERROR: CIE_AUDIT_URL no configurada");
            return;
        }
        if (secret == null || secret.trim().isEmpty()) {
            System.err.println("[CIE-AUDIT] ERROR: CIE_AUDIT_SECRET no configurada");
            return;
        }
        System.out.println("[CIE-AUDIT] Configuracion de auditoria detectada");
        final long elapsed = (System.nanoTime() - start) / 1000000L;
        try { EXEC.execute(() -> {
            HttpURLConnection con = null;
            try {
                java.util.Map<String,Object> event = new java.util.LinkedHashMap<>();
                System.out.println("[CIE-AUDIT] Enviando rhoTraceId " + rhoTraceId + " status " + status);
                event.put("secret", secret);
                event.put("entrada", entry.toString());
                event.put("salida", exit.toString());
                event.put("rhoTraceId", rhoTraceId == null ? "" : rhoTraceId.trim());       
                // JSON embebido como texto, no como objeto de Apps Script
                event.put("jsonEntrada", redact(input));
                event.put("jsonSalida", redact(output));
                event.put("duracionMs", elapsed);
                event.put("httpStatus", status);
                event.put("origenDespliegue", origenDespliegue);
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
                    try {
                        JsonNode confirmation = JSON.readTree(body);
                        if (confirmation == null || !"OK".equals(confirmation.path("status").asText())) {
                            System.err.println("[CIE-AUDIT] Apps Script no confirmó registro; status="
                                    + (confirmation == null ? "VACIO" : confirmation.path("status").asText()));
                        } else {
                            System.out.println("[CIE-AUDIT] Registro confirmado; plataforma=" + origenDespliegue);
                        }
                    } catch (Exception invalidResponse) {
                        System.err.println("[CIE-AUDIT] Respuesta de Apps Script no es JSON válido");
                    }
                }
            } catch (Exception ex) {
                System.err.println("[CIE-AUDIT] Fallo de comunicación: " + ex.getClass().getSimpleName()
                        + ": " + String.valueOf(ex.getMessage()).replaceAll("https?://[^\\s]+", "[URL]"));
            } finally { if (con != null) con.disconnect(); }
        });
        } catch (RejectedExecutionException queueFull) {
            System.err.println("[CIE-AUDIT] Cola llena: auditoría omitida sin afectar respuesta de negocio");
        }
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
                if (name.matches("tsec|password|passwd|token|authorization|secret|api[_-]?key|cookie|set-cookie")
                        || name.matches("numetdc|ctacarg|dcocarg|aorigab|usuario|iban|cardnumber|pan")) {
                    // No inventar un valor sensible cuando el dato original esta vacio.
                    // Mantener null como null y cadena vacia como cadena vacia.
                    if (child == null || child.isNull()) {
                        continue;
                    }
                    if (child.isTextual() && child.asText().isEmpty()) {
                        continue;
                    }
                    // Nunca almacenar credenciales ni identificadores completos.
                    object.put(field.getKey(), "[REDACTED]");
                } else {
                    maskTree(child);
                }
            }
        }
    }
}
