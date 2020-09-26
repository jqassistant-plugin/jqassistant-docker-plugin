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
    List<DockerRepositoryDescriptor> getRepositories();

    @ResultOf
    @Cypher("MATCH (registry:Docker:Registry) WHERE id(registry)=$this MERGE (registry)-[:CONTAINS_REPOSITORY]->(repository:Docker:Repository{name:$name}) RETURN repository")
    DockerRepositoryDescriptor resolveRepository(@Parameter("name") String name);

    @Relation("CONTAINS_BLOB")
    List<DockerBlobDescriptor> getBlobs();

    @Relation("CONTAINS_IMAGE")
    List<DockerImageDescriptor> getImages();

    @ResultOf
    @Cypher("MATCH (registry:Docker:Registry) WHERE id(registry)=$this " + "MERGE (registry)-[:CONTAINS_BLOB]->(blob:Docker:Blob{digest:$digest}) "
            + "ON CREATE SET blob.size=$size SET blob.mediaType=$mediaType RETURN blob")
    DockerBlobDescriptor resolveBlob(@Parameter("digest") String digest, @Parameter("mediaType") String mediaType, @Parameter("size") long size);

    @ResultOf
    @Cypher("MATCH (registry:Docker:Registry) WHERE id(registry)=$this MERGE (registry)-[:CONTAINS_IMAGE]->(image:Docker:Image{digest:$digest}) RETURN image")
    DockerImageDescriptor resolveImage(@Parameter("digest") String digest);

}
