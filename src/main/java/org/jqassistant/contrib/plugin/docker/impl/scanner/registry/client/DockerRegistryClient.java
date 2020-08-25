package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client;

import java.net.URI;

import javax.ws.rs.core.MediaType;

import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Catalog;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class DockerRegistryClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(DockerRegistryClient.class);

	private final WebResource resource;

	public DockerRegistryClient(URI uri) {
		DefaultClientConfig clientConfig = new DefaultClientConfig(ObjectMapperProvider.class,
				ManifestMessageBodyReader.class);
		Client client = Client.create(clientConfig);
		this.resource = client.resource(uri).path("v2");
	}

	public Tags getTags(String repository) {
		URI tagsUri = resource.getUriBuilder().path(repository).path("tags").path("list").build();
		Tags tags = get(resource, tagsUri, Tags.class);
		return tags;
	}

	public Catalog getCatalog() {
		URI catalogUri = resource.getUriBuilder().path("_catalog").build();
		return get(resource, catalogUri, Catalog.class);
	}

	public Manifest getManifest(String repository, String tagOrDigest) {
		URI manifestUri = resource.getUriBuilder().path(repository).path("manifests").path(tagOrDigest).build();

		ClientResponse clientResponse = get(resource, manifestUri, ClientResponse.class);
		Manifest manifest = clientResponse.getEntity(Manifest.class);
		manifest.setDigest(clientResponse.getHeaders().getFirst("Docker-Content-Digest"));
		return manifest;
	}

	private <T> T get(WebResource resource, URI uri, Class<T> responseType) {
		return resource.uri(uri).accept(MediaType.APPLICATION_JSON_TYPE).get(responseType);
	}
}
