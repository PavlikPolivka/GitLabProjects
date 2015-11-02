package com.ppolivka.gitlabprojects.merge.request;

import com.intellij.notification.NotificationListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThrowableConvertor;
import com.intellij.util.containers.Convertor;
import com.ppolivka.gitlabprojects.common.GitLabUtils;
import com.ppolivka.gitlabprojects.configuration.ProjectState;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.exception.MergeRequestException;
import com.ppolivka.gitlabprojects.merge.*;
import com.ppolivka.gitlabprojects.merge.info.BranchInfo;
import com.ppolivka.gitlabprojects.merge.info.DiffInfo;
import git4idea.GitLocalBranch;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GitLab Merge request worker
 *
 * @author ppolivka
 * @since 30.10.2015
 */
public class GitLabCreateMergeRequestWorker implements GitLabMergeRequestWorker {


    private static final String CANNOT_SHOW_DIFF_INFO = "Cannot Show Diff Info";

    private static SettingsState settingsState = SettingsState.getInstance();

    private Git git;
    private Project project;
    private ProjectState projectState;
    private GitRepository gitRepository;
    private String remoteUrl;
    private GitlabProject gitlabProject;
    private String remoteProjectName;
    private GitLabDiffViewWorker diffViewWorker;

    private GitLocalBranch gitLocalBranch;
    private BranchInfo localBranchInfo;
    private List<BranchInfo> branches;
    private BranchInfo lastUsedBranch;

