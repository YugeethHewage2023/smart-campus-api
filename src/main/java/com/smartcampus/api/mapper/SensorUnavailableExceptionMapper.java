package com.smartcampus.api.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.smartcampus.api.exception.SensorUnavailableException;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        return MapperSupport.buildResponse(Response.Status.FORBIDDEN, exception.getMessage(), uriInfo);
    }
}
