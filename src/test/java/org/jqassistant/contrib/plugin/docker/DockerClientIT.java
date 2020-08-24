package org.jqassistant.contrib.plugin.docker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.jqassistant.contrib.plugin.docker.rest.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.net.URI;

public class DockerClientIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerClientIT.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void readRegistry() throws JsonProcessingException {
        DefaultClientConfig clientConfig = new DefaultClientConfig(ManifestMessageBodyReader.class);
        Client client = Client.create(clientConfig);
        String registry = "http://localhost:5000";
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

                ClientResponse clientResponse = resource.uri(manifestUri).get(ClientResponse.class);
                Manifest manifest = clientResponse.getEntity(Manifest.class);
                LOGGER.info("--> {}: {}", manifest.getName(), clientResponse.getHeaders().getFirst("Docker-Content-Digest"));
                for (FileSystemLayer fsLayer : manifest.getFsLayers()) {
                    LOGGER.info("    {}", fsLayer.getBlobSum());
                }
                for (History history : manifest.getHistory()) {
                    V1Compatibility v1Compatibility = objectMapper.readValue(history.getV1Compatibility(), V1Compatibility.class);
                    LOGGER.info("    {}", v1Compatibility);
                }

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
