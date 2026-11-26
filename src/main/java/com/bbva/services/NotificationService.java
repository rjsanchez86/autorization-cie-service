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

    // Delays por operacion (ej: PAG_DELAY_5S, COB_DELAY_10S) y delay general (DELAY_10S)
    private static final Pattern OP_DELAY = Pattern
            .compile("(?:^|[\\s_])(CON|PAG|COB|REV)_DELAY_(\\d+)S(?:$|[\\s_])");
    private static final Pattern DELAY = Pattern
            .compile("(?:^|[\\s_])DELAY_(\\d+)S(?:$|[\\s_])");

    // Escenarios por operacion (ej: PAG_05, COB_03, PAG_REJ, COB_SUSP) y escenario general
    private static final Pattern OP_SCENARIO = Pattern
            .compile("(?:^|[\\s_])(CON|PAG|COB|REV)_([A-Z0-9]{2,7})(?:$|[\\s_])");
    private static final Pattern SCENARIO = Pattern
            .compile("(?:^|[\\s_])(SUSP|CANC|REJ|LIMIT|INV|DUP|UNREF|UNMER|NOENT|ANNUL|PROHIB|JSONER|TIMEOU|UNAVAIL|SYSTEMF)(?:$|[\\s_])");

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
        if (dto == null)
            return error(400, "JSON requerido");
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

        // 1. Evaluar delay (especifico por operacion o general)
        long delay = delayForConcept(tipo, concepto);
        if (delay > 0) {
            if (!DELAY_SLOTS.tryAcquire())
                return error(503, "Simulador ocupado");
            try {
                Thread.sleep(delay);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return error(503, "Espera de simulacion interrumpida");
            } finally {
                DELAY_SLOTS.release();
            }
        }

        // 2. Buscar codigo especifico para la operacion actual (ej. PAG_05, COB_03, COB_SUSP)
        String codigo = codeForOperationAndConcept(tipo, concepto);

        // 3. Si no hay especifico por operacion, buscar escenario general (ej. REJ, SUSP)
        if (codigo == null) {
            codigo = codeForConcept(concepto);
        }

        // Si existe un codigo forzado distinto de "00", responder con ese codigo
        if (codigo != null && !"00".equals(codigo)) {
            dto.setCodresp(codigo);
            return ok(dto, tsec);
        }

        // 4. Flujo exitoso ("00") por tipo de operacion
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        dto.setCodresp("00");
        if ("CON".equals(tipo)) {
            dto.setImptota(650.91);
            dto.setImminem(207.05);
            dto.setImmaxem(1000.00);
            dto.setFhvigem(now.plusDays(30).toLocalDate().toString());
            dto.setFecpago("");
            dto.setHorpago("");
            dto.setFlpagem("");
            dto.setClcurem("Dato");
            dto.setCncepto("Prueba CON");
        } else if ("PAG".equals(tipo)) {
            dto.setImminem(0.0);
            dto.setImmaxem(0.0);
            dto.setFhvigem("");
            dto.setFecpago(now.format(FECHA));
            dto.setHorpago(now.format(HORA));
            dto.setFlpagem("123456789012");
        } else if ("COB".equals(tipo)) {
            dto.setImminem(0.0);
            dto.setImmaxem(0.0);
            dto.setFhvigem("");
            dto.setFecpago(now.format(FECHA));
            dto.setHorpago(now.format(HORA));
            dto.setFlpagem("123456789012");
        } else if ("REV".equals(tipo)) {
            dto.setImminem(0.0);
            dto.setImmaxem(0.0);
            dto.setFhvigem("");
            dto.setFecpago("");
            dto.setHorpago("");
            dto.setFlpagem("");
        }

        return ok(dto, tsec);
    }

    /** Diagnostico de la validacion EXISTENTE; no altera el contrato funcional. */
    private static List<String> missingFields(NotificationDTO dto, String tipo) {
        List<String> fields = new ArrayList<>();
        if (missing(dto.getCanalor()))
            fields.add("canalor");
        if (missing(dto.getContror()))
            fields.add("contror");
        if (missing(dto.getServior()))
            fields.add("servior");
        if (missing(dto.getCnvenio()))
            fields.add("cnvenio");
        if (missing(dto.getGuiacie()))
            fields.add("guiacie");
        if (dto.getNummens() == null)
            fields.add("nummens");
        if (missing(dto.getTimesta()))
            fields.add("timesta");
        if (missing(dto.getTrminal()))
            fields.add("trminal");
        if (missing(dto.getRferenc()))
            fields.add("rferenc");
        if (missing(dto.getMoneda()))
            fields.add("moneda");
        if (missing(dto.getIded()))
            fields.add("ided");
        if (missing(dto.getTrns()))
            fields.add("trns");
        if (!("CON".equals(tipo) || "PAG".equals(tipo) ||
                "COB".equals(tipo) || "REV".equals(tipo)))
            fields.add("tipoper");
        return fields;
    }

    private static String codeForOperationAndConcept(String tipo, String concept) {
        String cleanConcept = stripDelays(concept);
        Matcher matcher = OP_SCENARIO.matcher(cleanConcept);
        while (matcher.find()) {
            String op = matcher.group(1);
            String value = matcher.group(2);
            if (op.equals(tipo)) {
                if (value.matches("\\d{2}")) {
                    return value;
                }
                return codeForToken(value);
            }
        }
        return null;
    }

    private static String codeForConcept(String concept) {
        String scenario = stripDelays(concept);
        Matcher token = SCENARIO.matcher(scenario);
        return token.find() ? codeForToken(token.group(1)) : null;
    }

    private static String codeForToken(String token) {
        switch (token) {
            case "SUSP":
                return "03";
            case "CANC":
                return "04";
            case "REJ":
                return "05";
            case "LIMIT":
                return "07";
            case "INV":
                return "10";
            case "DUP":
                return "12";
            case "UNREF":
                return "13";
            case "UNMER":
                return "14";
            case "NOENT":
                return "15";
            case "ANNUL":
                return "17";
            case "PROHIB":
                return "20";
            case "JSONER":
                return "30";
            case "TIMEOU":
                return "68";
            case "UNAVAIL":
                return "91";
            case "SYSTEMF":
                return "96";
            default:
                return null;
        }
    }

    private static long delayForConcept(String tipo, String concept) {
        Matcher opMatcher = OP_DELAY.matcher(concept);
        while (opMatcher.find()) {
            if (opMatcher.group(1).equals(tipo)) {
                return parseSecondsToMillis(opMatcher.group(2));
            }
        }
        Matcher matcher = DELAY.matcher(concept);
        if (matcher.find()) {
            return parseSecondsToMillis(matcher.group(1));
        }
        return 0L;
    }

    private static long parseSecondsToMillis(String secondsStr) {
        try {
            long segundos = Long.parseLong(secondsStr);
            if (segundos < 1 || segundos > 600)
                return 0L;
            return segundos * 1000L;
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static String stripDelays(String concept) {
        return concept
                .replaceAll("(?:^|[\\s_])(?:CON|PAG|COB|REV)_DELAY_\\d+S(?=$|[\\s_])", " ")
                .replaceAll("(?:^|[\\s_])DELAY_\\d+S(?=$|[\\s_])", " ")
                .trim();
    }

    private static String generateTestFolio() {
        long value = 100000000000L + (long) (FOLIO_RANDOM.nextDouble() * 900000000000L);
        return Long.toString(value);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean missing(String s) {
        return trim(s).isEmpty();
    }

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