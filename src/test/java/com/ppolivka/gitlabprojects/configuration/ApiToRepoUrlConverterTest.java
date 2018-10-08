package com.ppolivka.gitlabprojects.configuration;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ApiToRepoUrlConverterTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "https://gitlab.com", "gitlab.com" },
                { "https://www.gitlab.com", "gitlab.com" },
                { "http://gitlab.com/", "gitlab.com" }
        });
    }

    @Parameterized.Parameter
    public String apiUrl;
    @Parameterized.Parameter(1)
    public String repoUrl;

    @Test
    public void convertApiUrlToRepoUrl() {
        Assert.assertEquals(repoUrl, ApiToRepoUrlConverter.convertApiUrlToRepoUrl(apiUrl));
    }
}