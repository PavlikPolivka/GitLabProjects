package com.ppolivka.gitlabprojects.dto;


import lombok.Data;

@Data
public class GitlabServer {

    private String apiUrl = "";
    private String apiToken = "";
    private String repositoryUrl = "";
    private CheckoutType preferredConnection = CheckoutType.SSH;
    private boolean removeSourceBranch = true;

    @Override
    public String toString() {
        return apiUrl;
    }

    public enum CheckoutType {
        SSH,
        HTTPS;
    }

}
