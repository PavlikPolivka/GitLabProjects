package com.ppolivka.gitlabprojects.merge;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.ppolivka.gitlabprojects.configuration.ProjectState;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.exception.MergeRequestException;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import git4idea.commands.Git;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;

/**
 * Interface for worker classes that are related to merge requests
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public interface GitLabMergeRequestWorker {

  String CANNOT_CREATE_MERGE_REQUEST = "Cannot Create Merge Request";

  Git getGit();

  Project getProject();

  ProjectState getProjectState();

  GitRepository getGitRepository();

  String getRemoteUrl();

  GitlabProject getGitlabProject();

  String getRemoteProjectName();

  GitLabDiffViewWorker getDiffViewWorker();

  void setGit(Git git);

  void setProject(Project project);

  void setProjectState(ProjectState projectState);

  void setGitRepository(GitRepository gitRepository);

  void setRemoteUrl(String remoteUrl);

  void setGitlabProject(GitlabProject gitlabProject);

  void setRemoteProjectName(String remoteProjectName);

  void setDiffViewWorker(GitLabDiffViewWorker diffViewWorker);

  class Util {

    private static SettingsState settingsState = SettingsState.getInstance();

    public static void fillRequiredInfo(@NotNull final GitLabMergeRequestWorker mergeRequestWorker, @NotNull final Project project, @Nullable final VirtualFile file) throws MergeRequestException {
      ProjectState projectState = ProjectState.getInstance(project);
      mergeRequestWorker.setProjectState(projectState);

      mergeRequestWorker.setProject(project);

      Git git = ServiceManager.getService(Git.class);
      mergeRequestWorker.setGit(git);

      GitRepository gitRepository = GitLabUtil.getGitRepository(project, file);
      if (gitRepository == null) {
        showErrorDialog(project, "Can't find git repository", CANNOT_CREATE_MERGE_REQUEST);
        throw new MergeRequestException();
      }
      gitRepository.update();
      mergeRequestWorker.setGitRepository(gitRepository);

      Pair<GitRemote, String> remote = GitLabUtil.findGitLabRemote(gitRepository);
      if (remote == null) {
        showErrorDialog(project, "Can't find GitLab remote", CANNOT_CREATE_MERGE_REQUEST);
        throw new MergeRequestException();
      }

      String remoteProjectName = remote.first.getName();
      mergeRequestWorker.setRemoteProjectName(remoteProjectName);
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
          showErrorDialog(project, "Cannot find this project in GitLab Remote", CANNOT_CREATE_MERGE_REQUEST);
          throw new MergeRequestException(throwable);
        }
      }
      try {
        mergeRequestWorker.setGitlabProject(settingsState.api().getProject(projectId));
      } catch (Exception e) {
        showErrorDialog(project, "Cannot find this project in GitLab Remote", CANNOT_CREATE_MERGE_REQUEST);
        throw new MergeRequestException(e);
      }

      mergeRequestWorker.setDiffViewWorker(new GitLabDiffViewWorker(project, mergeRequestWorker.getGitRepository()));
    }
  }

}
