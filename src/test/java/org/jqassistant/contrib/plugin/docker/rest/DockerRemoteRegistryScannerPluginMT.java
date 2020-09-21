package org.jqassistant.contrib.plugin.docker.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jqassistant.contrib.plugin.docker.api.scope.DockerScope;
import org.junit.jupiter.api.Test;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DockerRemoteRegistryScannerPluginMT extends AbstractPluginIT {

	@Test
	@TestStore(type = TestStore.Type.FILE)
	public void scanTestBench() throws MalformedURLException {
		scanRegistry("http://172.19.245.131:5000", "*");
	}

	@Test
	@TestStore(type = TestStore.Type.FILE)
	public void scanNexus() throws MalformedURLException {
		scanRegistry("http://am-wfa-prd.asml.com:18443", "*");
	}

	@Test
	@TestStore(type = TestStore.Type.FILE)
	public void scanDocker() throws MalformedURLException {
		scanRegistry("http://172.19.245.131:5000", "*");
	}

	private Descriptor scanRegistry(String url, String repositoryIncludePattern) throws MalformedURLException {
		Map<String, Object> props = new HashMap<>();
		props.put("docker.repository.include", repositoryIncludePattern);
		return getScanner(props).scan(new URL(url), url, DockerScope.REGISTRY);
	}
}
