package com.ppolivka.gitlabprojects.api.dto;

import java.io.Serializable;

/**
 * DTO Class representing one GitLab Project
 *
 * @author ppolivka
 * @since 10.10.2015
 */
public class ProjectDto implements Serializable {
    private String name;
    private String namespace;
    private String sshUrl;
    private String httpUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public void setSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }
}
