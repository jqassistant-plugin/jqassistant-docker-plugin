package org.jqassistant.contrib.plugin.docker.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

import com.github.dockerjava.api.DockerClient;

import lombok.extern.slf4j.Slf4j;
import org.jqassistant.contrib.plugin.docker.api.model.*;
import org.jqassistant.contrib.plugin.docker.api.scope.DockerScope;
import org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static java.util.Collections.emptyMap;
import static java.util.Collections.sort;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Testcontainers
class DockerRegistryScannerPluginIT extends AbstractPluginIT {

    @Container
    public GenericContainer registry = new GenericContainer("registry:latest").withExposedPorts(5000);

    private DockerClient dockerClient;

    private String repositoryUrl;

    @BeforeEach
    void setup() {
        this.dockerClient = registry.getDockerClient();
        this.repositoryUrl = registry.getHost() + ":" + registry.getFirstMappedPort();
    }

    @Test
    void scanRepositoryWithSingleTag() throws MalformedURLException, ExecutionException, InterruptedException {
        createAndPushTestImage("Earth", "test-repository", "latest");
        DockerRegistryDescriptor registry = scanRegistry();

        store.beginTransaction();
        assertThat(registry.getUrl()).isEqualTo("http://" + repositoryUrl);
        List<DockerRepositoryDescriptor> repositories = registry.getRepositories();
        assertThat(repositories.size()).isEqualTo(1);
        DockerRepositoryDescriptor repository = repositories.get(0);
        assertThat(repository.getName()).isEqualTo("test-repository");
        List<DockerTagDescriptor> tags = repository.getTags();
        assertThat(tags.size()).isEqualTo(1);
        DockerTagDescriptor tagDescriptor = tags.get(0);
        assertThat(tagDescriptor.getName()).isEqualTo("latest");
        verifyManifest(tagDescriptor.getManifest());

        List<DockerBlobDescriptor> blobs = registry.getBlobs();
        assertThat(blobs.size()).isEqualTo(2);
        assertThat(blobs).allMatch(blob -> blob.getDigest().startsWith("sha256:"));
        assertThat(blobs).allMatch(blob -> blob.getMediaType().equals("application/vnd.docker.image.rootfs.diff.tar.gzip"));
        assertThat(blobs).allMatch(blob -> blob.getSize() > 0);
        assertThat(blobs).allMatch(blob -> blob.getRegistry().equals(registry));

        List<DockerImageDescriptor> images = registry.getImages();
        assertThat(images.size()).isEqualTo(1);
        DockerImageDescriptor image = images.get(0);
        assertThat(image.getDigest()).startsWith("sha256:");
        assertThat(image.getRegistry()).isEqualTo(registry);

        store.commitTransaction();
    }

    @Test
    void scanDifferentRepositoriesWithSharedBlob() throws MalformedURLException, ExecutionException, InterruptedException {
        createAndPushTestImage("Earth", "test-repository1", "1.0");
        createAndPushTestImage("Earth", "test-repository2", "1.0");
        DockerRegistryDescriptor registry = scanRegistry();

        store.beginTransaction();
        Map<String, DockerRepositoryDescriptor> repositories = registry.getRepositories().stream()
                .collect(toMap(repository -> repository.getName(), repository -> repository));
        assertThat(repositories.size()).isEqualTo(2);

        List<DockerBlobDescriptor> registryBlobs = registry.getBlobs();
        assertThat(registryBlobs.size()).isEqualTo(3);

        verifyLayers(repositories, "test-repository1", registryBlobs);
        verifyLayers(repositories, "test-repository2", registryBlobs);

        store.commitTransaction();
    }

