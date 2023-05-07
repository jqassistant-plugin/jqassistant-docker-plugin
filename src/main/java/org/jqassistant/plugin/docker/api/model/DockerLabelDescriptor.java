package org.jqassistant.plugin.docker.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;

@Label("Label")
public interface DockerLabelDescriptor extends DockerDescriptor {

    String getName();

    void setName(String name);

    String getValue();

    void setValue(String value);

}
