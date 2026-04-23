package com.smartcampus.api.mapper;

import java.time.Instant;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

final class MapperSupport {
    private MapperSupport() {
    }

    static Response buildResponse(Response.Status status, String message, UriInfo uriInfo) {
        return buildResponse(status.getStatusCode(), status.getReasonPhrase(), message, uriInfo);
    }

    static Response buildResponse(int statusCode, String reasonPhrase, String message, UriInfo uriInfo) {
        String path = uriInfo == null ? "" : uriInfo.getRequestUri().getPath();
        String errorJson = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
                statusCode,
                escapeJson(reasonPhrase),
                escapeJson(message),
                escapeJson(path),
                escapeJson(Instant.now().toString()));

        return Response.status(statusCode)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorJson)
                .build();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
