package com.ppolivka.gitlabprojects.merge;

import com.intellij.notification.NotificationListener;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.ppolivka.gitlabprojects.common.messages.Messages;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThrowableConvertor;
import com.intellij.util.containers.Convertor;
import com.ppolivka.gitlabprojects.common.GitLabUtils;
import com.ppolivka.gitlabprojects.configuration.ProjectState;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import git4idea.GitCommit;
import git4idea.GitLocalBranch;
import git4idea.changes.GitChangeUtils;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.ui.branch.GitCompareBranchesDialog;
import git4idea.update.GitFetchResult;
import git4idea.update.GitFetcher;
import git4idea.util.GitCommitCompareInfo;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * GitLab Merge request worker
 *
 * @author ppolivka
 * @since 30.10.2015
 */
public class GitLabMergeRequestWorker {

    private static final String CANNOT_CREATE_MERGE_REQUEST = "Cannot Create Merge Request";
    private static final String CANNOT_SHOW_DIFF_INFO = "Cannot Show Diff Info";

    private static SettingsState settingsState = SettingsState.getInstance();

    private Git git;
    private Project project;
    private ProjectState projectState;
    private GitRepository gitRepository;
    private GitLocalBranch gitLocalBranch;
    private String remoteUrl;
    private GitlabProject gitlabProject;
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
                                    return getDiffInfo(branch);
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

