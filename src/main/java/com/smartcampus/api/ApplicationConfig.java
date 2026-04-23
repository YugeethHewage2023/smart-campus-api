package com.smartcampus.api;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;

import com.smartcampus.api.filter.ApiLoggingFilter;
import com.smartcampus.api.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.api.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.api.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.api.mapper.ThrowableExceptionMapper;
import com.smartcampus.api.mapper.WebApplicationExceptionMapper;
import com.smartcampus.api.resource.DebugResource;
import com.smartcampus.api.resource.DiscoveryResource;
import com.smartcampus.api.resource.RoomResource;
import com.smartcampus.api.resource.SensorResource;

@ApplicationPath("/api/v1")
public class ApplicationConfig extends ResourceConfig {
    public ApplicationConfig() {
        register(JacksonFeature.class);
        register(JacksonJaxbJsonProvider.class);

        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);
        register(DebugResource.class);

        register(ApiLoggingFilter.class);

        register(RoomNotEmptyExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(WebApplicationExceptionMapper.class);
        register(ThrowableExceptionMapper.class);
    }
}
