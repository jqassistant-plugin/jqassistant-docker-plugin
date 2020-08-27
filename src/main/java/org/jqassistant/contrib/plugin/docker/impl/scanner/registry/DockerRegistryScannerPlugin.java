package org.jqassistant.contrib.plugin.docker.impl.scanner.registry;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jqassistant.contrib.plugin.docker.api.model.DockerBlobDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerManifestDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerRegistryDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerRepositoryDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerTagDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.ManifestContainsLayerDescriptor;
import org.jqassistant.contrib.plugin.docker.api.scope.DockerScope;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.DockerRegistryClient;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Catalog;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.xo.api.Query;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DockerRegistryScannerPlugin extends AbstractScannerPlugin<URL, DockerRegistryDescriptor> {

	@Override
	public boolean accepts(URL url, String s, Scope scope) {
		return DockerScope.REGISTRY == scope;
	}

	@Override
	public DockerRegistryDescriptor scan(URL url, String s, Scope scope, Scanner scanner) throws IOException {
		ScannerContext context = scanner.getContext();
		DockerRegistryClient registryClient = getRegistryClient(url);
		DockerRegistryDescriptor registryDescriptor = getRegistryDescriptor(url, context);
		log.info("Starting scan of Docker registry '{}'.", url);
		Catalog catalog = registryClient.getCatalog();
		log.info("Registry contains {} repositories.", catalog.getRepositories().size());
		for (String repository : catalog.getRepositories()) {
			Cache<String, DockerBlobDescriptor> blobDescriptorCache = Caffeine.newBuilder().maximumSize(256).build();
			DockerRepositoryDescriptor repositoryDescriptor = registryDescriptor.resolveRepository(repository);
			List<String> tags = registryClient.getTags(repository).getTags();
			log.info("Scanning repository '{}' ({} tags).", repository, tags.size());
			for (String tag : tags) {
				log.info("Processing '{}:{}'.", repository, tag);
				DockerTagDescriptor dockerTagDescriptor = repositoryDescriptor.resolveTag(tag);
				if (dockerTagDescriptor.getManifest() == null) {
					Optional<Manifest> manifest = registryClient.getManifest(repository, tag);
					manifest.ifPresent(m -> dockerTagDescriptor.setManifest(resolveManifest(repository, m,
							registryDescriptor, blobDescriptorCache, registryClient, context)));
				}
			}
		}
		log.info("Finished scan of Docker registry '{}'.", url);
		return registryDescriptor;
	}

	private DockerManifestDescriptor resolveManifest(String repository, Manifest manifest,
			DockerRegistryDescriptor registryDescriptor, Cache<String, DockerBlobDescriptor> blobDescriptorCache,
			DockerRegistryClient registryClient, ScannerContext context) {
		Map<String, Object> params = new HashMap<>();
		params.put("digest", manifest.getDigest());
		Query.Result<Query.Result.CompositeRowObject> result = context.getStore()
				.executeQuery("MATCH (manifest:Docker:Manifest{digest:$digest}) RETURN manifest", params);
		if (result.hasResult()) {
			return result.getSingleResult().get("manifest", DockerManifestDescriptor.class);
		}
		DockerManifestDescriptor manifestDescriptor = context.getStore().create(DockerManifestDescriptor.class);
		manifestDescriptor.setDigest(manifest.getDigest());
		resolveConfig(repository, manifest.getConfig(), manifestDescriptor, registryClient, context);
		resolveLayers(manifest.getLayers(), registryDescriptor, manifestDescriptor, blobDescriptorCache, context);
		return manifestDescriptor;
	}

	private void resolveConfig(String repository, Manifest.BlobReference configBlobReference,
			DockerManifestDescriptor manifestDescriptor, DockerRegistryClient registryClient, ScannerContext context) {
		Manifest.Config config = registryClient.getBlob(repository, configBlobReference.getDigest(),
				Manifest.Config.class, configBlobReference.getMediaType());
		manifestDescriptor.setArchitecture(config.getArchitecture());
		manifestDescriptor.setOs(config.getOs());
		manifestDescriptor.setDockerVersion(config.getDockerVersion());
		manifestDescriptor.setCreated(config.getCreated().toInstant().toEpochMilli());
	}

	private void resolveLayers(List<Manifest.BlobReference> layers, DockerRegistryDescriptor registryDescriptor,
			DockerManifestDescriptor manifestDescriptor, Cache<String, DockerBlobDescriptor> blobDescriptorCache,
			ScannerContext context) {
		int index = 0;
		for (Manifest.BlobReference layer : layers) {
			DockerBlobDescriptor blobDescriptor = blobDescriptorCache.get(layer.getDigest(),
					digest -> registryDescriptor.resolveBlob(layer.getDigest(), layer.getSize()));
			ManifestContainsLayerDescriptor manifestContainsLayerDescriptor = context.getStore()
					.create(manifestDescriptor, ManifestContainsLayerDescriptor.class, blobDescriptor);
			manifestContainsLayerDescriptor.setIndex(index);
			index++;
		}
	}

	private DockerRegistryDescriptor getRegistryDescriptor(URL url, ScannerContext context) {
		Map<String, Object> params = new HashMap<>();
		params.put("url", url.toString());
		DockerRegistryDescriptor registryDescriptor = context.getStore()
				.executeQuery("MERGE (registry:Docker:Registry{url:$url}) RETURN registry", params).getSingleResult()
				.get("registry", DockerRegistryDescriptor.class);
		return registryDescriptor;
	}

	private DockerRegistryClient getRegistryClient(URL url) throws IOException {
		try {
			return new DockerRegistryClient(url.toURI());
		} catch (URISyntaxException e) {
			throw new IOException("Cannot URI from url " + url);
		}
	}
}
