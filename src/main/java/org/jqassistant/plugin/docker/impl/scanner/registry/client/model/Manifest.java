package org.jqassistant.plugin.docker.impl.scanner.registry.client.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class Manifest extends BlobReference {

    public static final String HEADER_DOCKER_CONTENT_DIGEST = "Docker-Content-Digest";

    public static final String MEDIA_TYPE = "application/vnd.docker.distribution.manifest.v2+json";

    private BlobReference config;

    private List<BlobReference> layers;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @ToString
    public static class Content {

        private String id;

        private String parent;

        private String architecture;

        private String os;

        @JsonProperty("docker_version")
        private String dockerVersion;

        private ZonedDateTime created;

        private Config config;

        private String container;

        @JsonProperty("container_config")
        private Config containerConfig;

        private Boolean throwaway;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @ToString
    public static class Config {

        @JsonProperty("Hostname")
        private String hostName;

        @JsonProperty("Domainname")
        private String domainName;

        @JsonProperty("User")
        private String user;

        @JsonProperty("AttachStdin")
        private Boolean attachStdin;

        @JsonProperty("AttachStdout")
        private Boolean attachStdout;

        @JsonProperty("AttachStderr")
        private Boolean attachStderr;

        @JsonProperty("ExposedPorts")
        private Map<String, Object> exposedPorts;

        @JsonProperty("Tty")
        private Boolean tty;

        @JsonProperty("OpenStdin")
        private Boolean openStdin;

        @JsonProperty("StdinOnce")
        private Boolean stdinOnce;

        @JsonProperty("Env")
        private String[] env;

        @JsonProperty("Cmd")
        private String[] cmd;

        @JsonProperty("ArgsEscaped")
        private Boolean argsEscaped;

        @JsonProperty("Image")
        private String image;

        @JsonProperty("Volumes")
        private Map<String, Object> volumes;

        @JsonProperty("WorkingDir")
        private String workingDir;

        @JsonProperty("EntryPoint")
        private String[] entrypoint;

        @JsonProperty("onBuild")
        private String onBuild;

        @JsonProperty("Labels")
        private Map<String, String> labels;

    }
}
