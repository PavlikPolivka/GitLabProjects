package com.ppolivka.gitlabprojects.merge;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SortedComboBoxModel;
import com.ppolivka.gitlabprojects.configuration.ProjectState;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;

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

    private SortedComboBoxModel<GitLabMergeRequestWorker.BranchInfo> myBranchModel;
    private GitLabMergeRequestWorker.BranchInfo lastSelectedBranch;

    final ProjectState projectState;

    @NotNull
    final GitLabMergeRequestWorker mergeRequestWorker;

    protected CreateMergeRequestDialog(@Nullable Project project, @NotNull GitLabMergeRequestWorker gitLabMergeRequestWorker) {
        super(project);
        projectState = ProjectState.getInstance(project);
        mergeRequestWorker = gitLabMergeRequestWorker;
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle("Create Merge Request");
        setSize(600, 400);
        setAutoAdjustable(false);

        currentBranch.setText(mergeRequestWorker.getGitLocalBranch().getName());

        myBranchModel = new SortedComboBoxModel<>(new Comparator<GitLabMergeRequestWorker.BranchInfo>() {
            @Override
            public int compare(GitLabMergeRequestWorker.BranchInfo o1, GitLabMergeRequestWorker.BranchInfo o2) {
                return StringUtil.naturalCompare(o1.getName(), o2.getName());
            }
        });
        myBranchModel.setAll(mergeRequestWorker.getBranches());
        targetBranch.setModel(myBranchModel);
        targetBranch.setSelectedIndex(0);
        if (mergeRequestWorker.getLastUsedBranch() != null) {
            targetBranch.setSelectedItem(mergeRequestWorker.getLastUsedBranch());
        }
        lastSelectedBranch = getSelectedBranch();

        targetBranch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prepareTitle();
                lastSelectedBranch = getSelectedBranch();
                projectState.setLastMergedBranch(getSelectedBranch().getName());
                mergeRequestWorker.launchLoadDiffInfo(getSelectedBranch());
            }
        });

        prepareTitle();

        diffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mergeRequestWorker.showDiffDialog(getSelectedBranch());
            }
        });
    }

    @Override
    protected void doOKAction() {
        GitLabMergeRequestWorker.BranchInfo branch = getSelectedBranch();
        if (mergeRequestWorker.checkAction(branch)) {
            mergeRequestWorker.createMergeRequest(branch, mergeTitle.getText(), mergeDescription.getText());
            super.doOKAction();
        }
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (StringUtils.isBlank(mergeTitle.getText())) {
            return new ValidationInfo("Merge title cannot be empty", mergeTitle);
        }
        if (getSelectedBranch().getName().equals(currentBranch.getText())) {
            return new ValidationInfo("Target branch must be different from current branch.", targetBranch);
        }
        return null;
    }

    private GitLabMergeRequestWorker.BranchInfo getSelectedBranch() {
        return (GitLabMergeRequestWorker.BranchInfo) targetBranch.getSelectedItem();
    }

    private void prepareTitle() {
        if (StringUtils.isBlank(mergeTitle.getText()) || mergeTitleGenerator(lastSelectedBranch).equals(mergeTitle.getText())) {
            mergeTitle.setText(mergeTitleGenerator(getSelectedBranch()));
        }
    }

    private String mergeTitleGenerator(GitLabMergeRequestWorker.BranchInfo branchInfo) {
        return "Merge of " + currentBranch.getText() + " to " + branchInfo;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainView;
    }
}
