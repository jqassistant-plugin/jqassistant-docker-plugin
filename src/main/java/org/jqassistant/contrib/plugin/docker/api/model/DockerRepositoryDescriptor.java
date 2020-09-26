package org.jqassistant.contrib.plugin.docker.api.model;

import java.util.List;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.api.annotation.ResultOf;
import com.buschmais.xo.api.annotation.ResultOf.Parameter;
import com.buschmais.xo.neo4j.api.annotation.Cypher;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Repository")
public interface DockerRepositoryDescriptor extends DockerDescriptor, NamedDescriptor {

	@Relation("CONTAINS_TAG")
	List<DockerTagDescriptor> getTags();

	@Relation("CONTAINS_BLOB")
	List<DockerBlobDescriptor> getBlobs();

	@Relation("CONTAINS_IMAGE")
	List<DockerImageDescriptor> getImages();

	@ResultOf
	@Cypher("MATCH (repository:Docker:Repository) WHERE id(repository)=$this MERGE (repository)-[:CONTAINS_TAG]->(tag:Docker:Tag{name:$name}) RETURN tag")
	DockerTagDescriptor resolveTag(@Parameter("name") String name);

	@ResultOf
	@Cypher("MATCH (repository:Docker:Repository) WHERE id(repository)=$this "
			+ "MERGE (repository)-[:CONTAINS_BLOB]->(blob:Docker:Blob{digest:$digest}) "
			+ "ON CREATE SET blob.size=$size SET blob.mediaType=$mediaType RETURN blob")
	DockerBlobDescriptor resolveBlob(@Parameter("digest") String digest, @Parameter("mediaType") String mediaType,
			@Parameter("size") long size);

	@ResultOf
	@Cypher("MATCH (repository:Docker:Repository) WHERE id(repository)=$this MERGE (repository)-[:CONTAINS_IMAGE]->(image:Docker:Image{digest:$digest}) RETURN image")
	DockerImageDescriptor resolveImage(@Parameter("digest") String digest);

}
