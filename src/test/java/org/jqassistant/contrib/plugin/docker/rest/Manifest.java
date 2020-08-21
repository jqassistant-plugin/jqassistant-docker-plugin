package org.jqassistant.contrib.plugin.docker.rest;

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

    private String name;

    private String tag;

    private String architecture;

    private List<FileSystemLayer> fsLayers;

}
