package com.ppolivka.gitlabprojects.merge.list;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * TODO:Descibe
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class GitLabMergeRequestListDialog extends DialogWrapper {

    private JPanel mainView;
    private JTable listOfRequests;

    final GitLabMergeRequestListWorker mergeRequestListWorker;

    public GitLabMergeRequestListDialog(@Nullable Project project, @NotNull GitLabMergeRequestListWorker mergeRequestListWorker) {
        super(project);
        this.mergeRequestListWorker = mergeRequestListWorker;
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle("List of Merge Requests");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainView;
    }
}
