package org.jqassistant.contrib.plugin.docker.rest;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import com.github.dockerjava.api.DockerClient;
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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Testcontainers
public class DockerRegistryScannerPluginIT extends AbstractPluginIT {

    public static final String JQA_TEST_IMAGE = "test-image";

    @Container
    public GenericContainer registry = new GenericContainer("registry:latest").withExposedPorts(5000);

    @TestStore(type = TestStore.Type.FILE)
    @Test
    public void scanRegistry() throws MalformedURLException, ExecutionException, InterruptedException {
        DockerClient dockerClient = registry.getDockerClient();
        createImage();
        String repositoryUrl = registry.getHost() + ":" + registry.getFirstMappedPort();
        String taggedImageId = pushToRegistry(dockerClient, repositoryUrl, JQA_TEST_IMAGE, "latest");

        try {
            String url = "http://" + repositoryUrl;
            DockerRegistryDescriptor registryDescriptor = getScanner().scan(new URL(url), url, DockerScope.REGISTRY);

            store.beginTransaction();
            List<DockerRepositoryDescriptor> repositories = registryDescriptor.getContainsRepositories();
            assertThat(repositories.size()).isEqualTo(1);
            DockerRepositoryDescriptor repository = repositories.get(0);
            assertThat(repository.getName()).isEqualTo("test-image");
            List<DockerTagDescriptor> tags = repository.getTags();
            assertThat(tags.size()).isEqualTo(1);
            DockerTagDescriptor tagDescriptor = tags.get(0);
            assertThat(tagDescriptor.getName()).isEqualTo("latest");
            DockerManifestDescriptor manifestDescriptor = tagDescriptor.getManifest();
            verifyManifest(manifestDescriptor);

            List<DockerBlobDescriptor> blobs = repository.getBlobs();
            assertThat(blobs.size()).isEqualTo(2);
            assertThat(blobs).allMatch(blob -> blob.getDigest().startsWith("sha256:"));
            assertThat(blobs).allMatch(blob -> blob.getRepository() == repository);

            List<DockerImageDescriptor> images = repository.getImages();
            assertThat(images.size()).isEqualTo(1);
            DockerImageDescriptor image = images.get(0);
            assertThat(image.getDigest()).startsWith("sha256:");
            assertThat(image.getRepository()).isEqualTo(repository);

            store.commitTransaction();
        } finally {
            dockerClient.removeImageCmd(taggedImageId).exec();
        }
    }

    private void verifyManifest(DockerManifestDescriptor manifestDescriptor) {
        assertThat(manifestDescriptor).isNotNull();
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
        assertThat(dockerConfig.getCmd()).isEqualTo(new String[]{"echo", "Hello", "World;"});
        assertThat(dockerConfig.getDomainName()).isEmpty();
        String[] env = dockerConfig.getEnv();
        assertThat(env).hasSize(1);
        assertThat(env[0]).startsWith("PATH");
        assertThat(dockerConfig.getExposedPorts()).isEqualTo(new String[]{"80/tcp", "8080/tcp"});
        Map<String, String> labels = dockerConfig.getLabels().stream().collect(Collectors.toMap(label -> label.getName(), label -> label.getValue()));
        assertThat(labels).containsEntry("label1", "value1").containsEntry("label2", "value2");
        assertThat(dockerConfig.getHostName()).isEmpty();
        assertThat(dockerConfig.isOpenStdin()).isFalse();
        assertThat(dockerConfig.isStdinOnce()).isFalse();
        assertThat(dockerConfig.isTty()).isFalse();
        assertThat(dockerConfig.getUser()).isEqualTo("helloworld");
        assertThat(dockerConfig.getWorkingDir()).isEqualTo("/home/helloworld");
        assertThat(dockerConfig.getVolumes()).isEqualTo(new String[]{"/data", "/log"});
    }

    private void createImage()
        throws InterruptedException, ExecutionException {
        log.info("Local test image not found, creating it.");
        ImageFromDockerfile image = new ImageFromDockerfile(JQA_TEST_IMAGE, false)
            .withDockerfileFromBuilder(
                builder -> builder.from("alpine:3.2").user("helloworld").workDir("/home/helloworld")
                    .cmd("echo", "Hello", "World;").expose(80, 8080).volume("/data", "/log").label("label1", "value1").label("label2", "value2").build());
        image.get();
    }

    private String pushToRegistry(DockerClient dockerClient, String repositoryUrl, String imageId, String label)
        throws InterruptedException {
        dockerClient.tagImageCmd(imageId + ":" + label, repositoryUrl + "/" + imageId, label).exec();
        String repositoryImageId = repositoryUrl + "/" + imageId + ":" + label;
        dockerClient.pushImageCmd(repositoryImageId).start().awaitCompletion();
        return repositoryImageId;
    }

}
