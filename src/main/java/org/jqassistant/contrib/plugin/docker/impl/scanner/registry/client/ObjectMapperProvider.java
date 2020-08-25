package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

	static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModules(new JavaTimeModule(),
			new Jdk8Module());

	@Override
	public ObjectMapper getContext(Class<?> aClass) {
		return OBJECT_MAPPER;
	}
}
