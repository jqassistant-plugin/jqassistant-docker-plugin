package org.jqassistant.contrib.plugin.docker.api.model;

import java.util.List;

import com.buschmais.xo.neo4j.api.annotation.Indexed;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Label("Blob")
public interface DockerBlobDescriptor extends DockerDescriptor {

	long getSize();

	void setSize(long size);

	@Indexed
	String getDigest();

	void setDigest(String digest);

	List<ManifestContainsLayerDescriptor> getContainsBlobs();

}
