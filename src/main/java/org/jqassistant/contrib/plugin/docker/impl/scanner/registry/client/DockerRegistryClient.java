package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client;

import static java.lang.Long.parseLong;
import static java.time.Duration.ofSeconds;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest.HEADER_DOCKER_CONTENT_DIGEST;
import static org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest.MEDIA_TYPE;

import java.net.URI;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientProperties;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.*;

@Slf4j
public class DockerRegistryClient implements AutoCloseable {

    public static final String USER_AGENT = "jQAssistant/2.x";

    private static final int HTTP_NOT_FOUND = 404;

    private final WebTarget rootTarget;

    public DockerRegistryClient(URI uri) {
        Client client = ClientBuilder.newBuilder().register(JacksonJsonProvider.class).register(ObjectMapperProvider.class)
                .register(ManifestMessageBodyReader.class).register(ManifestConfigMessageBodyReader.class).build();
        client.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        client.property(ClientProperties.READ_TIMEOUT, 60000);
        this.rootTarget = client.target(uri).path("v2");
    }

    @Override
    public void close() {
    }

    public RepositoryTags getRepositoryTags(String repository) {
        return get(rootTarget.path(repository).path("tags").path("list"), RepositoryTags.class, MediaType.APPLICATION_JSON);
    }

    public Catalog getCatalog() {
        return get(rootTarget.path("_catalog"), Catalog.class, MediaType.APPLICATION_JSON);
    }

    public Optional<Manifest> getManifest(String repository, String tagOrDigest) {
        Response clientResponse = get(rootTarget.path(repository).path("manifests").path(tagOrDigest), Response.class, MEDIA_TYPE);
        if (MEDIA_TYPE.equals(clientResponse.getHeaders().getFirst("Content-Type"))) {
            Manifest manifest = clientResponse.readEntity(Manifest.class);
            manifest.setDigest(clientResponse.getHeaderString(HEADER_DOCKER_CONTENT_DIGEST));
            manifest.setMediaType(clientResponse.getHeaderString("Content-Type"));
            manifest.setSize(parseLong(clientResponse.getHeaderString("Content-Length")));
            return Optional.of(manifest);
        }
        return Optional.empty();
    }

    public <T> Optional<T> getBlob(String repository, String digest, Class<T> type, String mediaType) {
        return getOptional(rootTarget.path(repository).path("blobs").path(digest), type, mediaType);
    }

    private <T> Optional<T> getOptional(WebTarget target, Class<T> responseType, String mediaType) {
        try {
            return Optional.of(get(target, responseType, mediaType));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == HTTP_NOT_FOUND) {
                log.info("Cannot find resource with URI {}.", target.getUri());
                return Optional.empty();
            }
            throw e;
        }
    }

    private <T> T get(WebTarget target, Class<T> responseType, String mediaType) {
        RetryPolicy<T> retryPolicy = new RetryPolicy<T>().handleIf(e -> {
            if (e instanceof WebApplicationException) {
                WebApplicationException interfaceException = (WebApplicationException) e;
                Response.Status.Family family = interfaceException.getResponse().getStatusInfo().getFamily();
                return family != CLIENT_ERROR;
            }
            return true;
        }).withDelay(ofSeconds(1)).withMaxRetries(3);
        return Failsafe.with(retryPolicy).get(() -> target.request(mediaType).header(HttpHeaders.USER_AGENT, USER_AGENT).get(responseType));
    }
}
