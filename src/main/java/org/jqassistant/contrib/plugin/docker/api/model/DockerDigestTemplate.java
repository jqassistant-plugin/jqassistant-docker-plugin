package org.jqassistant.contrib.plugin.docker.api.model;

import com.buschmais.xo.neo4j.api.annotation.Indexed;

public interface DockerDigestTemplate {

    @Indexed
    String getDigest();

    void setDigest(String digest);

}
