package com.bbva.services;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.map.ObjectMapper;

/** Simulador de integracion CIE. Nunca ejecuta movimientos monetarios. */
@Path("services")
public class NotificationService {
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("MMdd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HHmmss");
    private static final ObjectMapper INPUT_JSON = new ObjectMapper();
    private static final SecureRandom FOLIO_RANDOM = new SecureRandom();
    // Limita peticiones demoradas. Los timeouts de proxies pueden cortar la conexion.
    private static final Semaphore DELAY_SLOTS = new Semaphore(2);
    private static final Pattern DELAY = Pattern.compile("(?:^|[\\s_])DELAY_(2M|5M|10M|150S)$");

    @PUT
    @Path("notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response notificationProcessPut(
            @HeaderParam("tsec") String tsec,
            @HeaderParam("ConsumerRequestID") String requestId,
            @HeaderParam("x-rho-traceid") String rhoTraceId,
            String jsonEntrada) {
        long start = System.nanoTime();
        Instant entry = Instant.now();
        String input = jsonEntrada == null ? "" : jsonEntrada;
        Response response;
        try {
            if (input.trim().isEmpty()) {
                response = error(400, "JSON requerido");
            } else {
                NotificationDTO dto;
                try {
                    dto = INPUT_JSON.readValue(input, NotificationDTO.class);
                } catch (java.io.IOException invalid) {
                    response = error(400, "JSON invalido");
                    audit(entry, start, requestId, rhoTraceId, input, response);
                    return response;
                }
                response = processNotification(tsec, requestId, dto);
            }
            audit(entry, start, requestId, rhoTraceId, input, response);
            return response;
        } catch (RuntimeException ex) {
            SheetAudit.send(entry, Instant.now(), start, requestId, rhoTraceId,
                    input, "{\"error\":\"Error interno\"}", 500);
            throw ex;
        }
    }

    private static void audit(Instant entry, long start, String requestId,
                              String rhoTraceId, String input, Response response) {
        Object entity = response.getEntity();
        String output = entity instanceof String ? (String) entity : SheetAudit.toJson(entity);
        SheetAudit.send(entry, Instant.now(), start, requestId, rhoTraceId,
                input, output, response.getStatus());
    }

    private Response processNotification(String tsec, String requestId, NotificationDTO dto) {
        if (dto == null) return error(400, "JSON requerido");
        String tipo = trim(dto.getTipoper()).toUpperCase(Locale.ROOT);
        if (missing(dto.getCanalor()) || missing(dto.getContror()) ||
                missing(dto.getServior()) || missing(dto.getCnvenio()) ||
                missing(dto.getGuiacie()) || dto.getNummens() == null ||
                missing(dto.getTimesta()) || missing(dto.getTrminal()) ||
                missing(dto.getRferenc()) || missing(dto.getMoneda()) ||
                missing(dto.getIded()) || missing(dto.getTrns()) ||
                !("CON".equals(tipo) || "PAG".equals(tipo) ||
                  "COB".equals(tipo) || "REV".equals(tipo))) {
            dto.setCodresp("30");
            return ok(dto, tsec, requestId);
        }
        // Se valida el importe antes de activar un escenario de prueba.
        if (!"CON".equals(tipo) && (dto.getImptota() == null ||
                !Double.isFinite(dto.getImptota()) || dto.getImptota() <= 0.0)) {
            dto.setCodresp("30");
            return ok(dto, tsec, requestId);
        }

        // Exclusivo para despliegues aislados de simulacion: cncepto activa el escenario.
        // No usar este endpoint para transacciones reales.
        String concepto = trim(dto.getCncepto()).toUpperCase(Locale.ROOT);
        String codigo = codeForConcept(concepto);
        long delay = delayForConcept(concepto);
        if (delay > 0) {
            if (!DELAY_SLOTS.tryAcquire()) return error(503, "Simulador ocupado");
            try {
                Thread.sleep(delay);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return error(503, "Espera de simulacion interrumpida");
            } finally {
                DELAY_SLOTS.release();
            }
        }
        if (codigo != null) {
            dto.setCodresp(codigo);
            return ok(dto, tsec, requestId);
        }

        dto.setCodresp("00");
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        if ("CON".equals(tipo)) {
            // Valores exclusivamente simulados, no saldo de una cuenta real.
            dto.setImptota(650.91);
            dto.setImminem(207.05);
            dto.setImmaxem(1000.00);
            dto.setFhvigem(now.plusDays(30).toLocalDate().toString());
        } else if ("PAG".equals(tipo)) {
            // Solo en PAG: fechas y folio de prueba si no llegaron informados.
            if (missing(dto.getFecpago())) dto.setFecpago(now.format(FECHA));
            if (missing(dto.getHorpago())) dto.setHorpago(now.format(HORA));
            if (missing(dto.getFlpagem())) dto.setFlpagem(generateTestFolio());
        }
        // COB y REV son acuses simulados: no inventan nuevos folios ni fechas.
        return ok(dto, tsec, requestId);
    }

    private static String codeForConcept(String concept) {
        // Se reconoce solo el token final, opcionalmente antes de DELAY.
        // Ejemplos: "Prueba REJ", "Prueba REJ_DELAY_2M".
        String scenario = concept.replaceFirst("(?:^|[\\s_])DELAY_(2M|5M|10M|150S)$", "").trim();
        Matcher token = Pattern.compile("(?:^|[\\s_])(SUSP|CANC|REJ|LIMIT|INV|DUP|UNREF|UNMER|NOENT|ANNUL|PROHIB|UNAVAIL)$")
                .matcher(scenario);
        return token.find() ? codeForToken(token.group(1)) : null;
    }

    private static String codeForToken(String token) {
        switch (token) {
            case "SUSP": return "03";
            case "CANC": return "04";
            case "REJ": return "05";
            case "LIMIT": return "07";
            case "INV": return "10";
            case "DUP": return "12";
            case "UNREF": return "13";
            case "UNMER": return "14";
            case "NOENT": return "15";
            case "ANNUL": return "17";
            case "PROHIB": return "20";
            case "UNAVAIL": return "91";
            default: return null;
        }
    }

    private static long delayForConcept(String concept) {
        Matcher matcher = DELAY.matcher(concept);
        if (!matcher.find()) return 0L;
        switch (matcher.group(1)) {
            case "2M": return 120000L;
            case "5M": return 300000L;
            case "10M": return 600000L;
            case "150S": return 150000L;
            default: return 0L;
        }
    }

    private static String generateTestFolio() {
        // 12 digitos; NO garantiza unicidad global entre Render y Railway.
        long value = 100000000000L + (long) (FOLIO_RANDOM.nextDouble() * 900000000000L);
        return Long.toString(value);
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static boolean missing(String s) { return trim(s).isEmpty(); }

    private static Response error(int status, String message) {
        return Response.status(status).entity("{\"error\":\"" + message + "\"}")
                .type(MediaType.APPLICATION_JSON).build();
    }

    private Response ok(NotificationDTO dto, String tsec, String requestId) {
        dto.normalizeForResponse();
        return Response.ok(dto, MediaType.APPLICATION_JSON)
                .header("tsec", tsec == null ? "" : tsec)
                .header("ConsumerRequestID", requestId == null ? "" : requestId)
                .header("X-CEL-Simulation", "true")
                .build();
    }
}
