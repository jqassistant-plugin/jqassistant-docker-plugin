package org.jqassistant.contrib.plugin.docker.api.model;

import java.util.List;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Manifest")
public interface DockerManifestDescriptor extends DockerBlobDescriptor {

    long getCreated();

    void setCreated(long toEpochMilli);

    String getDockerVersion();

    void setDockerVersion(String dockerVersion);

    String getOs();

    void setOs(String os);

    String getArchitecture();

    void setArchitecture(String architecture);

    List<DeclaresLayerDescriptor> getDeclaresLayers();

    @Relation("HAS_CONFIG")
    DockerConfigDescriptor getDockerConfig();

    void setDockerConfig(DockerConfigDescriptor dockerConfig);

    @Relation("HAS_CONTAINER_CONFIG")
    DockerContainerConfigDescriptor getDockerContainerConfig();

    void setDockerContainerConfig(DockerContainerConfigDescriptor dockerContainerConfig);
}
