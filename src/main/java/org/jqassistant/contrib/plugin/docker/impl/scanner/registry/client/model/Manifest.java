package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class Manifest {

	private String digest;

	private String name;

	private String tag;

	private String architecture;

	private Optional<List<FileSystemLayer>> fsLayers;

	private Optional<List<History>> history;
}
