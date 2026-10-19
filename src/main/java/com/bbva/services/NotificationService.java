package com.bbva.services;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/** Simulador CEL exclusivamente para pruebas de integracion, sin movimientos monetarios. */
@Path("services")
public class NotificationService {
    private static final boolean DEMO = Boolean.parseBoolean(
            System.getenv().getOrDefault("CEL_DEMO_MODE", "false"));
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");

    @PUT
    @Path("notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response notificationProcessPut(
            @HeaderParam("tsec") String tsec,
            @HeaderParam("ConsumerRequestID") String requestId,
            NotificationDTO dto) {
        if (!DEMO) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Simulador deshabilitado: CEL_DEMO_MODE debe ser true solo en pruebas")
                    .type(MediaType.TEXT_PLAIN).build();
        }
        if (dto == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("JSON requerido").type(MediaType.TEXT_PLAIN).build();
        }
        String tipo = trim(dto.getTipoper()).toUpperCase(Locale.ROOT);
        String referencia = trim(dto.getRferenc()).toUpperCase(Locale.ROOT);
        if (missing(dto.getCanalor()) || missing(dto.getContror()) ||
                missing(dto.getServior()) || missing(dto.getCnvenio()) ||
                missing(dto.getGuiacie()) || dto.getNummens() == null ||
                missing(dto.getTimesta()) || missing(dto.getTrminal()) ||
                missing(dto.getRferenc()) || missing(dto.getMoneda()) ||
                missing(dto.getIded()) || missing(dto.getTrns())) {
            dto.setCodresp("30"); // Formato incorrecto (Anexo B)
            return ok(dto, tsec, requestId);
        }
        if (!("CON".equals(tipo) || "PAG".equals(tipo) ||
                "COB".equals(tipo) || "REV".equals(tipo))) {
            dto.setCodresp("30");
            return ok(dto, tsec, requestId);
        }
        // Escenarios funcionales de prueba. No sustituye validaciones de negocio reales.
        String rechazo = testCode(referencia);
        if (rechazo != null) {
            dto.setCodresp(rechazo);
            return ok(dto, tsec, requestId);
        }
        dto.setCodresp("00");
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        if ("CON".equals(tipo)) {
            // Saldos deterministas SOLO para pruebas, nunca saldos reales.
            dto.setImptota(650.91);
            dto.setImminem(207.05);
            dto.setImmaxem(1000.00);
            dto.setFhvigem(now.plusDays(30).toLocalDate().toString());
        } else {
            // PAG/COB/REV: el importe proviene de BBVA; no modificarlo.
            if (dto.getImptota() == null || dto.getImptota() <= 0.0) {
                dto.setCodresp("30");
                return ok(dto, tsec, requestId);
            }
            if ("PAG".equals(tipo) || "COB".equals(tipo)) {
                dto.setFecpago(now.format(DateTimeFormatter.ofPattern("MMdd")));
                dto.setHorpago(now.format(DateTimeFormatter.ofPattern("HHmmss")));
            }
            if ("COB".equals(tipo)) {
                // Folio determinista de prueba; no representa acreditacion real.
                String tx = trim(dto.getTrns());
                dto.setFlpagem("SIM" + (tx.length() > 9 ? tx.substring(tx.length() - 9) : tx));
            }
            // REV: solo acuse simulado; no realiza reverso contable.
        }
        return ok(dto, tsec, requestId);
    }

    private static String testCode(String ref) {
        if (ref.endsWith("SUSP")) return "03";
        if (ref.endsWith("CANC")) return "04";
        if (ref.endsWith("REJ")) return "05";
        if (ref.endsWith("LIMIT")) return "07";
        if (ref.endsWith("INV")) return "10";
        if (ref.endsWith("DUP")) return "12";
        if (ref.endsWith("UNREF")) return "13";
        if (ref.endsWith("UNMER")) return "14";
        if (ref.endsWith("NOENT")) return "15";
        if (ref.endsWith("ANNUL")) return "17";
        if (ref.endsWith("PROHIB")) return "20";
        if (ref.endsWith("UNAVAIL")) return "91";
        return null;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static boolean missing(String s) { return trim(s).isEmpty(); }
    private Response ok(NotificationDTO dto, String tsec, String requestId) {
        return Response.ok(dto, MediaType.APPLICATION_JSON)
                .header("tsec", tsec == null ? "" : tsec)
                .header("ConsumerRequestID", requestId == null ? "" : requestId)
                .header("X-CEL-Simulation", "true")
                .build();
    }
}
