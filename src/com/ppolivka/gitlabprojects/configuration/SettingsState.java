package com.ppolivka.gitlabprojects.configuration;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.ppolivka.gitlabprojects.api.ApiFacade;
import com.ppolivka.gitlabprojects.api.dto.Project;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;


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

    public Collection<Project> projects = new ArrayList<>();

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

    public void reloadProjects() throws Throwable {
        ApiFacade apiFacade = new ApiFacade(host, token);

        Collection<Project> projects = new ArrayList<>();

            for (GitlabProject gitlabProject : apiFacade.getProjects()) {
                Project project = new Project();
                project.setName(gitlabProject.getName());
                project.setNamespace(gitlabProject.getNamespace().getName());
                project.setHttpUrl(gitlabProject.getHttpUrl());
                project.setSshUrl(gitlabProject.getSshUrl());
                projects.add(project);
            }
        this.setProjects(projects);

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

    public Collection<Project> getProjects() {
        return projects;
    }

    public void setProjects(Collection<Project> projects) {
        this.projects = projects;
    }

    //endregion

}
