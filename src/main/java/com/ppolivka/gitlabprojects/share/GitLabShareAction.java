package com.ppolivka.gitlabprojects.share;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.ppolivka.gitlabprojects.api.dto.NamespaceDto;
import com.ppolivka.gitlabprojects.common.GitLabApiAction;
import com.ppolivka.gitlabprojects.common.GitLabIcons;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.actions.BasicAction;
import git4idea.actions.GitInit;
import git4idea.commands.*;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.util.GitFileUtils;
import git4idea.util.GitUIUtil;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;
import static com.ppolivka.gitlabprojects.util.MessageUtil.showInfoMessage;

/**
 * Import to VCS project to gitlab
 *
 * @author ppolivka
 * @since 28.10.2015
 */
public class GitLabShareAction extends GitLabApiAction {

    private static SettingsState settingsState = SettingsState.getInstance();

    public GitLabShareAction() {
        super("Share on GitLab", "Easy share on your GitLab server", GitLabIcons.gitLabIcon);
    }

    @Override
    public void apiValidAction(AnActionEvent anActionEvent) {

        if (project.isDisposed()) {
            return;
        }

        shareProjectOnGitLab(project, file);
    }

    public void shareProjectOnGitLab(@NotNull final Project project, @Nullable final VirtualFile file) {
        BasicAction.saveAll();

        // get gitRepository
        final GitRepository gitRepository = GitLabUtil.getGitRepository(project, file);
        final boolean gitDetected = gitRepository != null;
        final VirtualFile root = gitDetected ? gitRepository.getRoot() : project.getBaseDir();

        if (gitDetected) {
            final String gitLabRemoteUrl = GitLabUtil.findGitLabRemoteUrl(gitRepository);
            if (gitLabRemoteUrl != null) {
                showInfoMessage(project, "This project already has remote to your GitLab Server", "Already Git Lab Project");
            }
        }
        GitLabShareDialog gitLabShareDialog = new GitLabShareDialog(project);
        gitLabShareDialog.show();
        if (!gitLabShareDialog.isOK()) {
            return;
        }
        final String name = gitLabShareDialog.getProjectName().getText();
        final String commitMessage = gitLabShareDialog.getCommitMessage().getText();
        final NamespaceDto namespace = (NamespaceDto) gitLabShareDialog.getGroupList().getSelectedItem();
        int visibility_level = 10;
        boolean isPublic = false;
        if (gitLabShareDialog.getIsPrivate().isSelected()) {
            visibility_level = 0;
        }
        if (gitLabShareDialog.getIsPublic().isSelected()) {
            visibility_level = 20;
            isPublic = true;
        }
        final int visibility = visibility_level;
        final boolean publicity = isPublic;

        boolean isSsh = true;
        if (gitLabShareDialog.getIsHTTPAuth().isSelected()) {
            isSsh = false;
        }
        final boolean authSsh = isSsh;

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Sharing to GitLab...") {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GitlabProject gitlabProject;
                try {
                    indicator.setText("Creating GitLab Repository");
                    gitlabProject = settingsState.api().createProject(name, visibility, publicity, namespace, "");
                } catch (IOException e) {
                    return;
                }

                if (!gitDetected) {
                    indicator.setText("Creating empty git repo...");
                    if (!createEmptyGitRepository(project, root, indicator)) {
                        return;
                    }
                }

                GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
                final GitRepository repository = repositoryManager.getRepositoryForRoot(root);
                if (repository == null) {
                    showErrorDialog(project, "Remote server was not found.", "Remote Not Found");
                    return;
                }

                final String remoteUrl = authSsh ? gitlabProject.getSshUrl() : gitlabProject.getHttpUrl();

                indicator.setText("Adding GitLAb as a remote host...");
                if (!GitLabUtil.addGitLabRemote(project, repository, name, remoteUrl)) {
                    return;
                }

                if (!performFirstCommitIfRequired(project, root, repository, indicator, remoteUrl, commitMessage)) {
                    return;
                }

                indicator.setText("Pushing to gitlab master...");
                if (!pushCurrentBranch(project, repository, name, remoteUrl, name, remoteUrl)) {
                    return;
                }

                showInfoMessage(project, "Project was shared to your GitLab server", "Project Shared");

            }
        });


    }

    private static boolean performFirstCommitIfRequired(@NotNull final Project project,
                                                        @NotNull VirtualFile root,
                                                        @NotNull GitRepository repository,
                                                        @NotNull ProgressIndicator indicator,
                                                        @NotNull String url,
                                                        @NotNull String commitMessage) {
        // check if there is no commits
        if (!repository.isFresh()) {
            return true;
        }

        try {
            indicator.setText("Adding files to git...");

            // ask for files to add
            final List<VirtualFile> trackedFiles = ChangeListManager.getInstance(project).getAffectedFiles();
            final Collection<VirtualFile> untrackedFiles =
                    filterOutIgnored(project, repository.getUntrackedFilesHolder().retrieveUntrackedFiles());
            untrackedFiles.removeAll(trackedFiles);

            GitFileUtils.addFiles(project, root, untrackedFiles);

            indicator.setText("Performing commit...");
            GitSimpleHandler handler = new GitSimpleHandler(project, root, GitCommand.COMMIT);
            handler.setStdoutSuppressed(false);
            handler.addParameters("-m", commitMessage);
            handler.endOptions();
            handler.run();
        } catch (VcsException e) {
            showErrorDialog(project, "Project was create on GitLab server, but files cannot be commited to it.", "Initial Commit Failure");
            return false;
        }
        return true;
    }

    @NotNull
    private static Collection<VirtualFile> filterOutIgnored(@NotNull Project project, @NotNull Collection<VirtualFile> files) {
        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        return ContainerUtil.filter(files, new Condition<VirtualFile>() {
            @Override
            public boolean value(VirtualFile file) {
                return !changeListManager.isIgnoredFile(file) && !vcsManager.isIgnored(file);
            }
        });
    }

    private static boolean createEmptyGitRepository(@NotNull Project project,
                                                    @NotNull VirtualFile root,
                                                    @NotNull ProgressIndicator indicator) {
        final GitLineHandler h = new GitLineHandler(project, root, GitCommand.INIT);
        h.setStdoutSuppressed(false);
        GitHandlerUtil.runInCurrentThread(h, indicator, true, GitBundle.getString("initializing.title"));
        if (!h.errors().isEmpty()) {
            GitUIUtil.showOperationErrors(project, h.errors(), "git init");
            return false;
        }
        GitInit.refreshAndConfigureVcsMappings(project, root, root.getPath());
        return true;
    }

    private static boolean pushCurrentBranch(@NotNull Project project,
                                             @NotNull GitRepository repository,
                                             @NotNull String remoteName,
                                             @NotNull String remoteUrl,
                                             @NotNull String name,
                                             @NotNull String url) {
        Git git = ServiceManager.getService(Git.class);

        GitLocalBranch currentBranch = repository.getCurrentBranch();
        if (currentBranch == null) {
            showErrorDialog(project, "Project was create on GitLAb server, but cannot be pushed.", "Cannot Be Pushed");
            return false;
        }
        GitCommandResult result = git.push(repository, remoteName, remoteUrl, currentBranch.getName(), true);
        if (!result.success()) {
            showErrorDialog(project, "Project was create on GitLab server, but cannot be pushed.", "Cannot Be Pushed");
            return false;
        }
        return true;
    }
}