    public void showDiffDialog(@Nullable final BranchInfo branch) {

        if (branch == null) {
            Messages.showErrorDialog(project, "Target branch is not selected", CANNOT_SHOW_DIFF_INFO);
            return;
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
                                    return getDiffInfo(branch);
                                }
                            });
                        }
                    });
        } catch (IOException e) {
            Messages.showErrorDialog(project, "Can't collect diff data", CANNOT_SHOW_DIFF_INFO);
            return;
        }
        if (info == null) {
            Messages.showErrorDialog(project, "Can't collect diff data", CANNOT_SHOW_DIFF_INFO);
            return;
        }

        GitCompareBranchesDialog dialog = new GitCompareBranchesDialog(project, info.getTo(), info.getFrom(), info.getInfo(), gitRepository, true);
        dialog.show();

    }

    @Nullable
    public DiffInfo getDiffInfo(@NotNull final BranchInfo branch) throws IOException {
        if (branch.getName() == null) {
            return null;
        }

        launchLoadDiffInfo(branch);

        assert branch.getDiffInfoTask() != null;
        try {
            return branch.getDiffInfoTask().get();
        } catch (InterruptedException e) {
            throw new IOException(e);
        } catch (ExecutionException e) {
            Throwable ex = e.getCause();
            if (ex instanceof VcsException) {
                throw new IOException(ex);
            }
            return null;
        }
    }

    public void launchLoadDiffInfo(@NotNull final BranchInfo branch) {
        if (branch.getName() == null) {
            return;
        }

        if (branch.getDiffInfoTask() != null) {
            return;
        }
        synchronized (branch.LOCK) {
            if (branch.getDiffInfoTask() != null) {
                return;
            }

            launchFetchRemote(branch);
            MasterFutureTask<Void> masterTask = branch.getFetchTask();
            assert masterTask != null;

            final SlaveFutureTask<DiffInfo> task = new SlaveFutureTask<DiffInfo>(masterTask, new Callable<DiffInfo>() {
                @Override
                public DiffInfo call() throws VcsException {
                    return doLoadDiffInfo(branch);
                }
            });
            branch.setDiffInfoTask(task);

            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
    }

    public void launchFetchRemote(@NotNull final BranchInfo branch) {
        if (branch.getName() == null) {
            return;
        }

        if (branch.getFetchTask() != null) {
            return;
        }
        synchronized (branch.LOCK_FETCH) {
            if (branch.getFetchTask() != null) {
                return;
            }

            final MasterFutureTask<Void> task = new MasterFutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    doFetchRemote(branch);
                    return null;
                }
            });
            branch.setFetchTask(task);

            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
    }

    private boolean doFetchRemote(@NotNull BranchInfo branch) {
        if (branch.getName() == null) {
            return false;
        }

        GitFetchResult result =
                new GitFetcher(project, new EmptyProgressIndicator(), false).fetch(gitRepository.getRoot(), branch.getRemoteName(), null);
        if (!result.isSuccess()) {
            GitFetcher.displayFetchResult(project, result, null, result.getErrors());
            return false;
        }
        return true;
    }

    @NotNull
    private DiffInfo doLoadDiffInfo(@NotNull final BranchInfo branch) throws VcsException {
        String currentBranch = gitLocalBranch.getName();
        String targetBranch = branch.getRemoteName() + "/" + branch.getName();

        List<GitCommit> commits1 = GitHistoryUtils.history(project, gitRepository.getRoot(), ".." + targetBranch);
        List<GitCommit> commits2 = GitHistoryUtils.history(project, gitRepository.getRoot(), targetBranch + "..");
        Collection<Change> diff = GitChangeUtils.getDiff(project, gitRepository.getRoot(), targetBranch, currentBranch, null);
        GitCommitCompareInfo info = new GitCommitCompareInfo(GitCommitCompareInfo.InfoType.BRANCH_TO_HEAD);
        info.put(gitRepository, diff);
        info.put(gitRepository, Couple.of(commits1, commits2));

        return new DiffInfo(info, currentBranch, targetBranch);
    }

    public static GitLabMergeRequestWorker create(@NotNull final Project project, @Nullable final VirtualFile file) {
        return GitLabUtils.computeValueInModal(project, "Loading data...", new Convertor<ProgressIndicator, GitLabMergeRequestWorker>() {

            @Override
            public GitLabMergeRequestWorker convert(ProgressIndicator indicator) {
                GitLabMergeRequestWorker mergeRequestWorker = new GitLabMergeRequestWorker();

                ProjectState projectState = ProjectState.getInstance(project);
                mergeRequestWorker.setProjectState(projectState);

                mergeRequestWorker.setProject(project);

                Git git = ServiceManager.getService(Git.class);
                mergeRequestWorker.setGit(git);

                GitRepository gitRepository = GitLabUtils.getGitRepository(project, file);
                if (gitRepository == null) {
                    Messages.showErrorDialog(project, "Can't find git repository", CANNOT_CREATE_MERGE_REQUEST);
                    return null;
                }
                gitRepository.update();
                mergeRequestWorker.setGitRepository(gitRepository);

                Pair<GitRemote, String> remote = GitLabUtils.findGitLabRemote(gitRepository);
                if (remote == null) {
                    Messages.showErrorDialog(project, "Can't find GitHub remote", CANNOT_CREATE_MERGE_REQUEST);
                    return null;
                }

                GitLocalBranch currentBranch = gitRepository.getCurrentBranch();
                if (currentBranch == null) {
                    Messages.showErrorDialog(project, "No current branch", CANNOT_CREATE_MERGE_REQUEST);
                    return null;
                }
                mergeRequestWorker.setGitLocalBranch(currentBranch);

                String remoteProjectName = remote.first.getName();
                mergeRequestWorker.setRemoteUrl(remote.getSecond());

                String remoteUrl = remote.getFirst().getFirstUrl();

                Integer projectId = projectState.getProjectId();
                if (projectId == null) {
                    try {
                        Collection<GitlabProject> projects = settingsState.api().getProjects();
                        for (GitlabProject gitlabProject : projects) {
                            if (gitlabProject.getName().equals(remoteProjectName) || gitlabProject.getSshUrl().equals(remoteUrl) || gitlabProject.getHttpUrl().equals(remoteUrl)) {
                                projectId = gitlabProject.getId();
                                projectState.setProjectId(projectId);
                                break;
                            }
                        }
                    } catch (Throwable throwable) {
                        new Notifications.Bus().notify();
                        Messages.showErrorDialog(project, "Cannot find this project in GitLab Remote", CANNOT_CREATE_MERGE_REQUEST);
                        return null;
                    }
                }
                try {
                    mergeRequestWorker.setGitlabProject(settingsState.api().getProject(projectId));
                } catch (Exception e) {
                    Messages.showErrorDialog(project, "Cannot find this project in GitLab Remote", CANNOT_CREATE_MERGE_REQUEST);
                    return null;
                }

                String lastMergedBranch = projectState.getLastMergedBranch();

                try {
                    List<GitlabBranch> branches = settingsState.api().loadProjectBranches(mergeRequestWorker.getGitlabProject());
                    List<BranchInfo> branchInfos = new ArrayList<>();
                    for (GitlabBranch branch : branches) {
                        BranchInfo branchInfo = new BranchInfo(branch.getName(), remoteProjectName);
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


                return mergeRequestWorker;
            }

        });

    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public GitRepository getGitRepository() {
        return gitRepository;
    }

    public void setGitRepository(GitRepository gitRepository) {
        this.gitRepository = gitRepository;
    }

    public GitLocalBranch getGitLocalBranch() {
        return gitLocalBranch;
    }

    public void setGitLocalBranch(GitLocalBranch gitLocalBranch) {
        this.gitLocalBranch = gitLocalBranch;
    }

    public GitlabProject getGitlabProject() {
        return gitlabProject;
    }

    public void setGitlabProject(GitlabProject gitlabProject) {
        this.gitlabProject = gitlabProject;
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public ProjectState getProjectState() {
        return projectState;
    }

    public void setProjectState(ProjectState projectState) {
        this.projectState = projectState;
    }

    public static class BranchInfo {

        @NotNull
        public final Object LOCK = new Object();
        @NotNull
        public final Object LOCK_FETCH = new Object();

        private String name;
        private String remoteName;
        private MasterFutureTask<Void> fetchTask;
        private SlaveFutureTask<DiffInfo> diffInfoTask;

        public BranchInfo(String name, String remoteName) {
            this.name = name;
            this.remoteName = remoteName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRemoteName() {
            return remoteName;
        }

        public void setRemoteName(String remoteName) {
            this.remoteName = remoteName;
        }

        public SlaveFutureTask<DiffInfo> getDiffInfoTask() {
            return diffInfoTask;
        }

        public void setDiffInfoTask(@NotNull SlaveFutureTask<DiffInfo> diffInfoTask) {
            this.diffInfoTask = diffInfoTask;
        }

        public MasterFutureTask<Void> getFetchTask() {
            return fetchTask;
        }

        public void setFetchTask(MasterFutureTask<Void> fetchTask) {
            this.fetchTask = fetchTask;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BranchInfo)) {
                return false;
            }
            BranchInfo that = (BranchInfo) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    public static class DiffInfo {
        @NotNull
        private final GitCommitCompareInfo myInfo;
        @NotNull
        private final String myFrom;
        @NotNull
        private final String myTo;

        private DiffInfo(@NotNull GitCommitCompareInfo info, @NotNull String from, @NotNull String to) {
            myInfo = info;
            myFrom = from; // HEAD
            myTo = to;     // BASE
        }

        @NotNull
        public GitCommitCompareInfo getInfo() {
            return myInfo;
        }

        @NotNull
        public String getFrom() {
            return myFrom;
        }

        @NotNull
        public String getTo() {
            return myTo;
        }
    }

    public static class SlaveFutureTask<T> extends FutureTask<T> {
        @NotNull
        private final MasterFutureTask myMaster;

        public SlaveFutureTask(@NotNull MasterFutureTask master, @NotNull Callable<T> callable) {
            super(callable);
            myMaster = master;
        }

        @Override
        public void run() {
            if (myMaster.isDone()) {
                super.run();
            } else {
                if (!myMaster.addSlave(this)) {
                    super.run();
                }
            }
        }

        public T safeGet() {
            try {
                return super.get();
            } catch (InterruptedException e) {
                return null;
            } catch (CancellationException e) {
                return null;
            } catch (ExecutionException e) {
                return null;
            }
        }
    }

    public static class MasterFutureTask<T> extends FutureTask<T> {
        @NotNull
        private final Object LOCK = new Object();
        private boolean myDone = false;

        @Nullable
        private List<SlaveFutureTask> mySlaves;

        public MasterFutureTask(@NotNull Callable<T> callable) {
            super(callable);
        }

        boolean addSlave(@NotNull SlaveFutureTask slave) {
            if (isDone()) {
                return false;
            } else {
                synchronized (LOCK) {
                    if (myDone) {
                        return false;
                    }
                    if (mySlaves == null) {
                        mySlaves = new ArrayList<SlaveFutureTask>();
                    }
                    mySlaves.add(slave);
                    return true;
                }
            }
        }

        @Override
        protected void done() {
            synchronized (LOCK) {
                myDone = true;
                if (mySlaves != null) {
                    for (final SlaveFutureTask slave : mySlaves) {
                        runSlave(slave);
                    }
                    mySlaves = null;
                }
            }
        }

        protected void runSlave(@NotNull final SlaveFutureTask slave) {
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    slave.run();
                }
            });
        }
    }
}
