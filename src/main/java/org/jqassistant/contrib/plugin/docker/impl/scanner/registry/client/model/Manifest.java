package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class Manifest {

	public static final String HEADER_DOCKER_CONTENT_DIGEST = "Docker-Content-Digest";

	public static final String MEDIA_TYPE = "application/vnd.docker.distribution.manifest.v2+json";

	private String digest;

	private BlobReference config;

	private List<BlobReference> layers;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@Getter
	@Setter
	@ToString
	public static class BlobReference {

		private String mediaType;

		private long size;

		private String digest;

	}
}
