package org.jqassistant.contrib.plugin.docker.api.model;

import com.buschmais.xo.api.annotation.ResultOf;
import com.buschmais.xo.api.annotation.ResultOf.Parameter;
import com.buschmais.xo.neo4j.api.annotation.Cypher;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Label("Registry")
public interface DockerRegistryDescriptor extends DockerDescriptor {

	String getUrl();

	void setUrl(String url);

	@ResultOf
	@Cypher("MATCH (registry:Docker:Registry) WHERE id(registry)=$this MERGE (registry)-[:CONTAINS_REPOSITORY]->(repository:Docker:Repository{name:$name}) RETURN repository")
	DockerRepositoryDescriptor resolveRepository(@Parameter("name") String name);

	@ResultOf
	@Cypher("MATCH (registry:Docker:Registry) WHERE id(registry)=$this MERGE (registry)-[:CONTAINS_BLOB]->(blob:Docker:Blob{digest:$digest}) ON CREATE SET blob.size=$size RETURN blob")
	DockerBlobDescriptor resolveBlob(@Parameter("digest") String name, @Parameter("size") long size);
}
