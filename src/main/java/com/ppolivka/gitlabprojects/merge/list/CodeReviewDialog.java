package com.ppolivka.gitlabprojects.merge.list;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.ppolivka.gitlabprojects.merge.info.BranchInfo;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import org.gitlab.api.models.GitlabMergeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog to accept merge request
 *
 * @author ppolivka
 * @since 22.12.2015
 */
public class CodeReviewDialog extends DialogWrapper {

    final private GitlabMergeRequest mergeRequest;
    final private GitLabMergeRequestListWorker mergeRequestWorker;

    private Project project;
    private BranchInfo sourceBranch;
    private BranchInfo targetBranch;

    private JPanel panel;
    private JButton diffButton;
    private JCheckBox removeSourceBranchCheckBox;
    private JLabel sourceName;
    private JLabel targetName;
    private JLabel requestName;

    private boolean diffClicked = false;

    protected CodeReviewDialog(@Nullable Project project,
                               @NotNull GitlabMergeRequest mergeRequest,
                               @NotNull GitLabMergeRequestListWorker mergeRequestWorker) {
        super(project);
        this.project = project;
        this.mergeRequest = mergeRequest;
        this.mergeRequestWorker = mergeRequestWorker;
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle("Code Review");
        setOKButtonText("Merge");

        sourceName.setText(mergeRequest.getSourceBranch());
        sourceBranch = createBranchInfo(mergeRequest.getSourceBranch());

        targetName.setText(mergeRequest.getTargetBranch());
        targetBranch = createBranchInfo(mergeRequest.getTargetBranch());

        requestName.setText(mergeRequest.getTitle());

        diffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                diffClicked = true;
                mergeRequestWorker.getDiffViewWorker().showDiffDialog(sourceBranch, targetBranch);
            }
        });
    }


    private BranchInfo createBranchInfo(String name) {
        return new BranchInfo(name, mergeRequestWorker.getRemoteProjectName(), true);
    }

    @Override
    protected void doOKAction() {
        boolean canContinue = diffClicked;
        if(!diffClicked) {
            canContinue = GitLabUtil
                    .showYesNoDialog(project, "Merging Without Review", "You are about to merge this merge request without looking at code differences. Are you sure?");
        }
        if(canContinue) {
            mergeRequestWorker.mergeBranches(project, mergeRequest, removeSourceBranchCheckBox.isSelected());
            super.doOKAction();
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }
}
