package com.bbva.services;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

/** Simulador CIE: nunca ejecuta movimientos monetarios. */
@Path("services")
public class NotificationService {
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("MMdd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HHmmss");
    private static final ObjectMapper INPUT_JSON = new ObjectMapper();
    private static final SecureRandom FOLIO_RANDOM = new SecureRandom();
    private static final Semaphore DELAY_SLOTS = new Semaphore(2);
    // CAMBIO 1: DELAY_1S a DELAY_600S (segundos).
    private static final Pattern DELAY = Pattern.compile("(?:^|[\\s_])DELAY_(\\d+)S$");
    private static final Pattern SCENARIO = Pattern.compile("(?:^|[\\s_])(SUSP|CANC|REJ|LIMIT|INV|DUP|UNREF|UNMER|NOENT|ANNUL|PROHIB|UNAVAIL)$");

    @PUT
    @Path("notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response notificationProcessPut(
            @HeaderParam("tsec") String tsec,
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
                    audit(entry, start, rhoTraceId, input, response);
                    return response;
                }
                response = processNotification(tsec, dto);
            }
            audit(entry, start, rhoTraceId, input, response);
            return response;
        } catch (RuntimeException ex) {
            SheetAudit.send(entry, Instant.now(), start, rhoTraceId,
                    input, "{\"error\":\"Error interno\"}", 500);
            throw ex;
        }
    }

    private static void audit(Instant entry, long start, String rhoTraceId,
                              String input, Response response) {
        Object entity = response.getEntity();
        String output = entity instanceof String ? (String) entity : SheetAudit.toJson(entity);
        SheetAudit.send(entry, Instant.now(), start, rhoTraceId,
                input, output, response.getStatus());
    }

    private Response processNotification(String tsec, NotificationDTO dto) {
        if (dto == null) return error(400, "JSON requerido");
        String tipo = trim(dto.getTipoper()).toUpperCase(Locale.ROOT);
        List<String> missingFields = missingFields(dto, tipo);
        if (!missingFields.isEmpty()) {
            // Solo nombres de campos; nunca registrar valores sensibles.
            System.out.println("[CIE-VALIDACION] codresp=30 campos=" + String.join(",", missingFields));
            dto.setCodresp("30");
            return ok(dto, tsec);
        }
        if (!"CON".equals(tipo) && (dto.getImptota() == null ||
                !Double.isFinite(dto.getImptota()) || dto.getImptota() <= 0.0)) {
            System.out.println("[CIE-VALIDACION] codresp=30 motivo=imptota_invalido");
            dto.setCodresp("30");
            return ok(dto, tsec);
        }

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
            return ok(dto, tsec);
        }

        dto.setCodresp("00");
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        if ("CON".equals(tipo)) {
            dto.setImptota(650.91);
            dto.setImminem(207.05);
            dto.setImmaxem(1000.00);
            dto.setFhvigem(now.plusDays(30).toLocalDate().toString());
        } else if ("PAG".equals(tipo)) {
            if (missing(dto.getFecpago())) dto.setFecpago(now.format(FECHA));
            if (missing(dto.getHorpago())) dto.setHorpago(now.format(HORA));
            if (missing(dto.getFlpagem())) dto.setFlpagem(generateTestFolio());
        }
        // COB y REV: no generar folios ni fechas adicionales.
        return ok(dto, tsec);
    }

    /** Diagnostico de la validacion EXISTENTE; no altera el contrato funcional. */
    private static List<String> missingFields(NotificationDTO dto, String tipo) {
        List<String> fields = new ArrayList<>();
        if (missing(dto.getCanalor())) fields.add("canalor");
        if (missing(dto.getContror())) fields.add("contror");
        if (missing(dto.getServior())) fields.add("servior");
        if (missing(dto.getCnvenio())) fields.add("cnvenio");
        if (missing(dto.getGuiacie())) fields.add("guiacie");
        if (dto.getNummens() == null) fields.add("nummens");
        if (missing(dto.getTimesta())) fields.add("timesta");
        if (missing(dto.getTrminal())) fields.add("trminal");
        if (missing(dto.getRferenc())) fields.add("rferenc");
        if (missing(dto.getMoneda())) fields.add("moneda");
        if (missing(dto.getIded())) fields.add("ided");
        if (missing(dto.getTrns())) fields.add("trns");
        if (!("CON".equals(tipo) || "PAG".equals(tipo) ||
              "COB".equals(tipo) || "REV".equals(tipo))) fields.add("tipoper");
        return fields;
    }

    private static String codeForConcept(String concept) {
        // CAMBIO 2: quitar DELAY_nS para reconocer escenarios como REJ DELAY_10S.
        String scenario = concept.replaceFirst("(?:^|[\\s_])DELAY_\\d+S$", "").trim();
        Matcher token = SCENARIO.matcher(scenario);
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
        // CAMBIO 3: convertir segundos a milisegundos; maximo 600 segundos.
        Matcher matcher = DELAY.matcher(concept);
        if (!matcher.find()) return 0L;
        try {
            long segundos = Long.parseLong(matcher.group(1));
            if (segundos < 1 || segundos > 600) return 0L;
            return segundos * 1000L;
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static String generateTestFolio() {
        long value = 100000000000L + (long) (FOLIO_RANDOM.nextDouble() * 900000000000L);
        return Long.toString(value);
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static boolean missing(String s) { return trim(s).isEmpty(); }

    private static Response error(int status, String message) {
        return Response.status(status).entity("{\"error\":\"" + message + "\"}")
                .type(MediaType.APPLICATION_JSON).build();
    }

    private Response ok(NotificationDTO dto, String tsec) {
        dto.normalizeForResponse();
        return Response.ok(dto, MediaType.APPLICATION_JSON)
                .header("tsec", tsec == null ? "" : tsec)
                .header("X-CEL-Simulation", "true")
                .build();
    }
}
