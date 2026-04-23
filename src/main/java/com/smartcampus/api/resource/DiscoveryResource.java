package com.smartcampus.api.resource;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import com.smartcampus.api.store.CampusStore;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {
    private final CampusStore store = CampusStore.getInstance();

    @GET
    public Map<String, Object> discover(@Context UriInfo uriInfo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "Smart Campus API");
        payload.put("version", "v1");
        payload.put("description", "JAX-RS API for managing campus rooms, sensors, and historical sensor readings.");
        payload.put("contact", Map.of(
                "module", "5COSC022W Client-Server Architectures",
                "role", "Backend API Administrator",
                "email", "smartcampus-admin@westminster.ac.uk"));
        payload.put("counts", store.counts());
        payload.put("resources", Map.of(
                "self", uriInfo.getAbsolutePath().toString(),
                "rooms", uriInfo.getBaseUriBuilder().path(RoomResource.class).build().toString(),
                "sensors", uriInfo.getBaseUriBuilder().path(SensorResource.class).build().toString(),
                "debugCrash", uriInfo.getBaseUriBuilder().path(DebugResource.class).path("crash").build().toString()));
        return payload;
    }
}
