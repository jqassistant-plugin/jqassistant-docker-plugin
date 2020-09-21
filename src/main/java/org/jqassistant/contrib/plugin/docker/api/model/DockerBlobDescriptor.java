package org.jqassistant.contrib.plugin.docker.api.model;

import java.util.List;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import com.buschmais.xo.neo4j.api.annotation.Relation.Incoming;

@Label(value = "Blob", usingIndexedPropertyOf = DockerDigestTemplate.class)
public interface DockerBlobDescriptor extends DockerDescriptor, DockerDigestTemplate {

	long getSize();

	void setSize(long size);

	List<ManifestContainsLayerDescriptor> getContainsBlobs();

	@Incoming
	@Relation("CONTAINS_BLOB")
	DockerRepositoryDescriptor getRepository();

	void setRepository(DockerRepositoryDescriptor dockerRepositoryDescriptor);

}
