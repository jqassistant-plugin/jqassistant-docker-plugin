package org.jqassistant.contrib.plugin.docker.rest;

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
