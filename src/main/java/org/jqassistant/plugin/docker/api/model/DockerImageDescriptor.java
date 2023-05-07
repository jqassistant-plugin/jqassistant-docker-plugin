package org.jqassistant.plugin.docker.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import com.buschmais.xo.neo4j.api.annotation.Relation.Incoming;

@Label(value = "Image", usingIndexedPropertyOf = DockerDigestTemplate.class)
public interface DockerImageDescriptor extends DockerDescriptor, DockerDigestTemplate {

    @Incoming
    @Relation("CONTAINS_IMAGE")
    DockerRegistryDescriptor getRegistry();

    void setRegistry(DockerRegistryDescriptor dockerRegistryDescriptor);

}
