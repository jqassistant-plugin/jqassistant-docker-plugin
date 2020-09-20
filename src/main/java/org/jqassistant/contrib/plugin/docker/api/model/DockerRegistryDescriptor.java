package org.jqassistant.contrib.plugin.docker.api.model;

import java.util.List;

import com.buschmais.xo.api.annotation.ResultOf;
import com.buschmais.xo.api.annotation.ResultOf.Parameter;
import com.buschmais.xo.neo4j.api.annotation.Cypher;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Registry")
public interface DockerRegistryDescriptor extends DockerDescriptor {

    String getUrl();

    void setUrl(String url);

    @Relation("CONTAINS_REPOSITORY")
    List<DockerRepositoryDescriptor> getContainsRepositories();

    @ResultOf
    @Cypher("MATCH (registry:Docker:Registry) WHERE id(registry)=$this MERGE (registry)-[:CONTAINS_REPOSITORY]->(repository:Docker:Repository{name:$name}) RETURN repository")
    DockerRepositoryDescriptor resolveRepository(@Parameter("name") String name);

}
