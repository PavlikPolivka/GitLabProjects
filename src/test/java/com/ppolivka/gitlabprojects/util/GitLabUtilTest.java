package com.ppolivka.gitlabprojects.util;

import com.intellij.openapi.application.ApplicationManager;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for parsing gitlab urls
 *
 * @author ppolivka
 * @since 1.3.6
 */
public class GitLabUtilTest {


  private SettingsState settingsState = new SettingsState();

  @Before
  public void setUp() throws Exception {
    ApplicationManager.setApplication(new DummyApplication(settingsState), new DummyDisposable());
  }

  @Test
  public void isGitLabUrl() throws Exception {
    testUrl("https://gitlab.com/", "https://gitlab.com/Polivka/130regression.git", true);
    testUrl("https://gitlab.com", "https://gitlab.com/Polivka/130regression.git", true);
    testUrl("https://gitlab.com", "https://gitlab.com/Polivka/130regression", true);
    testUrl("https://gitlab.com:8080/", "https://gitlab.com:8080/Polivka/130regression.git", true);
    testUrl("https://gitlab.com:8080/", "https://gitlab.com:8084/Polivka/130regression.git", true);
    testUrl("https://gitlab.com:8080/", "http://gitlab.com:8084/Polivka/130regression.git", true);
    testUrl("http://gitlab.com:8080/", "https://gitlab.com:8084/Polivka/130regression.git", true);
    testUrl("https://gitlab.com/", "git@gitlab.com:Polivka/130regression.git", true);
    testUrl("https://gitlab.com:8080/", "git@gitlab.com:8080:Polivka/130regression.git", true);
    testUrl("https://gitlab.com:8080/", "git@gitlab.com:8084:Polivka/130regression.git", true);
    testUrl("https://gitlab-1.com:8080/", "git@gitlab-1.com:8084:Polivka/130regression.git", true);
    testUrl("https://gitlab-1.com:8080/", "git@gitlab-2.com:8084:Polivka/130regression.git", false);
    testUrl("http://gitlab-1.com:8080/", "git@gitlab-1.com:8084:Polivka/130regression.git", true);
    testUrl("http://gitlab-1.com/", "git@gitlab-1.com:Polivka/130regression.git", true);
    testUrl("http://192.168.1.10/", "git@192.168.1.10:Polivka/130regression.git", true);
    testUrl("http://192.168.1.10/", "http://192.168.1.10/Polivka/130regression.git", true);
  }

  private void testUrl(String settings, String remote, boolean shouldBe) {
    settingsState.setHost(settings);
    Assert.assertEquals(GitLabUtil.isGitLabUrl(settings, remote), shouldBe);
  }

}
