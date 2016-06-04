package com.ppolivka.gitlabprojects.comment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Date;

/**
 * Dialog listing details of one comment
 *
 * @author ppolivka
 * @since 1.3.2
 */
public class CommentDetail extends DialogWrapper {
  private JPanel panel;
  private JLabel authorName;
  private JLabel dateText;
  private JTextArea bodyText;

  protected CommentDetail(@Nullable Project project, @NotNull String name, @NotNull Date date, @NotNull String body) {
    super(project);
    init();
    setTitle("Comment Detail");
    authorName.setText(name);
    dateText.setText(date.toString());
    bodyText.setText(body);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }
}
