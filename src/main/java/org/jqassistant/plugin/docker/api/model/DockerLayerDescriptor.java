package org.jqassistant.plugin.docker.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Layer")
public interface DockerLayerDescriptor extends DockerDescriptor {

    int getIndex();

    void setIndex(int index);

    @Relation("WITH_BLOB")
    DockerBlobDescriptor getBlob();

    void setBlob(DockerBlobDescriptor blob);

    @Relation("HAS_PARENT_LAYER")
    DockerLayerDescriptor getParentLayer();

    void setParentLayer(DockerLayerDescriptor parentLayerDescriptor);
}
