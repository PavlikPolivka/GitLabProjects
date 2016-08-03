package com.ppolivka.gitlabprojects.merge.request;

import org.gitlab.api.models.GitlabUser;

/**
 * TODO:Describe
 *
 * @author ppolivka
 * @since $_version_$
 */
public class SearchableUser {

  private GitlabUser user;

  public SearchableUser(GitlabUser user) {
    this.user = user;
  }

  @Override
  public String toString() {
    return user.getName() + " (" + user.getEmail() + ")";
  }
}
