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
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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
                @Storage(id = "default", file = "$APP_CONFIG$/gitlab-project-settings.xml")
        }
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    public String host;

    public String token;

    public boolean defaultRemoveBranch;

    public Collection<ProjectDto> projects = new ArrayList<>();

    public Collection<ServerDto> servers = new ArrayList<>();

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

    public void reloadProjects(ServerDto serverDto) throws Throwable {
        ApiFacade apiFacade = api(serverDto);

        Collection<ProjectDto> projects = new ArrayList<>();

            for (GitlabProject gitlabProject : apiFacade.getProjects()) {
                ProjectDto projectDto = new ProjectDto();
                projectDto.setName(gitlabProject.getName());
                projectDto.setNamespace(gitlabProject.getNamespace().getName());
                projectDto.setHttpUrl(gitlabProject.getHttpUrl());
                projectDto.setSshUrl(gitlabProject.getSshUrl());
                projects.add(projectDto);
            }
        this.setProjects(projects);

    }

    public ApiFacade api(Project project, VirtualFile file) {
        return api(currentGitlabServer(project, file));
    }

    public ApiFacade api(GitRepository gitRepository) {
        return api(currentGitlabServer(gitRepository));
    }

    public ApiFacade api(ServerDto serverDto) {
        return new ApiFacade(serverDto.getHost(), serverDto.getToken());
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

    public Collection<ServerDto> getServers() {
        return servers;
    }

    public void setServers(Collection<ServerDto> servers) {
        this.servers = servers;
    }

    public Collection<ServerDto> getAllServers() {
        Collection<ServerDto> allServers = new ArrayList<>(getServers());
        if(StringUtils.isNotBlank(host) && StringUtils.isNotBlank(token)) {
            ServerDto serverDto = new ServerDto();
            serverDto.setHost(host);
            serverDto.setToken(token);
            serverDto.setDefaultRemoveBranch(defaultRemoveBranch);
            allServers.add(serverDto);
        }

        return allServers;
    }

    public void addServer(ServerDto serverDto) {
        if(getServers().stream().noneMatch(serverDto1 -> serverDto.getHost().equals(serverDto1.getHost()))) {
            getServers().add(serverDto);
        } else {
            getServers().stream().filter(serverDto1 -> serverDto.getHost().equals(serverDto1.getHost())).forEach(server -> {
                server.setHost(serverDto.getHost());
                server.setToken(serverDto.getToken());
                server.setDefaultRemoveBranch(serverDto.isDefaultRemoveBranch());
            });
        }
    }

    public void deleteServer(ServerDto serverDto) {
        getServers().stream().filter(server -> serverDto.getHost().equals(server.getHost())).forEach(server -> getServers().remove(server));
    }
    public ServerDto currentGitlabServer(Project project, VirtualFile file) {
        GitRepository gitRepository = GitLabUtil.getGitRepository(project, file);
        return currentGitlabServer(gitRepository);
    }

    public ServerDto currentGitlabServer(GitRepository gitRepository) {
        ;
        for (GitRemote gitRemote : gitRepository.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                for(ServerDto server : getAllServers()) {
                    if (isGitLabUrl(server.getHost(), remoteUrl)) {
                        return server;
                    }
                }
            }
        }
        return null;
    }

    //endregion

}
