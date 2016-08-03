package com.ppolivka.gitlabprojects.merge.request;

import com.intellij.openapi.project.Project;
import com.ppolivka.gitlabprojects.component.Searchable;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.util.MessageUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * TODO:Describe
 *
 * @author ppolivka
 * @since $_version_$
 */
public class SearchableUsers implements Searchable<SearchableUser, String> {

  private Project project;
  private static SettingsState settingsState = SettingsState.getInstance();

  public SearchableUsers(Project project) {
    this.project = project;
  }

  @Override
  public Collection<SearchableUser> search(String toSearch) {
    try {
      return settingsState.api().searchUsers(toSearch).stream().map(SearchableUser::new).collect(Collectors.toList());
    } catch (IOException e) {
      MessageUtil.showErrorDialog(project, "New remote origin cannot be added to this project.", "Cannot Add New Remote");
    }
    return emptyList();
  }
}