    private void verifyLayers(Map<String, DockerRepositoryDescriptor> repositories, String repository, List<DockerBlobDescriptor> registryBlobs) {
        List<DockerLayerDescriptor> layers = repositories.get(repository).getTags().stream().flatMap(tag -> tag.getManifest().getDeclaresLayers().stream())
                .collect(toList());
        assertThat(layers).hasSize(2);
        sort(layers, comparingInt(DockerLayerDescriptor::getIndex));

        DockerLayerDescriptor layer0 = layers.get(0);
        DockerLayerDescriptor layer1 = layers.get(1);

        assertThat(registryBlobs).contains(layer0.getBlob());
        assertThat(layer0.getParentLayer()).isNull();
        assertThat(registryBlobs).contains(layer1.getBlob());
        assertThat(layer1.getParentLayer()).isEqualTo(layer0);
    }

    @Test
    void incrementalScanNewTag() throws MalformedURLException, ExecutionException, InterruptedException {
        createAndPushTestImage("Earth", "test-repository", "1.0");
        DockerRegistryDescriptor registryDescriptor1 = scanRegistry();
        store.beginTransaction();
        DockerManifestDescriptor manifest1 = registryDescriptor1.getRepositories().get(0).getTags().get(0).getManifest();
        store.commitTransaction();

        createAndPushTestImage("Mars", "test-repository", "2.0");
        DockerRegistryDescriptor registryDescriptor2 = scanRegistry();
        assertThat(registryDescriptor1).isEqualTo(registryDescriptor2);

        store.beginTransaction();
        List<DockerTagDescriptor> tags = registryDescriptor2.getRepositories().get(0).getTags();
        assertThat(tags.size()).isEqualTo(2);
        Map<String, DockerManifestDescriptor> manifestsByTag = tags.stream().collect(toMap(tag -> tag.getName(), tag -> tag.getManifest()));
        assertThat(manifestsByTag).hasSize(2);
        assertThat(manifestsByTag.get("1.0")).isEqualTo(manifest1);
        DockerManifestDescriptor manifest2 = manifestsByTag.get("2.0");
        assertThat(manifest1).isNotEqualTo(manifest2);
        store.commitTransaction();
    }

    @Test
    void incrementalScanKeepTag() throws MalformedURLException, ExecutionException, InterruptedException {
        createAndPushTestImage("Earth", "test-repository", "latest");
        DockerRegistryDescriptor registryDescriptor1 = scanRegistry();
        store.beginTransaction();
        DockerManifestDescriptor manifest1 = registryDescriptor1.getRepositories().get(0).getTags().get(0).getManifest();
        store.commitTransaction();

        createAndPushTestImage("Mars", "test-repository", "latest");
        DockerRegistryDescriptor registryDescriptor2 = scanRegistry();
        assertThat(registryDescriptor1).isEqualTo(registryDescriptor2);

        store.beginTransaction();
        List<DockerTagDescriptor> tags = registryDescriptor2.getRepositories().get(0).getTags();
        assertThat(tags.size()).isEqualTo(1);
        DockerTagDescriptor tagDescriptor = tags.get(0);
        assertThat(tagDescriptor.getName()).isEqualTo("latest");
        DockerManifestDescriptor manifest2 = tagDescriptor.getManifest();
        assertThat(manifest1).isEqualTo(manifest2);
        store.commitTransaction();
    }

    @Test
    void incrementalScanUpdateTag() throws MalformedURLException, ExecutionException, InterruptedException {
        createAndPushTestImage("Earth", "test-repository", "latest");
        DockerRegistryDescriptor registryDescriptor1 = scanRegistry();
        store.beginTransaction();
        DockerManifestDescriptor manifest1 = registryDescriptor1.getRepositories().get(0).getTags().get(0).getManifest();
        store.commitTransaction();

        createAndPushTestImage("Mars", "test-repository", "latest");
        Map<String, Object> properties = new HashMap<>();
        properties.put("docker.repository.updateExistingTags", true);
        DockerRegistryDescriptor registryDescriptor2 = scanRegistry(properties);
        assertThat(registryDescriptor1).isEqualTo(registryDescriptor2);

        store.beginTransaction();
        List<DockerTagDescriptor> tags = registryDescriptor2.getRepositories().get(0).getTags();
        assertThat(tags.size()).isEqualTo(1);
        DockerTagDescriptor tagDescriptor = tags.get(0);
        assertThat(tagDescriptor.getName()).isEqualTo("latest");
        DockerManifestDescriptor manifest2 = tagDescriptor.getManifest();
        assertThat(manifest1).isNotEqualTo(manifest2);
        store.commitTransaction();
    }

