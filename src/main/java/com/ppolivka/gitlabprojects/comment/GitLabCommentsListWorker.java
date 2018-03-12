package com.ppolivka.gitlabprojects.comment;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.Convertor;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabNote;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;

/**
 * Worker for extracting comments on merge request
 *
 * @author ppolivka
 * @since 1.3.2
 */
public class GitLabCommentsListWorker {

  static SettingsState settingsState = SettingsState.getInstance();

  GitlabMergeRequest mergeRequest;
  List<GitlabNote> comments;
  VirtualFile file;

  public GitlabMergeRequest getMergeRequest() {
    return mergeRequest;
  }

  public void setMergeRequest(GitlabMergeRequest mergeRequest) {
    this.mergeRequest = mergeRequest;
  }

  public List<GitlabNote> getComments() {
    return comments;
  }

  public void setComments(List<GitlabNote> comments) {
    this.comments = comments;
  }

  public static GitLabCommentsListWorker create(@NotNull final Project project, @NotNull final GitlabMergeRequest mergeRequest, final VirtualFile file) {
    return GitLabUtil.computeValueInModal(project, "Loading comments...", (Convertor<ProgressIndicator, GitLabCommentsListWorker>) indicator -> {
      GitLabCommentsListWorker commentsListWorker = new GitLabCommentsListWorker();
      commentsListWorker.setMergeRequest(mergeRequest);
      try {
        commentsListWorker.setComments(settingsState.api(project, file).getMergeRequestComments(mergeRequest));
      } catch (IOException e) {
        commentsListWorker.setComments(Collections.<GitlabNote>emptyList());
        showErrorDialog(project, "Cannot load comments from GitLab API", "Cannot Load Comments");
      }

      return commentsListWorker;
    });
  }

}
