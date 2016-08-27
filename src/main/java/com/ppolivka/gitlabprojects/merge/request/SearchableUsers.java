package com.ppolivka.gitlabprojects.merge.request;

import com.intellij.openapi.project.Project;
import com.ppolivka.gitlabprojects.component.Searchable;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.util.MessageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * Searchable users model
 *
 * @author ppolivka
 * @since 1.4.0
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
            List<SearchableUser> users = settingsState.api().searchUsers(toSearch).stream().map(SearchableUser::new).collect(Collectors.toList());
            List<SearchableUser> resultingUsers = new ArrayList<>();
            resultingUsers.addAll(users);
            return resultingUsers;
        } catch (IOException e) {
            MessageUtil.showErrorDialog(project, "New remote origin cannot be added to this project.", "Cannot Add New Remote");
        }
        return emptyList();
    }

}
