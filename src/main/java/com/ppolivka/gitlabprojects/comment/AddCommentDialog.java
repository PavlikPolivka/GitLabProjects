package com.ppolivka.gitlabprojects.comment;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.containers.Convertor;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import org.apache.commons.lang.StringUtils;
import org.gitlab.api.models.GitlabMergeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;

/**
 * Dialog for adding comments
 *
 * @author ppolivka
 * @since 1.3.2
 */
public class AddCommentDialog extends DialogWrapper {

  static SettingsState settingsState = SettingsState.getInstance();

  private JPanel panel;
  private JTextArea commentText;

  private Project project;
  private GitlabMergeRequest mergeRequest;

  protected AddCommentDialog(@Nullable Project project, @NotNull GitlabMergeRequest mergeRequest) {
    super(project);
    this.project = project;
    this.mergeRequest = mergeRequest;
    init();
  }

  @Override
  protected void init() {
    super.init();
    setTitle("Add Comment");
    setOKButtonText("Add");
  }

  @Override
  protected void doOKAction() {
    super.doOKAction();
    GitLabUtil.computeValueInModal(project, "Adding comment...", (Convertor<ProgressIndicator, Void>) indicator -> {
      String comment = commentText.getText();
      if (StringUtils.isNotBlank(comment)) {
        try {
          settingsState.api().addComment(mergeRequest, comment);
        } catch (IOException e) {
          showErrorDialog(project, "Cannot add comment.", "Cannot Add Comment");
        }
      }
      return null;
    });
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    if(StringUtils.isBlank(commentText.getText())) {
      return new ValidationInfo("Comment text cannot be empty.", commentText);
    }
    return super.doValidate();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }
}
