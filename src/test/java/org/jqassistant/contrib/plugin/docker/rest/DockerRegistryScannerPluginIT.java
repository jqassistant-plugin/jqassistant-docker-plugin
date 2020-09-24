package org.jqassistant.contrib.plugin.docker.rest;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.jqassistant.contrib.plugin.docker.api.model.DockerBlobDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerConfigDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerImageDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerManifestDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerRegistryDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerRepositoryDescriptor;
import org.jqassistant.contrib.plugin.docker.api.model.DockerTagDescriptor;
import org.jqassistant.contrib.plugin.docker.api.scope.DockerScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import com.github.dockerjava.api.DockerClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Testcontainers
public class DockerRegistryScannerPluginIT extends AbstractPluginIT {

	public static final String JQA_TEST_IMAGE = "test-image";

	@Container
	public GenericContainer registry = new GenericContainer("registry:latest").withExposedPorts(5000);

	private DockerClient dockerClient;

	private String repositoryUrl;

	@BeforeEach
	public void setup() {
		this.dockerClient = registry.getDockerClient();
		this.repositoryUrl = registry.getHost() + ":" + registry.getFirstMappedPort();
	}

	@TestStore(type = TestStore.Type.FILE)
	@Test
	public void scan() throws MalformedURLException, ExecutionException, InterruptedException {
		createAndPushTestImage("World", "latest");
		DockerRegistryDescriptor registryDescriptor = scanRegistry();

		store.beginTransaction();
		List<DockerRepositoryDescriptor> repositories = registryDescriptor.getContainsRepositories();
		assertThat(repositories.size()).isEqualTo(1);
		DockerRepositoryDescriptor repository = repositories.get(0);
		assertThat(repository.getName()).isEqualTo(JQA_TEST_IMAGE);
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
	}

	@TestStore(type = TestStore.Type.FILE)
	@Test
	public void incrementalScanNewTag() throws MalformedURLException, ExecutionException, InterruptedException {
		createAndPushTestImage("World", "1.0");
		DockerRegistryDescriptor registryDescriptor1 = scanRegistry();
		store.beginTransaction();
		DockerManifestDescriptor manifest1 = registryDescriptor1.getContainsRepositories().get(0).getTags().get(0)
				.getManifest();
		store.commitTransaction();

		createAndPushTestImage("Mars", "2.0");
		DockerRegistryDescriptor registryDescriptor2 = scanRegistry();
		assertThat(registryDescriptor1).isEqualTo(registryDescriptor2);

		store.beginTransaction();
		List<DockerTagDescriptor> tags = registryDescriptor2.getContainsRepositories().get(0).getTags();
		assertThat(tags.size()).isEqualTo(2);
		Map<String, DockerManifestDescriptor> manifestsByTag = tags.stream()
				.collect(toMap(tag -> tag.getName(), tag -> tag.getManifest()));
		assertThat(manifestsByTag).hasSize(2);
		assertThat(manifestsByTag.get("1.0")).isEqualTo(manifest1);
		DockerManifestDescriptor manifest2 = manifestsByTag.get("2.0");
		assertThat(manifest1).isNotEqualTo(manifest2);
		store.commitTransaction();
	}

	@TestStore(type = TestStore.Type.FILE)
	@Test
	public void incrementalScanOverrideTag() throws MalformedURLException, ExecutionException, InterruptedException {
		createAndPushTestImage("World", "latest");
		DockerRegistryDescriptor registryDescriptor1 = scanRegistry();
		store.beginTransaction();
		DockerManifestDescriptor manifest1 = registryDescriptor1.getContainsRepositories().get(0).getTags().get(0)
				.getManifest();
		store.commitTransaction();

		createAndPushTestImage("Mars", "latest");
		DockerRegistryDescriptor registryDescriptor2 = scanRegistry();
		assertThat(registryDescriptor1).isEqualTo(registryDescriptor2);

		store.beginTransaction();
		List<DockerTagDescriptor> tags = registryDescriptor2.getContainsRepositories().get(0).getTags();
		assertThat(tags.size()).isEqualTo(1);
		DockerTagDescriptor tagDescriptor = tags.get(0);
		assertThat(tagDescriptor.getName()).isEqualTo("latest");
		DockerManifestDescriptor manifest2 = tagDescriptor.getManifest();
		assertThat(manifest1).isNotEqualTo(manifest2);
		store.commitTransaction();
	}

	private void createAndPushTestImage(String value, String tag) throws InterruptedException, ExecutionException {
		ImageFromDockerfile image = new ImageFromDockerfile(JQA_TEST_IMAGE + ":" + tag, false)
				.withDockerfileFromBuilder(builder -> builder.from("alpine:3.2").user("helloworld")
						.workDir("/home/helloworld").cmd("echo", "Hello", value).expose(80, 8080)
						.volume("/data", "/log").label("label1", "value1").label("label2", "value2").build());
		String imageId = image.get();
		try {
			dockerClient.tagImageCmd(JQA_TEST_IMAGE + ":" + tag, repositoryUrl + "/" + JQA_TEST_IMAGE, tag).exec();
			String repositoryImageId = repositoryUrl + "/" + JQA_TEST_IMAGE + ":" + tag;
			dockerClient.pushImageCmd(repositoryImageId).start().awaitCompletion();
		} finally {
			dockerClient.removeImageCmd(imageId).exec();
		}
	}

	private DockerRegistryDescriptor scanRegistry() throws MalformedURLException {
		String url = "http://" + repositoryUrl;
		return getScanner().scan(new URL(url), url, DockerScope.REGISTRY);
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
		assertThat(dockerConfig.getCmd()).isEqualTo(new String[] { "echo", "Hello", "World" });
		assertThat(dockerConfig.getDomainName()).isEmpty();
		String[] env = dockerConfig.getEnv();
		assertThat(env).hasSize(1);
		assertThat(env[0]).startsWith("PATH");
		assertThat(dockerConfig.getExposedPorts()).isEqualTo(new String[] { "80/tcp", "8080/tcp" });
		Map<String, String> labels = dockerConfig.getLabels().stream()
				.collect(toMap(label -> label.getName(), label -> label.getValue()));
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
