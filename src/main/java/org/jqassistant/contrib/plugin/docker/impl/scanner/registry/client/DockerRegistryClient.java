package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest.HEADER_DOCKER_CONTENT_DIGEST;
import static org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest.MEDIA_TYPE;

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.client.apache.ApacheHttpClientHandler;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.*;

@Slf4j
public class DockerRegistryClient implements AutoCloseable {

    public static final String USER_AGENT = "jQAssistant/1.x";

    private static final int HTTP_NOT_FOUND = 404;

    private final WebResource resource;

    private SimpleHttpConnectionManager connectionManager;

    public DockerRegistryClient(URI uri) {
        this.connectionManager = new SimpleHttpConnectionManager();
        connectionManager.getParams().setConnectionTimeout(5000);
        connectionManager.getParams().setSoTimeout(60000);
        connectionManager.getParams().setDefaultMaxConnectionsPerHost(1);
        HttpClient httpClient = new HttpClient(connectionManager);
        ClientConfig config = new DefaultApacheHttpClientConfig() {
            @Override
            public Set<Class<?>> getClasses() {
                return new HashSet<>(asList(ObjectMapperProvider.class, ManifestMessageBodyReader.class, ManifestConfigMessageBodyReader.class));
            }
        };
        ClientHandler clientHandler = new ApacheHttpClientHandler(httpClient, config);
        Client client = new Client(clientHandler, config);
        this.resource = client.resource(uri).path("v2");
    }

    @Override
    public void close() {
        connectionManager.shutdown();
    }

    public RepositoryTags getRepositoryTags(String repository) {
        URI tagsUri = resource.getUriBuilder().path(repository).path("tags").path("list").build();
        return get(resource, tagsUri, RepositoryTags.class, MediaType.APPLICATION_JSON);
    }

    public Catalog getCatalog() {
        URI catalogUri = resource.getUriBuilder().path("_catalog").build();
        return get(resource, catalogUri, Catalog.class, MediaType.APPLICATION_JSON);
    }

    public Optional<Manifest> getManifest(String repository, String tagOrDigest) {
        URI manifestUri = resource.getUriBuilder().path(repository).path("manifests").path(tagOrDigest).build();
        ClientResponse clientResponse = get(resource, manifestUri, ClientResponse.class, MEDIA_TYPE);
        if (MEDIA_TYPE.equals(clientResponse.getHeaders().getFirst("Content-Type"))) {
            Manifest manifest = clientResponse.getEntity(Manifest.class);
            manifest.setDigest(clientResponse.getHeaders().getFirst(HEADER_DOCKER_CONTENT_DIGEST));
            manifest.setMediaType(clientResponse.getHeaders().getFirst("Content-Type"));
            manifest.setSize(Long.valueOf(clientResponse.getHeaders().getFirst("Content-Length")));
            return Optional.of(manifest);
        }
        return Optional.empty();
    }

    public <T> Optional<T> getBlob(String repository, String digest, Class<T> type, String mediaType) {
        URI blobUri = resource.getUriBuilder().path(repository).path("blobs").path(digest).build();
        return getOptional(resource, blobUri, type, mediaType);
    }

    private <T> Optional<T> getOptional(WebResource resource, URI uri, Class<T> responseType, String mediaType) {
        try {
            return Optional.of(get(resource, uri, responseType, mediaType));
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getStatus() == HTTP_NOT_FOUND) {
                log.info("Cannot find resource with URI {}.", uri);
                return Optional.empty();
            }
            throw e;
        }
    }

    private <T> T get(WebResource resource, URI uri, Class<T> responseType, String mediaType) {
        RetryPolicy<T> retryPolicy = new RetryPolicy<T>().handleIf(e -> {
            if (e instanceof UniformInterfaceException) {
                UniformInterfaceException interfaceException = (UniformInterfaceException) e;
                Response.Status.Family family = interfaceException.getResponse().getStatusInfo().getFamily();
                return family != CLIENT_ERROR;
            }
            return true;
        }).withDelay(ofSeconds(1)).withMaxRetries(3);
        return Failsafe.with(retryPolicy).get(() -> resource.uri(uri).accept(mediaType).header(HttpHeaders.USER_AGENT, USER_AGENT).get(responseType));
    }
}
