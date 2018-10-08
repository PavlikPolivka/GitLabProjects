package com.ppolivka.gitlabprojects.merge.helper;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.ppolivka.gitlabprojects.api.ApiFacade;
import com.ppolivka.gitlabprojects.api.dto.ServerDto;
import com.ppolivka.gitlabprojects.configuration.ProjectState;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.dto.GitlabServer;
import com.ppolivka.gitlabprojects.util.DummyApplication;
import com.ppolivka.gitlabprojects.util.DummyDisposable;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.gitlab.api.models.GitlabProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static java.util.Collections.emptyList;

public class GitLabProjectMatcherTest {

    GitLabProjectMatcher gitLabProjectMatcher;

    DummySettingState settingsState;

    @Before
    public void setUp() throws Exception {
        settingsState = new DummySettingState();
        ApplicationManager.setApplication(new DummyApplication(settingsState), new DummyDisposable());
        gitLabProjectMatcher = new GitLabProjectMatcher();
    }

    @Test
    public void resolveProjectValidHttp() throws Exception {
        testResolving("https://gitlab.com/Polivka/kugkg.git", "https://gitlab.com/Polivka/kugkg.git", false, true);
    }

    @Test
    public void resolveProjectValidHttpMixup() throws Exception {
        testResolving("https://gitlab.com/Polivka/kugkg.git", "http://gitlab.com/Polivka/kugkg.git", false, true);
    }

    @Test
    public void resolveProjectNotResolvingSsh() throws Exception {
        testResolving("git@gitlab1.com:Polivka/kugkg", "git@gitlab2.com:Polivka/kugkg.git", true, false);
    }

    @Test
    public void resolveProjectNotResolvingHttp() throws Exception {
        testResolving("https://gitlab1.com/Polivka/kugkg", "https://gitlab2.com/Polivka/kugkg.git", false, false);
    }

    @Test
    public void resolveProjectMissingSuffixHttp() throws Exception {
        testResolving("https://gitlab.com/Polivka/kugkg", "https://gitlab.com/Polivka/kugkg.git", false, true);
    }

    private void testResolving(String remoteUrl, String projectUrl, boolean ssh, boolean shouldBeResolved) {
        ProjectState projectState = new ProjectState();
        GitRemote remote = remote("origin", remoteUrl);
        GitlabProject project = project(1, projectUrl, ssh);
        settingsState.addProject(1, project);
        Optional<GitlabProject> resolvedProject = gitLabProjectMatcher.resolveProject(projectState, remote, null);
        Assert.assertNotNull(resolvedProject);
        Assert.assertEquals(shouldBeResolved, resolvedProject.isPresent());
    }


    private GitlabProject project(Integer id, String url, boolean ssh) {
        GitlabProject project = new GitlabProject();
        project.setId(id);
        project.setName("");
        if(ssh) {
            project.setHttpUrl("");
            project.setSshUrl(url);
        } else {
            project.setHttpUrl(url);
            project.setSshUrl("");
        }
        return project;
    }

    private GitRemote remote(String name, String url) {
        return new GitRemote(name, Arrays.asList(url), emptyList(), emptyList(), emptyList());
    }

    private class DummyApiFacade extends ApiFacade {
        private Map<Integer,GitlabProject> projects = new HashMap<>();

        public void addProject(Integer id, GitlabProject project) {
            projects.put(id, project);
        }

        @Override
        public GitlabProject getProject(Integer id) throws IOException {
            return projects.get(id);
        }

        @Override
        public Collection<GitlabProject> getProjects() throws Throwable {
            return projects.values();
        }
    }

    private class DummySettingState extends SettingsState {
        DummyApiFacade apiFacade = new DummyApiFacade();

        @Override
        public ApiFacade api(Project project, VirtualFile file) {
            return apiFacade;
        }

        @Override
        public ApiFacade api(GitRepository gitRepository) {
            return apiFacade;
        }

        @Override
        public ApiFacade api(GitlabServer serverDto) {
            return apiFacade;
        }

        public void addProject(Integer id, GitlabProject project) {
            apiFacade.addProject(id, project);
        }
    }

}