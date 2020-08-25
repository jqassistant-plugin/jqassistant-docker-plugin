package org.jqassistant.contrib.plugin.docker.rest;

import java.net.MalformedURLException;
import java.net.URL;

import org.jqassistant.contrib.plugin.docker.api.model.DockerRegistryDescriptor;
import org.jqassistant.contrib.plugin.docker.api.scope.DockerScope;
import org.junit.jupiter.api.Test;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

public class DockerRegistryScannerPluginIT extends AbstractPluginIT {

	@Test
	@TestStore(type = TestStore.Type.FILE)
	public void scanRegistry() throws MalformedURLException {
		String url = "http://localhost:5000";
		DockerRegistryDescriptor registryDescriptor = getScanner().scan(new URL(url), url, DockerScope.REGISTRY);
	}

}
