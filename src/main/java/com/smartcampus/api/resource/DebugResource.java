package com.smartcampus.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {
    @GET
    @Path("/crash")
    public String crash() {
        throw new NullPointerException("Intentional crash endpoint for 500 error demonstration.");
    }
}
