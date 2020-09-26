package org.jqassistant.contrib.plugin.docker.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jqassistant.contrib.plugin.docker.api.scope.DockerScope;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DockerRemoteRegistryScannerPluginIT extends AbstractPluginIT {

	@Disabled
	@Test
	@TestStore(type = TestStore.Type.FILE)
	public void scanLocalRegistry() throws MalformedURLException {
		scanRegistry("http://localhost:5000", "*");
	}

	private Descriptor scanRegistry(String url, String repositoryIncludePattern) throws MalformedURLException {
		return scanRegistry(url, repositoryIncludePattern, null);
	}

	private Descriptor scanRegistry(String url, String repositoryIncludePattern, String repositoryExcludePattern)
			throws MalformedURLException {
		Map<String, Object> props = new HashMap<>();
		props.put("docker.repository.include", repositoryIncludePattern);
		if (repositoryExcludePattern != null) {
			props.put("docker.repository.exclude", repositoryExcludePattern);
		}
		return getScanner(props).scan(new URL(url), url, DockerScope.REGISTRY);
	}
}
