package com.ppolivka.gitlabprojects.util;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThrowableConvertor;
import com.intellij.util.containers.Convertor;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;
import git4idea.config.GitVcsApplicationSettings;
import git4idea.config.GitVersion;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;

/**
 * GitLab specific untils
 *
 * @author ppolivka
 * @since 28.10.2015
 */
@SuppressWarnings("Duplicates")
public class GitLabUtil {

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
        for (GitRemote gitRemote : repository.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                if (isGitLabUrl(remoteUrl)) {
                  return Pair.create(gitRemote, gitRemote.getName());
                }
            }
        }
        return null;
    }

    public static boolean isGitLabUrl(String url) {
      try {
        URI fromSettings = new URI(settingsState.getHost());
        String fromSettingsHost = fromSettings.getHost();

        String patternString = "(\\w+://)(.+@)*([\\w\\d\\.\\-]+)(:[\\d]+){0,1}/*(.*)|(.+@)*([\\w\\d\\.\\-]+):(.*)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(url);
        String fromUrlHost = "";
        if(matcher.matches()) {
          String group3 = matcher.group(3);
          String group7 = matcher.group(7);
          if(StringUtils.isNotEmpty(group3)) {
            fromUrlHost = group3;
          } else if (StringUtils.isNotEmpty(group7)) {
            fromUrlHost = group7;
          }
        }
        return fromSettingsHost != null && removeNotAlpha(fromSettingsHost).equals(removeNotAlpha(fromUrlHost));
      } catch (Exception e) {
        return false;
      }
    }

    public static String removeNotAlpha(String input) {
        input = input.replaceAll("[^a-zA-Z0-9]", "");
        input = input.toLowerCase();
        return input;
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
                showErrorDialog(project, "New remote origin cannot be added to this project.", "Cannot Add New Remote");
                return false;
            }
            // catch newly added remote
            repository.update();
            return true;
        } catch (VcsException e) {
            showErrorDialog(project, "New remote origin cannot be added to this project.", "Cannot Add New Remote");
            return false;
        }
    }

    public static boolean testGitExecutable(final Project project) {
        final GitVcsApplicationSettings settings = GitVcsApplicationSettings.getInstance();
        final String executable = settings.getPathToGit();
        final GitVersion version;
        try {
            version = GitVersion.identifyVersion(executable);
        } catch (Exception e) {
            showErrorDialog(project, "Cannot find git executable.", "Cannot Find Git");
            return false;
        }

        if (!version.isSupported()) {
            showErrorDialog(project, "Your version of git is not supported.", "Cannot Find Git");
            return false;
        }
        return true;
    }

    public static <T> T computeValueInModal(@NotNull Project project,
                                            @NotNull String caption,
                                            @NotNull final ThrowableConvertor<ProgressIndicator, T, IOException> task) throws IOException {
        final Ref<T> dataRef = new Ref<T>();
        final Ref<Throwable> exceptionRef = new Ref<Throwable>();
        ProgressManager.getInstance().run(new Task.Modal(project, caption, true) {
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    dataRef.set(task.convert(indicator));
                } catch (Throwable e) {
                    exceptionRef.set(e);
                }
            }
        });
        if (!exceptionRef.isNull()) {
            Throwable e = exceptionRef.get();
            if (e instanceof IOException) {
                throw ((IOException) e);
            }
            if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            }
            if (e instanceof Error) {
                throw ((Error) e);
            }
            throw new RuntimeException(e);
        }
        return dataRef.get();
    }

    public static <T> T computeValueInModal(@NotNull Project project,
                                            @NotNull String caption,
                                            @NotNull final Convertor<ProgressIndicator, T> task) {
        return computeValueInModal(project, caption, true, task);
    }

    public static <T> T computeValueInModal(@NotNull Project project,
                                            @NotNull String caption,
                                            boolean canBeCancelled,
                                            @NotNull final Convertor<ProgressIndicator, T> task) {
        final Ref<T> dataRef = new Ref<T>();
        final Ref<Throwable> exceptionRef = new Ref<Throwable>();
        ProgressManager.getInstance().run(new Task.Modal(project, caption, canBeCancelled) {
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    dataRef.set(task.convert(indicator));
                } catch (Throwable e) {
                    exceptionRef.set(e);
                }
            }
        });
        if (!exceptionRef.isNull()) {
            Throwable e = exceptionRef.get();
            if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            }
            if (e instanceof Error) {
                throw ((Error) e);
            }
            throw new RuntimeException(e);
        }
        return dataRef.get();
    }

    public static <T> T runInterruptable(@NotNull final ProgressIndicator indicator,
                                         @NotNull ThrowableComputable<T, IOException> task) throws IOException {
        ScheduledFuture<?> future = null;
        try {
            final Thread thread = Thread.currentThread();
            future = addCancellationListener(indicator, thread);

            return task.compute();
        } finally {
            if (future != null) {
                future.cancel(true);
            }
            Thread.interrupted();
        }
    }

    @NotNull
    private static ScheduledFuture<?> addCancellationListener(@NotNull final ProgressIndicator indicator,
                                                              @NotNull final Thread thread) {
        return addCancellationListener(new Runnable() {
            @Override
            public void run() {
                if (indicator.isCanceled()) {
                    thread.interrupt();
                }
            }
        });
    }

    @NotNull
    private static ScheduledFuture<?> addCancellationListener(@NotNull Runnable run) {
        return JobScheduler.getScheduler().scheduleWithFixedDelay(run, 1000, 300, TimeUnit.MILLISECONDS);
    }

    @Messages.YesNoResult
    public static boolean showYesNoDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
        return Messages.YES == Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon());
    }

}
