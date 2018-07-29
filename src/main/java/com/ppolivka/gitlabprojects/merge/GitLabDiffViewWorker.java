package com.ppolivka.gitlabprojects.merge;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.util.ThrowableConvertor;
import com.ppolivka.gitlabprojects.merge.info.BranchInfo;
import com.ppolivka.gitlabprojects.merge.info.DiffInfo;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import git4idea.GitCommit;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.ui.branch.GitCompareBranchesDialog;
import git4idea.update.GitFetchResult;
import git4idea.update.GitFetcher;
import git4idea.util.GitCommitCompareInfo;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.PooledThreadExecutor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;

/**
 * Worker class that helps to calculate diff between two branches
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class GitLabDiffViewWorker {

    private Project project;
    private GitRepository gitRepository;

    GitLabDiffViewWorker(Project project, GitRepository gitRepository) {
        this.project = project;
        this.gitRepository = gitRepository;
    }

    private static String CANNOT_SHOW_DIFF_INFO = "Cannot Show Diff Info";


    public void showDiffDialog(@NotNull final BranchInfo from, @NotNull final BranchInfo branch) {
        DiffInfo info;
        try {
            info = GitLabUtil
                    .computeValueInModal(project, "Collecting diff data...", new ThrowableConvertor<ProgressIndicator, DiffInfo, IOException>() {
                        @Override
                        public DiffInfo convert(ProgressIndicator indicator) throws IOException {
                            return GitLabUtil.runInterruptable(indicator, () -> getDiffInfo(from, branch));
                        }
                    });
        } catch (IOException e) {
            showErrorDialog(project, "Can't collect diff data", CANNOT_SHOW_DIFF_INFO);
            return;
        }
        if (info == null) {
            showErrorDialog(project, "Can't collect diff data", CANNOT_SHOW_DIFF_INFO);
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

        try {
            return launchLoadDiffInfo(from, branch).get();
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

    public CompletableFuture<DiffInfo> launchLoadDiffInfo(@NotNull final BranchInfo from, @NotNull final BranchInfo branch) {
        if (branch.getName() == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Boolean> fetchFuture = launchFetchRemote(branch);
        return fetchFuture.thenApply(t -> doLoadDiffInfo(from, branch));
    }

    private CompletableFuture<Boolean> launchFetchRemote(@NotNull final BranchInfo branch) {
        if (branch.getName() == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> doFetchRemote(branch), PooledThreadExecutor.INSTANCE);
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
    @SneakyThrows(VcsException.class)
    private DiffInfo doLoadDiffInfo(@NotNull final BranchInfo from, @NotNull final BranchInfo to) {
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
