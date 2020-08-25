package org.jqassistant.contrib.plugin.docker.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.api.annotation.ResultOf;
import com.buschmais.xo.api.annotation.ResultOf.Parameter;
import com.buschmais.xo.neo4j.api.annotation.Cypher;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Label("Repository")
public interface DockerRepositoryDescriptor extends DockerDescriptor, NamedDescriptor {

	@ResultOf
	@Cypher("MATCH (repository:Docker:Repository) WHERE id(repository)=$this MERGE (repository)-[:CONTAINS_TAG]->(tag:Docker:Tag{name:$name}) RETURN tag")
	DockerTagDescriptor resolveTag(@Parameter("name") String name);

}
