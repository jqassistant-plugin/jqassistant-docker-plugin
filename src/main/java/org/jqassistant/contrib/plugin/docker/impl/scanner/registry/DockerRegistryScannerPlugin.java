package org.jqassistant.contrib.plugin.docker.impl.scanner.registry;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FilePatternMatcher;
import com.buschmais.xo.api.Query;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.contrib.plugin.docker.api.model.*;
import org.jqassistant.contrib.plugin.docker.api.scope.DockerScope;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.DockerRegistryClient;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.BlobReference;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Catalog;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest;

@Slf4j
public class DockerRegistryScannerPlugin extends AbstractScannerPlugin<URL, DockerRegistryDescriptor> {

    private FilePatternMatcher filePatternMatcher;

    @Override
    protected void configure() {
        String repositoryInclude = getStringProperty("docker.repository.include", "**");
        String repositoryExclude = getStringProperty("docker.repository.exclude", null);
        filePatternMatcher = FilePatternMatcher.builder().include(repositoryInclude).exclude(repositoryExclude).build();
    }

    @Override
    public boolean accepts(URL url, String s, Scope scope) {
        return DockerScope.REGISTRY == scope;
    }

    @Override
    public DockerRegistryDescriptor scan(URL url, String s, Scope scope, Scanner scanner) throws IOException {
        log.info("Starting scan of Docker registry '{}'.", url);
        ScannerContext context = scanner.getContext();
        DockerRegistryClient registryClient = getRegistryClient(url);
        DockerRegistryDescriptor registryDescriptor = resolveRegistryDescriptor(url, context);
        LoadingCache<BlobReference, DockerBlobDescriptor> blobCache = createCache(
                blobReference -> registryDescriptor.resolveBlob(blobReference.getDigest(), blobReference.getMediaType(), blobReference.getSize()));
        LoadingCache<String, DockerImageDescriptor> imageCache = createCache(image -> registryDescriptor.resolveImage(image));
        Catalog catalog = registryClient.getCatalog();
        log.info("Registry contains {} repositories.", catalog.getRepositories().size());
        for (String repository : catalog.getRepositories()) {
            if (filePatternMatcher.accepts(repository)) {
                scanRepository(repository, registryDescriptor, blobCache, imageCache, context, registryClient);
            }
        }
        log.info("Finished scan of Docker registry '{}'.", url);
        return registryDescriptor;
    }

    private void scanRepository(String repository, DockerRegistryDescriptor registryDescriptor,
            LoadingCache<BlobReference, DockerBlobDescriptor> blobDescriptorCache, LoadingCache<String, DockerImageDescriptor> imageDescriptorCache,
            ScannerContext context, DockerRegistryClient registryClient) {
        log.info("Scanning repository '{}'.", repository);
        List<String> tags = registryClient.getRepositoryTags(repository).getTags();
        log.info("Repository '{}' contains {} tags.", repository, tags.size());
        DockerRepositoryDescriptor repositoryDescriptor = registryDescriptor.resolveRepository(repository);
        for (String tag : tags) {
            log.info("Processing '{}:{}'.", repository, tag);
            DockerTagDescriptor dockerTagDescriptor = repositoryDescriptor.resolveTag(tag);
            registryClient.getManifest(repository, tag).ifPresent(manifest -> dockerTagDescriptor
                    .setManifest(scanManifest(repository, manifest, imageDescriptorCache, blobDescriptorCache, registryClient, context)));
        }
    }

    private DockerManifestDescriptor scanManifest(String repository, Manifest manifest, LoadingCache<String, DockerImageDescriptor> imageDescriptorCache,
            LoadingCache<BlobReference, DockerBlobDescriptor> blobDescriptorCache, DockerRegistryClient registryClient, ScannerContext context) {
        Map<String, Object> params = new HashMap<>();
        params.put("digest", manifest.getDigest());
        Query.Result<Query.Result.CompositeRowObject> result = context.getStore()
                .executeQuery("MATCH (manifest:Docker:Manifest{digest:$digest}) RETURN manifest", params);
        if (result.hasResult()) {
            // Manifest has already been scanned, skip.
            return result.getSingleResult().get("manifest", DockerManifestDescriptor.class);
        }
        DockerManifestDescriptor manifestDescriptor = context.getStore().create(DockerManifestDescriptor.class);
        manifestDescriptor.setDigest(manifest.getDigest());
        manifestDescriptor.setMediaType(manifest.getMediaType());
        manifestDescriptor.setSize(manifest.getSize());
        manifestDescriptor.setMediaType(manifest.getMediaType());
        scanManifestConfig(repository, manifest.getConfig(), manifestDescriptor, imageDescriptorCache, registryClient, context);
        scanLayers(manifest.getLayers(), manifestDescriptor, blobDescriptorCache, context);

        return manifestDescriptor;
    }

