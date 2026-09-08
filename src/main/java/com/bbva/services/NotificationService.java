package com.bbva.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("services")
public class NotificationService {

    @POST
    @Path("notification")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response notificationProcessPost(
            @HeaderParam("tsec") String tsecHeader,
            @HeaderParam("ConsumerRequestID") String consumerRequestIdHeader,
            NotificationDTO dto) {
        return process(tsecHeader, consumerRequestIdHeader, dto);
    }

    @PUT
    @Path("notification")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response notificationProcessPut(
            @HeaderParam("tsec") String tsecHeader,
            @HeaderParam("ConsumerRequestID") String consumerRequestIdHeader,
            NotificationDTO dto) {
        return process(tsecHeader, consumerRequestIdHeader, dto);
    }

    private Response process(String tsecHeader, String consumerRequestIdHeader, NotificationDTO dto) {

        // 1. VALIDACIÓN MANDATORIOS AJUSTADA AL JSON RECIBIDO
        if (isMissing(dto.getCanalor()) || isMissing(dto.getContror()) || isMissing(dto.getServior())
                || isMissing(dto.getTipoper()) || isMissing(dto.getTrminal()) || isMissing(dto.getRferenc())
                || isMissing(dto.getCnvenio()) || isMissing(dto.getMoneda())) {
            
            dto.setCodresp("96");
            return buildResponse(tsecHeader, consumerRequestIdHeader, dto);
        }

        // Asignación de valores por defecto para campos ausentes en el JSON
        if (isMissing(dto.getGuiacie())) dto.setGuiacie("DEFAULT_GUIA");
        if (dto.getNummens() == null) dto.setNummens(1L);
        if (isMissing(dto.getTimesta())) dto.setTimesta(ZonedDateTime.now().toString());
        if (isMissing(dto.getIded())) dto.setIded("DEFAULT_IDED");
        if (isMissing(dto.getTrns())) dto.setTrns("DEFAULT_TRNS");

        // Normalizar imptota si sólo se recibe imefect
        if ((dto.getImptota() == null || dto.getImptota() == 0) && dto.getImefect() != null) {
            dto.setImptota(dto.getImefect());
        }

        // 2. SIMULACIÓN FORZADA (Si se envía codresp explícito diferente de 00)
        if (dto.getCodresp() != null && !dto.getCodresp().isEmpty() && !"00".equals(dto.getCodresp())) {
            return buildResponse(tsecHeader, consumerRequestIdHeader, dto);
        }

        // 3. REGLAS DINÁMICAS POR VALOR EN 'rferenc'
        String ref = dto.getRferenc().toUpperCase();

        if (ref.endsWith("SUSP")) {
            dto.setCodresp("03"); // Suspended Reference
        } else if (ref.endsWith("CANC")) {
            dto.setCodresp("04"); // Canceled Reference
        } else if (ref.endsWith("REJ")) {
            dto.setCodresp("05"); // Target Entity Rejection
        } else if (ref.endsWith("LIMIT")) {
            dto.setCodresp("07"); // Amount Exceeds Limit
        } else if (ref.endsWith("INV")) {
            dto.setCodresp("10"); // Invalid Reference Format
        } else if (ref.endsWith("DUP")) {
            dto.setCodresp("12"); // Duplicate Payment
        } else if (ref.endsWith("UNREF")) {
            dto.setCodresp("13"); // Uncataloged Reference
        } else if (ref.endsWith("UNMER")) {
            dto.setCodresp("14"); // Uncataloged Merchant
        } else if (ref.endsWith("NOENT")) {
            dto.setCodresp("15"); // Non-existent Target Entity
        } else if (ref.endsWith("ANNUL")) {
            dto.setCodresp("17"); // Cancellation
        } else if (ref.endsWith("PROHIB")) {
            dto.setCodresp("20"); // Prohibited Amount
        } else if (ref.endsWith("TIMEOUT")) {
            dto.setCodresp("68"); // Timeout
        } else if (ref.endsWith("UNAVAIL")) {
            dto.setCodresp("91"); // Entity Unavailable
        } else {
            dto.setCodresp("00");
        }

        if (!"00".equals(dto.getCodresp())) {
            return buildResponse(tsecHeader, consumerRequestIdHeader, dto);
        }

        // 4. LÓGICA EXITOSA (codresp = "00")
        if ("CON".equals(dto.getTipoper())) {
            if (dto.getFhvigem() == null || dto.getFhvigem().isEmpty()) {
                LocalDate fechaVigencia = LocalDate.now().plusYears(1);
                dto.setFhvigem(fechaVigencia.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }

            if (dto.getImptota() == null || dto.getImptota() == 0) {
                double imptota = ThreadLocalRandom.current().nextDouble(100.00, 5000.00);
                dto.setImptota(Math.round(imptota * 100.0) / 100.0);
            }

            if (dto.getImminem() == null || dto.getImminem() == 0) {
                double imminem = dto.getImptota() * 0.10;
                dto.setImminem(Math.round(imminem * 100.0) / 100.0);
            }

            if (dto.getImmaxem() == null || dto.getImmaxem() == 0) {
                dto.setImmaxem(dto.getImptota());
            }
        }

        if (("PAG".equals(dto.getTipoper()) || "COB".equals(dto.getTipoper()))
                && (dto.getHorpago() == null || dto.getHorpago().isEmpty())) {

            String horaActualCDMX = ZonedDateTime.now(ZoneId.of("America/Mexico_City"))
                    .format(DateTimeFormatter.ofPattern("HHmmss"));
            dto.setHorpago(horaActualCDMX);
        }

        return buildResponse(tsecHeader, consumerRequestIdHeader, dto);
    }

    private boolean isMissing(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Response buildResponse(String tsecHeader, String consumerRequestIdHeader, NotificationDTO dto) {
        if (dto.getAutoriz() == null) dto.setAutoriz("");
        if (dto.getCertiem() == null) dto.setCertiem("");
        if (dto.getClcurem() == null) dto.setClcurem("");
        if (dto.getCodfijo() == null) dto.setCodfijo("");
        if (dto.getFeccap() == null) dto.setFeccap("");
        if (dto.getFecpago() == null) dto.setFecpago("");
        if (dto.getFilesp1() == null) dto.setFilesp1("");
        if (dto.getFilesp2() == null) dto.setFilesp2("");
        if (dto.getFillmas() == null) dto.setFillmas("");
        if (dto.getFillnum() == null) dto.setFillnum("");
        if (dto.getFlpagem() == null) dto.setFlpagem("");
        if (dto.getTppagem() == null) dto.setTppagem("");
        if (dto.getFhvigem() == null) dto.setFhvigem("");
        if (dto.getHorpago() == null) dto.setHorpago("");
        if (dto.getImmaxem() == null) dto.setImmaxem(0.0);
        if (dto.getImminem() == null) dto.setImminem(0.0);
        if (dto.getImptota() == null) dto.setImptota(0.0);
        if (dto.getIdmsjpa() == null) dto.setIdmsjpa(0L);
        if (dto.getNummens() == null) dto.setNummens(0L);

        String responseTsec = (tsecHeader != null && !tsecHeader.trim().isEmpty()) ? tsecHeader : "";
        String responseRequestId = (consumerRequestIdHeader != null && !consumerRequestIdHeader.trim().isEmpty())
                ? consumerRequestIdHeader
                : "";

        return Response.ok(dto)
                .header("tsec", responseTsec)
                .header("ConsumerRequestID", responseRequestId)
                .build();
    }
}