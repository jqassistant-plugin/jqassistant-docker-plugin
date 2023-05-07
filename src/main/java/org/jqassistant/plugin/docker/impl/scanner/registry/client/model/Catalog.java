package org.jqassistant.plugin.docker.impl.scanner.registry.client.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Catalog {

    private List<String> repositories;

}
