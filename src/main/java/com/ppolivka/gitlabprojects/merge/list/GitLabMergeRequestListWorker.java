package com.ppolivka.gitlabprojects.merge.list;

import com.intellij.notification.NotificationListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.Convertor;
import com.ppolivka.gitlabprojects.configuration.ProjectState;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.exception.MergeRequestException;
import com.ppolivka.gitlabprojects.merge.GitLabDiffViewWorker;
import com.ppolivka.gitlabprojects.merge.GitLabMergeRequestWorker;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.ppolivka.gitlabprojects.merge.GitLabMergeRequestWorker.Util.fillRequiredInfo;
import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;

/**
 * Worker for listing and accepting merge request
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class GitLabMergeRequestListWorker implements GitLabMergeRequestWorker {

    static SettingsState settingsState = SettingsState.getInstance();

    private Git git;
    private Project project;
    private ProjectState projectState;
    private GitRepository gitRepository;
    private String remoteUrl;
    private GitlabProject gitlabProject;
    private String remoteProjectName;
    private GitLabDiffViewWorker diffViewWorker;

    private List<GitlabMergeRequest> mergeRequests;

    public void mergeBranches(final Project project, final GitlabMergeRequest mergeRequest, final boolean removeSource) {
        new Task.Backgroundable(project, "Merging Branches...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    settingsState.api().acceptMergeRequest(gitlabProject, mergeRequest, removeSource);
                    VcsNotifier.getInstance(project)
                            .notifyImportantInfo("Merged", "Merge request is merged.", NotificationListener.URL_OPENING_LISTENER);
                } catch (IOException e) {
                    showErrorDialog(project, "Cannot create merge this request", "Cannot Merge");
                }
            }
        }.queue();
    }


    public static GitLabMergeRequestListWorker create(@NotNull final Project project, @Nullable final VirtualFile file) {
        return GitLabUtil.computeValueInModal(project, "Loading data...", new Convertor<ProgressIndicator, GitLabMergeRequestListWorker>() {
            @Override
            public GitLabMergeRequestListWorker convert(ProgressIndicator indicator) {
                GitLabMergeRequestListWorker mergeRequestListWorker = new GitLabMergeRequestListWorker();

                try {
                    fillRequiredInfo(mergeRequestListWorker, project, file);
                } catch (MergeRequestException e) {
                    return null;
                }

                try {
                    mergeRequestListWorker.setMergeRequests(settingsState.api().getMergeRequests(mergeRequestListWorker.getGitlabProject()));
                } catch (IOException e) {
                    mergeRequestListWorker.setMergeRequests(Collections.<GitlabMergeRequest>emptyList());
                    showErrorDialog(project, "Cannot load merge requests from GitLab API", "Cannot Load Merge Requests");
                }

                return mergeRequestListWorker;
            }
        });
    }

    //region Getters & Setters
    @Override
    public Git getGit() {
        return git;
    }

    @Override
    public void setGit(Git git) {
        this.git = git;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public ProjectState getProjectState() {
        return projectState;
    }

    @Override
    public void setProjectState(ProjectState projectState) {
        this.projectState = projectState;
    }

    @Override
    public GitRepository getGitRepository() {
        return gitRepository;
    }

    @Override
    public void setGitRepository(GitRepository gitRepository) {
        this.gitRepository = gitRepository;
    }

    @Override
    public String getRemoteUrl() {
        return remoteUrl;
    }

    @Override
    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    @Override
    public GitlabProject getGitlabProject() {
        return gitlabProject;
    }

    @Override
    public void setGitlabProject(GitlabProject gitlabProject) {
        this.gitlabProject = gitlabProject;
    }

    @Override
    public String getRemoteProjectName() {
        return remoteProjectName;
    }

    @Override
    public void setRemoteProjectName(String remoteProjectName) {
        this.remoteProjectName = remoteProjectName;
    }

    @Override
    public GitLabDiffViewWorker getDiffViewWorker() {
        return diffViewWorker;
    }

    @Override
    public void setDiffViewWorker(GitLabDiffViewWorker diffViewWorker) {
        this.diffViewWorker = diffViewWorker;
    }

    public List<GitlabMergeRequest> getMergeRequests() {
        return mergeRequests;
    }

    public void setMergeRequests(List<GitlabMergeRequest> mergeRequests) {
        this.mergeRequests = mergeRequests;
    }
    //endregion
}
