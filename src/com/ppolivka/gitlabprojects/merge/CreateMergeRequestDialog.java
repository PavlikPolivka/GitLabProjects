package com.ppolivka.gitlabprojects.merge;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Dialog fore creating merge requests
 *
 * @author ppolivka
 * @since 30.10.2015
 */
public class CreateMergeRequestDialog extends DialogWrapper {

    private JPanel mainView;
    private JComboBox targetBranch;
    private JLabel currentBranch;
    private JTextField mergeTitle;
    private JTextArea mergeDescription;
    private JButton diffButton;

    @NotNull
    final GitLabMergeRequestWorker mergeRequestWorker;

    protected CreateMergeRequestDialog(@Nullable Project project, @NotNull GitLabMergeRequestWorker gitLabMergeRequestWorker) {
        super(project);
        mergeRequestWorker = gitLabMergeRequestWorker;
        init();
    }

    @Override
    protected void init() {
        super.init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainView;
    }
}
