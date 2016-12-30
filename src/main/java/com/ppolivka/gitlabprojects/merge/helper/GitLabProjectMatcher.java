package com.ppolivka.gitlabprojects.merge.helper;

import com.ppolivka.gitlabprojects.configuration.ProjectState;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.exception.GitLabException;
import git4idea.repo.GitRemote;
import org.apache.commons.lang.StringUtils;
import org.gitlab.api.models.GitlabProject;

import java.util.Collection;
import java.util.Optional;

public class GitLabProjectMatcher {

    private static SettingsState settingsState = SettingsState.getInstance();

    public Optional<GitlabProject> resolveProject(ProjectState projectState, GitRemote remote) {
        String remoteProjectName = remote.getName();
        String remoteUrl = remote.getFirstUrl();

        Integer projectId = projectState.getProjectId();
        if (projectId == null) {
            try {
                Collection<GitlabProject> projects = settingsState.api().getProjects();
                for (GitlabProject gitlabProject : projects) {
                    if (gitlabProject.getName().equals(remoteProjectName) || urlMatch(remoteUrl, gitlabProject.getSshUrl()) || urlMatch(remoteUrl, gitlabProject.getHttpUrl())) {
                        projectId = gitlabProject.getId();
                        projectState.setProjectId(projectId);
                        return Optional.of(gitlabProject);
                    }
                }
            } catch (Throwable throwable) {
                throw new GitLabException("Cannot match project.", throwable);
            }
        }
        return Optional.empty();
    }

    private boolean urlMatch(String remoteUrl, String apiUrl) {
        String formattedRemoteUrl = remoteUrl.trim();
        String formattedApiUrl = apiUrl.trim();
        formattedRemoteUrl = formattedRemoteUrl.replace("https://", "");
        formattedRemoteUrl = formattedRemoteUrl.replace("http://", "");
        return StringUtils.isNotBlank(formattedApiUrl) && StringUtils.isNotBlank(formattedRemoteUrl) && formattedApiUrl.contains(formattedRemoteUrl);
    }

}
