package org.jqassistant.contrib.plugin.docker.api.model;

import java.util.List;

import com.buschmais.xo.neo4j.api.annotation.Label;

@Label("Manifest")
public interface DockerManifestDescriptor extends DockerDescriptor {

	String getDigest();

	void setDigest(String digest);

	long getCreated();

	void setCreated(long toEpochMilli);

	String getDockerVersion();

	void setDockerVersion(String dockerVersion);

	String getOs();

	void setOs(String os);

	String getArchitecture();

	void setArchitecture(String architecture);

	List<ManifestContainsLayerDescriptor> getContainsBlobs();

}
