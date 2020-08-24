package org.jqassistant.contrib.plugin.docker.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class V1Compatibility {

    private String parent;

    private String id;

    private String architecture;

    private String os;

    @JsonProperty("docker_version")
    private String dockerVersion;

}
