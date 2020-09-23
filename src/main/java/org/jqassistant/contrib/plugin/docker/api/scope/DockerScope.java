package org.jqassistant.contrib.plugin.docker.api.scope;

import com.buschmais.jqassistant.core.scanner.api.Scope;

public enum DockerScope implements Scope {

	REGISTRY;

	@Override
	public String getPrefix() {
		return "docker";
	}

	@Override
	public String getName() {
		return name();
	}
}
