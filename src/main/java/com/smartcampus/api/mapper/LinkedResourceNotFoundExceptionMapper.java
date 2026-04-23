package com.smartcampus.api.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.smartcampus.api.exception.LinkedResourceNotFoundException;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        return MapperSupport.buildResponse(422, "Unprocessable Entity", exception.getMessage(), uriInfo);
    }
}
