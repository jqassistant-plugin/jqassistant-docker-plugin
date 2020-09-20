package org.jqassistant.contrib.plugin.docker.api.model;

import java.util.List;

import com.buschmais.xo.neo4j.api.annotation.Label;

@Label(value = "Blob", usingIndexedPropertyOf = DockerDigestTemplate.class)
public interface DockerBlobDescriptor extends DockerDescriptor, DockerDigestTemplate {

	long getSize();

	void setSize(long size);

	List<ManifestContainsLayerDescriptor> getContainsBlobs();

}
