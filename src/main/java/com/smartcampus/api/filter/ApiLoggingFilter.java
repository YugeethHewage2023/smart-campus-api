package com.smartcampus.api.filter;

import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOGGER = Logger.getLogger(ApiLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            LOGGER.info(String.format("Incoming request: %s %s",
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getRequestUri()));
        } catch (RuntimeException exception) {
            LOGGER.fine("Skipping request log because request metadata was unavailable.");
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        try {
            LOGGER.info(String.format("Outgoing response: %s %s -> %d",
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getRequestUri(),
                    responseContext.getStatus()));
        } catch (RuntimeException exception) {
            LOGGER.fine("Skipping response log because request metadata was unavailable.");
        }
    }
}
