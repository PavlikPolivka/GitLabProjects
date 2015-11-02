package com.ppolivka.gitlabprojects.merge;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.util.ThrowableConvertor;
import com.ppolivka.gitlabprojects.common.GitLabUtils;
import com.ppolivka.gitlabprojects.common.MasterFutureTask;
import com.ppolivka.gitlabprojects.common.SlaveFutureTask;
import com.ppolivka.gitlabprojects.merge.info.BranchInfo;
import com.ppolivka.gitlabprojects.merge.info.DiffInfo;
import git4idea.GitCommit;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.ui.branch.GitCompareBranchesDialog;
import git4idea.update.GitFetchResult;
import git4idea.update.GitFetcher;
import git4idea.util.GitCommitCompareInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * TODO:Descibe
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class GitLabDiffViewWorker {

    Project project;
    GitRepository gitRepository;

    public GitLabDiffViewWorker(Project project, GitRepository gitRepository) {
        this.project = project;
        this.gitRepository = gitRepository;
    }

    public static String CANNOT_SHOW_DIFF_INFO = "Cannot Show Diff Info";


    public void showDiffDialog(@NotNull final BranchInfo from, @NotNull final BranchInfo branch) {
        DiffInfo info;
        try {
            info = GitLabUtils
                    .computeValueInModal(project, "Collecting diff data...", new ThrowableConvertor<ProgressIndicator, DiffInfo, IOException>() {
                        @Override
                        public DiffInfo convert(ProgressIndicator indicator) throws IOException {
                            return GitLabUtils.runInterruptable(indicator, new ThrowableComputable<DiffInfo, IOException>() {
                                @Override
                                public DiffInfo compute() throws IOException {
                                    return getDiffInfo(from, branch);
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
    public DiffInfo getDiffInfo(@NotNull final BranchInfo from, @NotNull final BranchInfo branch) throws IOException {
        if (branch.getName() == null) {
            return null;
        }

        launchLoadDiffInfo(from, branch);

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

    public void launchLoadDiffInfo(@NotNull final BranchInfo from, @NotNull final BranchInfo branch) {
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

            final SlaveFutureTask<DiffInfo> task = new SlaveFutureTask<>(masterTask, new Callable<DiffInfo>() {
                @Override
                public DiffInfo call() throws VcsException {
                    return doLoadDiffInfo(from, branch);
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
    private DiffInfo doLoadDiffInfo(@NotNull final BranchInfo from, @NotNull final BranchInfo to) throws VcsException {
        String currentBranch = from.getFullName();
        String targetBranch = to.getFullRemoteName();

        List<GitCommit> commits1 = GitHistoryUtils.history(project, gitRepository.getRoot(), ".." + targetBranch);
        List<GitCommit> commits2 = GitHistoryUtils.history(project, gitRepository.getRoot(), targetBranch + "..");
        Collection<Change> diff = GitChangeUtils.getDiff(project, gitRepository.getRoot(), targetBranch, currentBranch, null);
        GitCommitCompareInfo info = new GitCommitCompareInfo(GitCommitCompareInfo.InfoType.BRANCH_TO_HEAD);
        info.put(gitRepository, diff);
        info.put(gitRepository, Couple.of(commits1, commits2));

        return new DiffInfo(info, currentBranch, targetBranch);
    }

}
