package com.ppolivka.gitlabprojects.configuration;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.ppolivka.gitlabprojects.api.ApiFacade;
import com.ppolivka.gitlabprojects.api.dto.ProjectDto;
import com.ppolivka.gitlabprojects.api.dto.ServerDto;
import com.ppolivka.gitlabprojects.dto.GitlabServer;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

import static com.ppolivka.gitlabprojects.util.GitLabUtil.isGitLabUrl;


/**
 * Settings State for GitLab Projects plugin
 *
 * @author ppolivka
 * @since 9.10.2015
 */
@State(
        name = "SettingsState",
        storages = {
                @Storage("$APP_CONFIG$/gitlab-project-settings-new-format.xml")
        }
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    public String host;

    public String token;

    public boolean defaultRemoveBranch;

    public Collection<ProjectDto> projects = new ArrayList<>();

    public Collection<GitlabServer> gitlabServers = new ArrayList<>();

    public static SettingsState getInstance() {
        return ServiceManager.getService(SettingsState.class);
    }

    @Nullable
    @Override
    public SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(SettingsState settingsState) {
        XmlSerializerUtil.copyBean(settingsState, this);
    }

    public void isApiValid(Project project, VirtualFile file) throws IOException {
        api(project, file).getSession();
    }
    public void isApiValid(String host, String key) throws IOException {
        ApiFacade apiFacade = new ApiFacade();
        apiFacade.reload(host, key);
        apiFacade.getSession();
    }

    public void reloadProjects(Collection<GitlabServer> servers) throws Throwable {
        setProjects(new ArrayList<>());
        for(GitlabServer server : servers) {
            reloadProjects(server);
        }
    }

    @SneakyThrows
    public Map<GitlabServer, Collection<ProjectDto>> loadMapOfServersAndProjects(Collection<GitlabServer> servers) {
        Map<GitlabServer, Collection<ProjectDto>> map = new HashMap<>();
        for(GitlabServer server : servers) {
            Collection<ProjectDto> projects = loadProjects(server);
            map.put(server, projects);
        }
        return map;
    }

    public void reloadProjects(GitlabServer server) throws Throwable {
        this.setProjects(loadProjects(server));

    }

    public Collection<ProjectDto> loadProjects(GitlabServer server) throws Throwable {
        ApiFacade apiFacade = api(server);

        Collection<ProjectDto> projects = getProjects();
        if(projects == null) {
            projects = new ArrayList<>();
        }

        for (GitlabProject gitlabProject : apiFacade.getProjects()) {
            ProjectDto projectDto = new ProjectDto();
            projectDto.setName(gitlabProject.getName());
            projectDto.setNamespace(gitlabProject.getNamespace().getName());
            projectDto.setHttpUrl(gitlabProject.getHttpUrl());
            projectDto.setSshUrl(gitlabProject.getSshUrl());
            projects.add(projectDto);
        }
        this.setProjects(projects);
        return projects;

    }

    public ApiFacade api(Project project, VirtualFile file) {
        return api(currentGitlabServer(project, file));
    }

    public ApiFacade api(GitRepository gitRepository) {
        return api(currentGitlabServer(gitRepository));
    }

    public ApiFacade api(GitlabServer serverDto) {
        return new ApiFacade(serverDto.getApiUrl(), serverDto.getApiToken());
    }

    //region Getters & Setters

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isDefaultRemoveBranch() {
        return defaultRemoveBranch;
    }

    public void setDefaultRemoveBranch(boolean defaultRemoveBranch) {
        this.defaultRemoveBranch = defaultRemoveBranch;
    }

    public Collection<ProjectDto> getProjects() {
        return projects;
    }

    public void setProjects(Collection<ProjectDto> projects) {
        this.projects = projects;
    }

    public Collection<GitlabServer> getGitlabServers() {
        return gitlabServers;
    }

    public void setGitlabServers(Collection<GitlabServer> gitlabServers) {
        this.gitlabServers = gitlabServers;
    }

    public void addServer(GitlabServer server) {
        if(getGitlabServers().stream().noneMatch(server1 -> server.getApiUrl().equals(server1.getApiUrl()))) {
            getGitlabServers().add(server);
        } else {
            getGitlabServers().stream().filter(server1 -> server.getApiUrl().equals(server1.getApiUrl())).forEach(changedServer -> {
                changedServer.setApiUrl(server.getApiUrl());
                changedServer.setRepositoryUrl(server.getRepositoryUrl());
                changedServer.setApiToken(server.getApiToken());
                changedServer.setPreferredConnection(server.getPreferredConnection());
                changedServer.setRemoveSourceBranch(server.isRemoveSourceBranch());
            });
        }
    }

    public void deleteServer(GitlabServer server) {
        getGitlabServers().stream().filter(server1 -> server.getApiUrl().equals(server1.getApiUrl())).forEach(removedServer -> getGitlabServers().remove(removedServer));
    }
    public GitlabServer currentGitlabServer(Project project, VirtualFile file) {
        GitRepository gitRepository = GitLabUtil.getGitRepository(project, file);
        return currentGitlabServer(gitRepository);
    }

    public GitlabServer currentGitlabServer(GitRepository gitRepository) {
        for (GitRemote gitRemote : gitRepository.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                for(GitlabServer server : getGitlabServers()) {
                    if(remoteUrl.contains(server.getRepositoryUrl()))
                        return server;
                    }
                }
            }
        return null;
    }

    public boolean isEnabled() {
        return getGitlabServers() != null && getGitlabServers().size() > 0;
    }

    //endregion

}
