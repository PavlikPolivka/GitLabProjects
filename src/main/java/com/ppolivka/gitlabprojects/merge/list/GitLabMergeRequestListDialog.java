package com.ppolivka.gitlabprojects.merge.list;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.ppolivka.gitlabprojects.common.ReadOnlyTableModel;
import org.gitlab.api.models.GitlabMergeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.List;

/**
 * Dialog that is listing all active merge request in git lab repo
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class GitLabMergeRequestListDialog extends DialogWrapper {

    private JPanel mainView;
    private JTable listOfRequests;

    private Project project;

    final GitLabMergeRequestListWorker mergeRequestListWorker;

    public GitLabMergeRequestListDialog(@Nullable Project project, @NotNull GitLabMergeRequestListWorker mergeRequestListWorker) {
        super(project);
        this.project = project;
        this.mergeRequestListWorker = mergeRequestListWorker;
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle("List of Merge Requests");

        setOKActionEnabled(false);
        setOKButtonText("Code Review");
        setHorizontalStretch(2);

        List<GitlabMergeRequest> mergeRequests = mergeRequestListWorker.getMergeRequests();
        listOfRequests.setModel(mergeRequestModel(mergeRequests));
        listOfRequests.getColumnModel().getColumn(0).setPreferredWidth(200);
        listOfRequests.getColumnModel().getColumn(5).setWidth(0);
        listOfRequests.getColumnModel().getColumn(5).setMinWidth(0);
        listOfRequests.getColumnModel().getColumn(5).setMaxWidth(0);
        listOfRequests.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        listOfRequests.getSelectionModel().addListSelectionListener(event -> setOKActionEnabled(true));
    }

    @Override
    protected void doOKAction() {
        GitlabMergeRequest mergeRequest =
                (GitlabMergeRequest) listOfRequests.getValueAt(listOfRequests.getSelectedRow(), 5);
        CodeReviewDialog codeReviewDialog = new CodeReviewDialog(project, mergeRequest, mergeRequestListWorker);
        codeReviewDialog.show();
        if (codeReviewDialog.isOK()) {
            super.doOKAction();
        }
    }

    private TableModel mergeRequestModel(List<GitlabMergeRequest> mergeRequests) {
        Object[] columnNames = {"Merge request", "Author", "Source", "Target", "Assignee", ""};
        Object[][] data = new Object[mergeRequests.size()][columnNames.length];
        int i = 0;
        for (GitlabMergeRequest mergeRequest : mergeRequests) {
            Object[] row = new Object[columnNames.length];
            row[0] = mergeRequest.getTitle();
            row[1] = mergeRequest.getAuthor().getName();
            row[2] = mergeRequest.getSourceBranch();
            row[3] = mergeRequest.getTargetBranch();
            String assignee = "";
            if (mergeRequest.getAssignee() != null) {
                assignee = mergeRequest.getAssignee().getName();
            }
            row[4] = assignee;
            row[5] = mergeRequest;
            data[i] = row;
            i++;
        }
        return new ReadOnlyTableModel(data, columnNames);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainView;
    }
}
