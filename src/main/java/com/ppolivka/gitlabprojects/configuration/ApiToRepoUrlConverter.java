package com.ppolivka.gitlabprojects.configuration;

import lombok.SneakyThrows;

import java.net.URI;

public class ApiToRepoUrlConverter {

    @SneakyThrows
    public static String convertApiUrlToRepoUrl(String apiUrl) {
        URI uri = new URI(apiUrl);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

}