    private void createAndPushTestImage(String value, String repository, String tag) throws InterruptedException, ExecutionException {
        ImageFromDockerfile image = new ImageFromDockerfile(repository + ":" + tag, false).withDockerfileFromBuilder(
                builder -> builder.from("alpine:3.2").run("echo", value, ">", "hello.txt").user("helloworld").workDir("/home/helloworld")
                        .cmd("echo", "Hello", value).expose(80, 8080).volume("/data", "/log").label("label1", "value1").label("label2", "value2").build());
        String imageId = image.get();
        dockerClient.tagImageCmd(repository + ":" + tag, repositoryUrl + "/" + repository, tag).exec();
        dockerClient.removeImageCmd(imageId).exec();
        String repositoryImageId = repositoryUrl + "/" + repository + ":" + tag;
        dockerClient.pushImageCmd(repositoryImageId).start().awaitCompletion();
        dockerClient.removeImageCmd(repositoryImageId).exec();
    }

    private DockerRegistryDescriptor scanRegistry() throws MalformedURLException {
        return scanRegistry(emptyMap());
    }

    private DockerRegistryDescriptor scanRegistry(Map<String, Object> properties) throws MalformedURLException {
        String url = "http://" + repositoryUrl;
        return getScanner(properties).scan(new URL(url), url, DockerScope.REGISTRY);
    }

    private void verifyManifest(DockerManifestDescriptor manifestDescriptor) {
        assertThat(manifestDescriptor).isNotNull();
        assertThat(manifestDescriptor.getDigest()).startsWith("sha256:");
        assertThat(manifestDescriptor.getMediaType()).isEqualTo(Manifest.MEDIA_TYPE);
        assertThat(manifestDescriptor.getSize()).isPositive();
        assertThat(manifestDescriptor.getArchitecture()).isNotBlank();
        assertThat(manifestDescriptor.getCreated()).isPositive();
        assertThat(manifestDescriptor.getDigest()).isNotBlank().startsWith("sha256:");
        assertThat(manifestDescriptor.getDockerVersion()).isNotBlank();
        assertThat(manifestDescriptor.getOs()).isEqualTo("linux");
        verifyDockerConfig(manifestDescriptor.getDockerConfig());
    }

    private void verifyDockerConfig(DockerConfigDescriptor dockerConfig) {
        assertThat(dockerConfig).isNotNull();
        assertThat(dockerConfig.isArgsEscaped()).isNull();
        assertThat(dockerConfig.isAttachStderr()).isFalse();
        assertThat(dockerConfig.isAttachStdin()).isFalse();
        assertThat(dockerConfig.isAttachStdout()).isFalse();
        assertThat(dockerConfig.getCmd()).isEqualTo(new String[] { "echo", "Hello", "Earth" });
        assertThat(dockerConfig.getDomainName()).isEmpty();
        String[] env = dockerConfig.getEnv();
        assertThat(env).hasSize(1);
        assertThat(env[0]).startsWith("PATH");
        assertThat(dockerConfig.getExposedPorts()).isEqualTo(new String[] { "80/tcp", "8080/tcp" });
        Map<String, String> labels = dockerConfig.getLabels().stream().collect(toMap(label -> label.getName(), label -> label.getValue()));
        assertThat(labels).containsEntry("label1", "value1").containsEntry("label2", "value2");
        assertThat(dockerConfig.getHostName()).isEmpty();
        assertThat(dockerConfig.isOpenStdin()).isFalse();
        assertThat(dockerConfig.isStdinOnce()).isFalse();
        assertThat(dockerConfig.isTty()).isFalse();
        assertThat(dockerConfig.getUser()).isEqualTo("helloworld");
        assertThat(dockerConfig.getWorkingDir()).isEqualTo("/home/helloworld");
        assertThat(dockerConfig.getVolumes()).isEqualTo(new String[] { "/data", "/log" });
    }

}
