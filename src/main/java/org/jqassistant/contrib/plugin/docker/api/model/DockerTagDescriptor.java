package org.jqassistant.contrib.plugin.docker.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Label("Tag")
public interface DockerTagDescriptor extends DockerDescriptor, NamedDescriptor {

	DockerManifestDescriptor getManifest();

	void setManifest(DockerManifestDescriptor manifest);

}
