 package com.smartcampus.api.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response.StatusType statusInfo = exception.getResponse().getStatusInfo();
        Response.Status status = Response.Status.fromStatusCode(statusInfo.getStatusCode());
        if (status == null) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = statusInfo.getReasonPhrase();
        }

        return MapperSupport.buildResponse(status, message, uriInfo);
    }
}