    private void scanManifestConfig(String repository, BlobReference configBlobReference, DockerManifestDescriptor manifestDescriptor,
            LoadingCache<String, DockerImageDescriptor> imageDescriptorCache, DockerRegistryClient registryClient, ScannerContext context) {
        registryClient.getBlob(repository, configBlobReference.getDigest(), Manifest.Content.class, configBlobReference.getMediaType()).ifPresent(content -> {
            manifestDescriptor.setArchitecture(content.getArchitecture());
            manifestDescriptor.setOs(content.getOs());
            manifestDescriptor.setDockerVersion(content.getDockerVersion());
            manifestDescriptor.setCreated(content.getCreated().toInstant().toEpochMilli());
            manifestDescriptor.setDockerConfig(scanConfig(content.getConfig(), DockerConfigDescriptor.class, imageDescriptorCache, context));
            manifestDescriptor
                    .setDockerContainerConfig(scanConfig(content.getContainerConfig(), DockerContainerConfigDescriptor.class, imageDescriptorCache, context));
        });
    }

    private <D extends DockerConfigDescriptor> D scanConfig(Manifest.Config config, Class<D> configDescriptorType,
            LoadingCache<String, DockerImageDescriptor> imageDescriptorCache, ScannerContext context) {
        if (config == null) {
            return null;
        }
        D configDescriptor = context.getStore().create(configDescriptorType);
        configDescriptor.setArgsEscaped(config.getArgsEscaped());
        configDescriptor.setAttachStderr(config.getAttachStderr());
        configDescriptor.setAttachStdin(config.getAttachStdin());
        configDescriptor.setAttachStdout(config.getAttachStdout());
        configDescriptor.setCmd(config.getCmd());
        configDescriptor.setDomainName(config.getDomainName());
        configDescriptor.setEntryPoint(config.getEntrypoint());
        configDescriptor.setEnv(config.getEnv());
        if (config.getExposedPorts() != null) {
            configDescriptor.setExposedPorts(config.getExposedPorts().keySet().toArray(new String[0]));
        }
        configDescriptor.setHostName(config.getHostName());
        String imageDigest = config.getImage();
        if (imageDigest != null) {
            configDescriptor.setImage(imageDescriptorCache.get(imageDigest));
        }
        if (config.getLabels() != null) {
            for (Map.Entry<String, String> entry : config.getLabels().entrySet()) {
                DockerLabelDescriptor labelDescriptor = context.getStore().create(DockerLabelDescriptor.class);
                labelDescriptor.setName(entry.getKey());
                labelDescriptor.setValue(entry.getValue());
                configDescriptor.getLabels().add(labelDescriptor);
            }
        }
        configDescriptor.setOnBuild(config.getOnBuild());
        configDescriptor.setOpenStdin(config.getOpenStdin());
        configDescriptor.setStdinOnce(config.getStdinOnce());
        configDescriptor.setTty(config.getTty());
        configDescriptor.setUser(config.getUser());
        if (config.getVolumes() != null) {
            configDescriptor.setVolumes(config.getVolumes().keySet().toArray(new String[0]));
        }
        configDescriptor.setWorkingDir(config.getWorkingDir());
        return configDescriptor;
    }

    private void scanLayers(List<BlobReference> layers, DockerManifestDescriptor manifestDescriptor,
            LoadingCache<BlobReference, DockerBlobDescriptor> blobDescriptorCache, ScannerContext context) {
        int index = 0;
        DockerLayerDescriptor parentLayerDescriptor = null;
        for (BlobReference layer : layers) {
            DockerLayerDescriptor layerDescriptor = context.getStore().create(DockerLayerDescriptor.class);
            layerDescriptor.setParentLayer(parentLayerDescriptor);
            DockerBlobDescriptor blobDescriptor = blobDescriptorCache.get(layer);
            layerDescriptor.setBlob(blobDescriptor);
            layerDescriptor.setIndex(index);
            manifestDescriptor.getDeclaresLayers().add(layerDescriptor);
            parentLayerDescriptor = layerDescriptor;
            index++;
        }
    }

    private DockerRegistryDescriptor resolveRegistryDescriptor(URL url, ScannerContext context) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", url.toString());
        return context.getStore().executeQuery("MERGE (registry:Docker:Registry{url:$url}) RETURN registry", params).getSingleResult().get("registry",
                DockerRegistryDescriptor.class);
    }

    private DockerRegistryClient getRegistryClient(URL url) throws IOException {
        try {
            return new DockerRegistryClient(url.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Cannot URI from url " + url);
        }
    }

    private <K, V> LoadingCache<K, V> createCache(CacheLoader<K, V> cacheLoader) {
        return Caffeine.newBuilder().maximumSize(256).build(cacheLoader);
    }
}
