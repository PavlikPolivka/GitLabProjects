package com.ppolivka.gitlabprojects.api.dto;

import java.util.Objects;

public class ServerDto {

    private String host;
    private String token;
    private boolean defaultRemoveBranch;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isDefaultRemoveBranch() {
        return defaultRemoveBranch;
    }

    public void setDefaultRemoveBranch(boolean defaultRemoveBranch) {
        this.defaultRemoveBranch = defaultRemoveBranch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerDto serverDto = (ServerDto) o;
        return defaultRemoveBranch == serverDto.defaultRemoveBranch &&
                Objects.equals(host, serverDto.host) &&
                Objects.equals(token, serverDto.token);
    }

    @Override
    public int hashCode() {

        return Objects.hash(host, token, defaultRemoveBranch);
    }

    @Override
    public String toString() {
        return host;
    }
}
