package org.jqassistant.contrib.plugin.docker.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;

@Label(value = "Image", usingIndexedPropertyOf = DockerDigestTemplate.class)
public interface DockerImageDescriptor extends DockerDescriptor, DockerDigestTemplate {
}