    public void createMergeRequest(final BranchInfo branch, final String title, final String description) {
        new Task.Backgroundable(project, "Creating merge request...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {

                indicator.setText("Pushing current branch...");
                GitCommandResult result = git.push(gitRepository, branch.getRemoteName(), remoteUrl, gitLocalBranch.getName(), true);
                if (!result.success()) {
                    Messages.showErrorDialog(project, "Push failed:<br/>" + result.getErrorOutputAsHtmlString(), CANNOT_CREATE_MERGE_REQUEST);
                    return;
                }

                indicator.setText("Creating merge request...");
                GitlabMergeRequest mergeRequest;
                try {
                    mergeRequest = settingsState.api().createMergeRequest(gitlabProject, gitLocalBranch.getName(), branch.getName(), title, description);
                } catch (IOException e) {
                    Messages.showErrorDialog(project, "Cannot create Merge Request vis GitLab REST API", CANNOT_CREATE_MERGE_REQUEST);
                    return;
                }
                VcsNotifier.getInstance(project)
                        .notifyImportantInfo(title, "<a href='" + generateMergeRequestUrl(mergeRequest) + "'>Merge request '" + title + "' created</a>", NotificationListener.URL_OPENING_LISTENER);
            }
        }.queue();
    }

    private String generateMergeRequestUrl(GitlabMergeRequest mergeRequest) {
        final String hostText = settingsState.getHost();
        StringBuilder helpUrl = new StringBuilder();
        helpUrl.append(hostText);
        if (!hostText.endsWith("/")) {
            helpUrl.append("/");
        }
        helpUrl.append(gitlabProject.getPathWithNamespace());
        helpUrl.append("/merge_requests/");
        helpUrl.append(mergeRequest.getIid());
        return helpUrl.toString();
    }

    public boolean checkAction(@Nullable final BranchInfo branch) {
        if (branch == null) {
            Messages.showWarningDialog(project, "Target branch is not selected", CANNOT_CREATE_MERGE_REQUEST);
            return false;
        }

        DiffInfo info;
        try {
            info = GitLabUtils
                    .computeValueInModal(project, "Collecting diff data...", new ThrowableConvertor<ProgressIndicator, DiffInfo, IOException>() {
                        @Override
                        public DiffInfo convert(ProgressIndicator indicator) throws IOException {
                            return GitLabUtils.runInterruptable(indicator, new ThrowableComputable<DiffInfo, IOException>() {
                                @Override
                                public DiffInfo compute() throws IOException {
                                    return diffViewWorker.getDiffInfo(localBranchInfo, branch);
                                }
                            });
                        }
                    });
        } catch (IOException e) {
            Messages.showErrorDialog(project, "Can't collect diff data", CANNOT_CREATE_MERGE_REQUEST);
            return true;
        }
        if (info == null) {
            return true;
        }

        String localBranchName = "'" + gitLocalBranch.getName() + "'";
        String targetBranchName = "'" + branch.getRemoteName() + "/" + branch.getName() + "'";
        if (info.getInfo().getBranchToHeadCommits(gitRepository).isEmpty()) {
            return GitLabUtils
                    .showYesNoDialog(project, "Empty Pull Request",
                            "The branch " + localBranchName + " is fully merged to the branch " + targetBranchName + '\n' +
                                    "Do you want to proceed anyway?");
        }
        if (!info.getInfo().getHeadToBranchCommits(gitRepository).isEmpty()) {
            return GitLabUtils
                    .showYesNoDialog(project, "Target Branch Is Not Fully Merged",
                            "The branch " + targetBranchName + " is not fully merged to the branch " + localBranchName + '\n' +
                                    "Do you want to proceed anyway?");
        }

        return true;
    }


    public static GitLabCreateMergeRequestWorker create(@NotNull final Project project, @Nullable final VirtualFile file) {
        return GitLabUtils.computeValueInModal(project, "Loading data...", new Convertor<ProgressIndicator, GitLabCreateMergeRequestWorker>() {

            @Override
            public GitLabCreateMergeRequestWorker convert(ProgressIndicator indicator) {
                GitLabCreateMergeRequestWorker mergeRequestWorker = new GitLabCreateMergeRequestWorker();

                try {
                    Util.fillRequiredInfo(mergeRequestWorker, project, file);
                } catch (MergeRequestException e) {
                    return null;
                }

                //region Additional fields
                GitLocalBranch currentBranch = mergeRequestWorker.getGitRepository().getCurrentBranch();
                if (currentBranch == null) {
                    Messages.showErrorDialog(project, "No current branch", CANNOT_CREATE_MERGE_REQUEST);
                    return null;
                }
                mergeRequestWorker.setGitLocalBranch(currentBranch);

                String lastMergedBranch = mergeRequestWorker.getProjectState().getLastMergedBranch();

                try {
                    List<GitlabBranch> branches = settingsState.api().loadProjectBranches(mergeRequestWorker.getGitlabProject());
                    List<BranchInfo> branchInfos = new ArrayList<>();
                    for (GitlabBranch branch : branches) {
                        BranchInfo branchInfo = new BranchInfo(branch.getName(), mergeRequestWorker.getRemoteProjectName());
                        if (branch.getName().equals(lastMergedBranch)) {
                            mergeRequestWorker.setLastUsedBranch(branchInfo);
                        }
                        branchInfos.add(branchInfo);
                    }
                    mergeRequestWorker.setBranches(branchInfos);
                } catch (Exception e) {
                    Messages.showErrorDialog(project, "Cannot list GitLab branches", CANNOT_CREATE_MERGE_REQUEST);
                    return null;
                }

                mergeRequestWorker.setLocalBranchInfo(new BranchInfo(mergeRequestWorker.getGitLocalBranch().getName(),mergeRequestWorker.getRemoteProjectName(), false));
                //endregion

                return mergeRequestWorker;
            }

        });

    }

    //region Getters & Setters
    public Git getGit() {
        return git;
    }

    public Project getProject() {
        return project;
    }

    public ProjectState getProjectState() {
        return projectState;
    }

    public GitRepository getGitRepository() {
        return gitRepository;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public GitlabProject getGitlabProject() {
        return gitlabProject;
    }

    @Override
    public String getRemoteProjectName() {
        return remoteProjectName;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setProjectState(ProjectState projectState) {
        this.projectState = projectState;
    }

    public void setGitRepository(GitRepository gitRepository) {
        this.gitRepository = gitRepository;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public void setGitlabProject(GitlabProject gitlabProject) {
        this.gitlabProject = gitlabProject;
    }

    @Override
    public void setRemoteProjectName(String remoteProjectName) {
        this.remoteProjectName = remoteProjectName;
    }

    public GitLocalBranch getGitLocalBranch() {
        return gitLocalBranch;
    }

    public void setGitLocalBranch(GitLocalBranch gitLocalBranch) {
        this.gitLocalBranch = gitLocalBranch;
    }

    public BranchInfo getLocalBranchInfo() {
        return localBranchInfo;
    }

    public void setLocalBranchInfo(BranchInfo localBranchInfo) {
        this.localBranchInfo = localBranchInfo;
    }

    public List<BranchInfo> getBranches() {
        return branches;
    }

    public void setBranches(List<BranchInfo> branches) {
        this.branches = branches;
    }

    public BranchInfo getLastUsedBranch() {
        return lastUsedBranch;
    }

    public void setLastUsedBranch(BranchInfo lastUsedBranch) {
        this.lastUsedBranch = lastUsedBranch;
    }

    public GitLabDiffViewWorker getDiffViewWorker() {
        return diffViewWorker;
    }

    public void setDiffViewWorker(GitLabDiffViewWorker diffViewWorker) {
        this.diffViewWorker = diffViewWorker;
    }
    //endregion
}
