package org.jqassistant.plugin.docker.api.model;

import java.util.List;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Repository")
public interface DockerRepositoryDescriptor extends DockerDescriptor, NamedDescriptor {

    @Relation("CONTAINS_TAG")
    List<DockerTagDescriptor> getTags();

}
