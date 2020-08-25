package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Tags {

	private String name;

	private List<String> tags;

}
