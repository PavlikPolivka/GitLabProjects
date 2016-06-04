package com.ppolivka.gitlabprojects.comment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.ppolivka.gitlabprojects.common.ReadOnlyTableModel;
import git4idea.DialogManager;
import org.gitlab.api.models.GitlabNote;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;

/**
 * Dialog for listing comments
 *
 * @author ppolivka
 * @since 1.3.2
 */
public class CommentsDialog extends DialogWrapper {

  private JPanel panel;
  private JTable comments;
  private JButton addCommentButton;

  private Project project;
  private GitLabCommentsListWorker worker;

  public CommentsDialog(@Nullable Project project, GitLabCommentsListWorker worker) {
    super(project);
    this.project = project;
    this.worker = worker;
    init();
  }

  @Override
  protected void init() {
    super.init();

    setTitle("Comments");

    reloadModel();

    comments.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent me) {
        if (me.getClickCount() == 2) {
          String name = (String) comments.getValueAt(comments.getSelectedRow(), 0);
          Date date = (Date) comments.getValueAt(comments.getSelectedRow(), 1);
          String body = (String) comments.getValueAt(comments.getSelectedRow(), 2);
          DialogManager.show(new CommentDetail(project, name, date, body));
        }
      }
    });

    addCommentButton.addActionListener(e -> {
      new AddCommentDialog(project, worker.getMergeRequest()).show();
      this.worker = GitLabCommentsListWorker.create(project, worker.getMergeRequest());
      reloadModel();
      comments.repaint();
    });

  }

  private void reloadModel() {
    comments.setModel(commentsModel(worker.getComments()));
    comments.getColumnModel().getColumn(0).setPreferredWidth(100);
    comments.getColumnModel().getColumn(1).setPreferredWidth(150);
    comments.getColumnModel().getColumn(2).setPreferredWidth(400);
    comments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }

  private TableModel commentsModel(List<GitlabNote> notes) {
    Object[] columnNames = {"Author", "Date", "Text"};
    Object[][] data = new Object[notes.size()][columnNames.length];
    int i = 0;
    notes.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
    for(GitlabNote mergeRequest : notes) {
      Object[] row = new Object[columnNames.length];
      row[0] = mergeRequest.getAuthor().getName();
      row[1] = mergeRequest.getCreatedAt();
      row[2] = mergeRequest.getBody();
      data[i] = row;
      i++;
    }
    return new ReadOnlyTableModel(data, columnNames);
  }
}
