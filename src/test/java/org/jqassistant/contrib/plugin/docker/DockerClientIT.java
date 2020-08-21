package org.jqassistant.contrib.plugin.docker;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.jqassistant.contrib.plugin.docker.rest.Catalog;
import org.jqassistant.contrib.plugin.docker.rest.Manifest;
import org.jqassistant.contrib.plugin.docker.rest.ManifestMessageBodyReader;
import org.jqassistant.contrib.plugin.docker.rest.Tags;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.net.URI;

public class DockerClientIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerClientIT.class);

    @Test
    public void readRegistry() {
        DefaultClientConfig clientConfig = new DefaultClientConfig(ManifestMessageBodyReader.class);
        Client client = Client.create(clientConfig);
        String registry = "http://172.19.245.131:5000";
        WebResource resource = client.resource(registry)
            .path("v2");

        URI catalogUri = resource.getUriBuilder()
            .path("_catalog")
            .build();
        Catalog catalog = get(resource, catalogUri, Catalog.class);

        for (String repository : catalog.getRepositories()) {
            URI tagsUri = resource.getUriBuilder()
                .path(repository)
                .path("tags/list")
                .build();
            Tags tags = get(resource, tagsUri, Tags.class);

            for (String tag : tags.getTags()) {
                URI manifestUri = resource.getUriBuilder()
                    .path(repository)
                    .path("manifests")
                    .path(tag)
                    .build();
                Manifest manifest = get(resource, manifestUri, Manifest.class);
                LOGGER.info("--> {}.", manifest);
            }
        }
    }

    private <T> T get(WebResource resource, URI uri, Class<T> responseType) {
        T response = resource.uri(uri)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .get(responseType);
        return response;
    }
}
