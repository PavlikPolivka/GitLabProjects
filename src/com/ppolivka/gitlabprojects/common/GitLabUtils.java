package com.ppolivka.gitlabprojects.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitLab specific untils
 *
 * @author ppolivka
 * @since 28.10.2015
 */
public class GitLabUtils {

    private static SettingsState settingsState = SettingsState.getInstance();

    @Nullable
    public static GitRepository getGitRepository(@NotNull Project project, @Nullable VirtualFile file) {
        GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
        List<GitRepository> repositories = manager.getRepositories();
        if (repositories.size() == 0) {
            return null;
        }
        if (repositories.size() == 1) {
            return repositories.get(0);
        }
        if (file != null) {
            GitRepository repository = manager.getRepositoryForFile(file);
            if (repository != null) {
                return repository;
            }
        }
        return manager.getRepositoryForFile(project.getBaseDir());
    }

    @Nullable
    public static String findGitLabRemoteUrl(@NotNull GitRepository repository) {
        Pair<GitRemote, String> remote = findGitLabRemote(repository);
        if (remote == null) {
            return null;
        }
        return remote.getSecond();
    }

    @Nullable
    public static Pair<GitRemote, String> findGitLabRemote(@NotNull GitRepository repository) {
        Pair<GitRemote, String> gitlabRemote = null;
        for (GitRemote gitRemote : repository.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                if (isGitLabUrl(remoteUrl)) {
                    final String remoteName = gitRemote.getName();
                    if ("gitlab".equals(remoteName) || "origin".equals(remoteName)) {
                        return Pair.create(gitRemote, remoteUrl);
                    }
                    if (gitlabRemote == null) {
                        gitlabRemote = Pair.create(gitRemote, remoteUrl);
                    }
                    break;
                }
            }
        }
        return gitlabRemote;
    }

    public static boolean isGitLabUrl(String url) {
        Pattern urlPattern = Pattern.compile("^(https?)://([-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])");
        Matcher matcher = urlPattern.matcher(settingsState.getHost());
        return (matcher.matches() && url.contains(matcher.group(2)));
    }

    public static boolean addGitLabRemote(@NotNull Project project,
                                          @NotNull GitRepository repository,
                                          @NotNull String remote,
                                          @NotNull String url) {
        final GitSimpleHandler handler = new GitSimpleHandler(project, repository.getRoot(), GitCommand.REMOTE);
        handler.setSilent(true);

        try {
            handler.addParameters("add", remote, url);
            handler.run();
            if (handler.getExitCode() != 0) {
                Messages.showErrorDialog(project, "New remote origin cannot be added to this project.", "Cannot Add New Remote");
                return false;
            }
            // catch newly added remote
            repository.update();
            return true;
        } catch (VcsException e) {
            Messages.showErrorDialog(project, "New remote origin cannot be added to this project.", "Cannot Add New Remote");
            return false;
        }
    }

}
