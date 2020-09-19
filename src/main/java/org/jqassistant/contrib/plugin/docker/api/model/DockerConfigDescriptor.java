package org.jqassistant.contrib.plugin.docker.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Config")
public interface DockerConfigDescriptor extends DockerDescriptor {

    Boolean isArgsEscaped();
    void setArgsEscaped(Boolean argsEscaped);

    Boolean isAttachStderr();
    void setAttachStderr(Boolean attachStderr);

    Boolean isAttachStdin();
    void setAttachStdin(Boolean attachStdin);

    Boolean isAttachStdout();
    void setAttachStdout(Boolean attachStdout);

    String[] getCmd();
    void setCmd(String[] cmd);

    String getDomainName();
    void setDomainName(String domainName);

    String[] getEntryPoint();
    void setEntryPoint(String[] entrypoint);

    String[] getEnv();
    void setEnv(String[] env);

    String[] getExposedPorts();
    void setExposedPorts(String[] exposedPorts);

    String getHostName();
    void setHostName(String hostName);

    String getImage();
    void setImage(String image);

    String getOnBuild();
    void setOnBuild(String onBuild);

    Boolean isOpenStdin();
    void setOpenStdin(Boolean openStdin);

    Boolean isStdinOnce();
    void setStdinOnce(Boolean stdinOnce);

    Boolean isTty();
    void setTty(Boolean tty);

    String getUser();
    void setUser(String user);

    String[] getVolumes();
    void setVolumes(String[] toArray);

    String getWorkingDir();
    void setWorkingDir(String workingDir);

    @Relation("HAS_LABEL")
    List<DockerLabelDescriptor> getLabels();
}
