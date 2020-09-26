package org.jqassistant.contrib.plugin.docker.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import com.buschmais.xo.neo4j.api.annotation.Relation.Incoming;
import com.buschmais.xo.neo4j.api.annotation.Relation.Outgoing;

@Relation("DECLARES_LAYER")
public interface DeclaresLayerDescriptor extends Descriptor {

    int getIndex();

    void setIndex(int index);

    @Outgoing
    DockerManifestDescriptor getManifestDescriptor();

    @Incoming
    DockerBlobDescriptor getBlobDescriptor();
}
