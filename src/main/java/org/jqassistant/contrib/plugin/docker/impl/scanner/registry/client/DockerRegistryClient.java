package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client;

import static org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest.HEADER_DOCKER_CONTENT_DIGEST;
import static org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest.MEDIA_TYPE;

import java.net.URI;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Catalog;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.ManifestMessageBodyReader;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Tags;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class DockerRegistryClient {

	private final WebResource resource;

	public DockerRegistryClient(URI uri) {
		DefaultClientConfig clientConfig = new DefaultClientConfig(ObjectMapperProvider.class,
				ManifestMessageBodyReader.class);
		Client client = Client.create(clientConfig);
		this.resource = client.resource(uri).path("v2");
	}

	public Tags getTags(String repository) {
		URI tagsUri = resource.getUriBuilder().path(repository).path("tags").path("list").build();
		return get(resource, tagsUri, Tags.class, MediaType.APPLICATION_JSON);
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
			return Optional.of(manifest);
		}
		return Optional.empty();
	}

	private <T> T get(WebResource resource, URI uri, Class<T> responseType, String... mediaTypes) {
		return resource.uri(uri).accept(mediaTypes).get(responseType);
	}
}
