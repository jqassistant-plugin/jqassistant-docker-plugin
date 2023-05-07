package org.jqassistant.plugin.docker.impl.scanner.registry.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BlobReference {

    private String mediaType;

    private long size;

    @EqualsAndHashCode.Include
    private String digest;

}
