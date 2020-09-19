package org.jqassistant.contrib.plugin.docker.rest;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.PushImageResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.contrib.plugin.docker.api.model.*;
import org.jqassistant.contrib.plugin.docker.api.scope.DockerScope;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Testcontainers
public class DockerRegistryScannerPluginIT extends AbstractPluginIT {

    public static final String JQA_TEST_IMAGE = "test-image";

    @Container
    public GenericContainer registry = new GenericContainer("registry:latest")
        .withExposedPorts(5000);

    @Test
    @TestStore(type = TestStore.Type.FILE)
    public void scanRegistry() throws MalformedURLException, ExecutionException, InterruptedException {
        DockerClient dockerClient = registry.getDockerClient();
        createImage(dockerClient, JQA_TEST_IMAGE, "latest");
        String repositoryUrl = registry.getHost() + ":" + registry.getFirstMappedPort();
        String taggedImageId = pushToRegistry(dockerClient, repositoryUrl, JQA_TEST_IMAGE, "latest");

        try {
            String url = "http://" + repositoryUrl;
            DockerRegistryDescriptor registryDescriptor = getScanner().scan(new URL(url), url, DockerScope.REGISTRY);

            store.beginTransaction();
            List<DockerRepositoryDescriptor> repositories = registryDescriptor.getContainsRepositories();
            assertThat(repositories.size()).isEqualTo(1);
            DockerRepositoryDescriptor repositoryDescriptor = repositories.get(0);
            assertThat(repositoryDescriptor.getName()).isEqualTo("test-image");
            List<DockerTagDescriptor> tags = repositoryDescriptor.getContainsTags();
            assertThat(tags.size()).isEqualTo(1);
            DockerTagDescriptor tagDescriptor = tags.get(0);
            assertThat(tagDescriptor.getName()).isEqualTo("latest");
            DockerManifestDescriptor manifestDescriptor = tagDescriptor.getManifest();
            assertThat(manifestDescriptor).isNotNull();
            assertThat(manifestDescriptor.getDigest()).isNotBlank().startsWith("sha256:");

            List<DockerBlobDescriptor> blobs = registryDescriptor.getContainsBlobs();
            assertThat(blobs.size()).isEqualTo(1);

            store.commitTransaction();
        } finally {
            dockerClient.removeImageCmd(taggedImageId).exec();
        }
    }

    private void createImage(DockerClient dockerClient, String imageId, String label) throws InterruptedException, ExecutionException {
        List<Image> images = dockerClient.listImagesCmd().exec();
        String repoTag = imageId + ":" + label;
        if (!images.stream().flatMap(image -> stream(image.getRepoTags() != null ? image.getRepoTags() : new String[0])).filter(tag -> repoTag.equals(tag)).findAny().isPresent()) {
            log.info("Local test image not found, creating it.");
            ImageFromDockerfile image = new ImageFromDockerfile(JQA_TEST_IMAGE, false)
                .withDockerfileFromBuilder(builder ->
                    builder
                        .from("alpine:3.2")
                        .cmd("echo", "Hello", "World;")
                        .expose(80, 8080)
                        .volume("/data", "/log")
                        .build());
            image.get();
        }
    }

    private String pushToRegistry(DockerClient dockerClient, String repositoryUrl, String imageId, String label) {
        dockerClient.tagImageCmd(imageId + ":" + label, repositoryUrl + "/" + imageId, label).exec();
        String repositoryImageId = repositoryUrl + "/" + imageId + ":" + label;
        dockerClient.pushImageCmd(repositoryImageId).exec(new PushImageResultCallback());
        return repositoryImageId;
    }

}
