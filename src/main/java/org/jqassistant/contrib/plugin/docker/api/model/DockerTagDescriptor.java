package org.jqassistant.contrib.plugin.docker.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import com.buschmais.xo.neo4j.api.annotation.Relation.Incoming;

@Label("Tag")
public interface DockerTagDescriptor extends DockerDescriptor, NamedDescriptor {

    @Relation("HAS_MANIFEST")
    DockerManifestDescriptor getManifest();

    void setManifest(DockerManifestDescriptor manifest);

}
