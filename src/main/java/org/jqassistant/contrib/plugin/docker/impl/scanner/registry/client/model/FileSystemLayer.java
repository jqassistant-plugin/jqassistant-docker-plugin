package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class FileSystemLayer {

	private String blobSum;

}
