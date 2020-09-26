package org.jqassistant.contrib.plugin.docker.api.model;

import java.util.List;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import com.buschmais.xo.neo4j.api.annotation.Relation.Incoming;

@Label(value = "Blob", usingIndexedPropertyOf = DockerDigestTemplate.class)
public interface DockerBlobDescriptor extends DockerDescriptor, DockerDigestTemplate {

    String getMediaType();

    void setMediaType(String mediaType);

    long getSize();

    void setSize(long size);

    List<DeclaresLayerDescriptor> getDeclaresLayer();

    @Incoming
    @Relation("CONTAINS_BLOB")
    DockerRegistryDescriptor getRegistry();

    void setRegistry(DockerRegistryDescriptor registryDescriptor);

}
