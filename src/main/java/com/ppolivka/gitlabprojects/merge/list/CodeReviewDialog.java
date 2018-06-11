package com.ppolivka.gitlabprojects.merge.list;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.Convertor;
import com.ppolivka.gitlabprojects.comment.CommentsDialog;
import com.ppolivka.gitlabprojects.comment.GitLabCommentsListWorker;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.merge.info.BranchInfo;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;

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
    private VirtualFile virtualFile;
    private BranchInfo sourceBranch;
    private BranchInfo targetBranch;

    private JPanel panel;
    private JButton diffButton;
    private JLabel sourceName;
    private JLabel targetName;
    private JLabel requestName;
    private JButton commentsButton;
    private JLabel assigneeName;
    private JButton assignMe;

    SettingsState settingsState = SettingsState.getInstance();

    private boolean diffClicked = false;

    protected CodeReviewDialog(@Nullable Project project,
                               @NotNull GitlabMergeRequest mergeRequest,
                               @NotNull GitLabMergeRequestListWorker mergeRequestWorker,
                               VirtualFile virtualFile
    ) {
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

        String assignee = "";
        if (mergeRequest.getAssignee() != null) {
            assignee = mergeRequest.getAssignee().getName();
        }
        assigneeName.setText(assignee);

        diffButton.addActionListener(e -> {
            diffClicked = true;
            mergeRequestWorker.getDiffViewWorker().showDiffDialog(sourceBranch, targetBranch);
        });

        commentsButton.addActionListener(e -> {
            GitLabCommentsListWorker commentsListWorker = GitLabCommentsListWorker.create(project, mergeRequest, virtualFile);
            CommentsDialog commentsDialog = new CommentsDialog(project, commentsListWorker, virtualFile);
            commentsDialog.show();
        });

        assignMe.addActionListener(event -> {
            GitLabUtil.computeValueInModal(project, "Changing assignee...", (Convertor<ProgressIndicator, Void>) o -> {
                try {
                    SettingsState settingsState = SettingsState.getInstance();
                    GitlabUser currentUser = settingsState.api(mergeRequestWorker.getGitRepository()).getCurrentUser();
                    settingsState.api(mergeRequestWorker.getGitRepository()).changeAssignee(
                            mergeRequestWorker.getGitlabProject(),
                            mergeRequest,
                            currentUser
                    );
                    assigneeName.setText(currentUser.getName());
                } catch (Exception e) {
                    showErrorDialog(project, "Cannot change assignee of this merge request.", "Cannot Change Assignee");
                }
                return null;
            });
        });
    }


    private BranchInfo createBranchInfo(String name) {
        return new BranchInfo(name, mergeRequestWorker.getRemoteProjectName(), true);
    }

    @Override
    protected void doOKAction() {
        boolean canContinue = diffClicked;
        if (!diffClicked) {
            canContinue = GitLabUtil
                    .showYesNoDialog(project, "Merging Without Review", "You are about to merge this merge request without looking at code differences. Are you sure?");
        }
        if (canContinue) {
            mergeRequestWorker.mergeBranches(project, mergeRequest);
            super.doOKAction();
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }
}
