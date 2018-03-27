package com.ppolivka.gitlabprojects.checkout;

import com.intellij.dvcs.hosting.RepositoryListLoader;
import com.intellij.dvcs.hosting.RepositoryListLoadingException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.ppolivka.gitlabprojects.api.dto.ProjectDto;
import com.ppolivka.gitlabprojects.configuration.SettingsDialog;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import git4idea.DialogManager;
import git4idea.remote.GitRepositoryHostingService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GitLabRepositoryHostingService extends GitRepositoryHostingService {
    @NotNull
    @Override
    public String getServiceDisplayName() {
        return "GitLab";
    }

    @NotNull
    @Override
    public RepositoryListLoader getRepositoryListLoader(@NotNull Project project) {
        return new RepositoryListLoader() {

            private SettingsState settingsState = SettingsState.getInstance();

            @Override
            public boolean isEnabled() {
                return settingsState.isEnabled();
            }

            @Override
            public boolean enable() {
                SettingsDialog settingsDialog = new SettingsDialog(project);
                DialogManager.show(settingsDialog);
                return isEnabled();
            }

            @NotNull
            @Override
            public List<String> getAvailableRepositories(@NotNull ProgressIndicator progressIndicator) throws RepositoryListLoadingException {
                try {
                    List<String> repos = new ArrayList<>();
                    GitLabUtil.runInterruptable(progressIndicator, () -> {
                        try {
                            settingsState.reloadProjects(settingsState.getAllServers());
                            return settingsState.getProjects();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                        return new ArrayList<ProjectDto>();
                    }).forEach(projectDto -> {
                        repos.add(projectDto.getSshUrl());
                        repos.add(projectDto.getHttpUrl());
                    });
                    return repos;
                } catch (IOException e) {
                    return Collections.emptyList();
                }
            }
        };
    }
}
