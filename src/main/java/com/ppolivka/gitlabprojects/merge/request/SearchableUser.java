package com.ppolivka.gitlabprojects.merge.request;

import org.gitlab.api.models.GitlabUser;

/**
 * One user returned from Search, used by combo box model
 *
 * @author ppolivka
 * @since 1.4.0
 */
public class SearchableUser {

  private GitlabUser user;

  public SearchableUser(GitlabUser user) {
    this.user = user;
  }

  public GitlabUser getGitLabUser() {
    return user;
  }

  @Override
  public String toString() {
    return user.getName();
  }
}
