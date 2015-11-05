package com.ppolivka.gitlabprojects.merge;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.ppolivka.gitlabprojects.common.GitLabUtils;
import git4idea.DialogManager;

/**
 * GitLab Merge Request Action class
 *
 * @author ppolivka
 * @since 30.10.2015
 */
public class GitLabMergeRequestAction extends DumbAwareAction {

    public GitLabMergeRequestAction() {
        super("Create _Merge Request", "Creates merge request from current branch", AllIcons.Vcs.Merge);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final VirtualFile file = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || project.isDisposed() || !GitLabUtils.testGitExecutable(project)) {
            return;
        }

        GitLabMergeRequestWorker mergeRequestWorker = GitLabMergeRequestWorker.create(project, file);
        if(mergeRequestWorker != null) {
            CreateMergeRequestDialog createMergeRequestDialog = new CreateMergeRequestDialog(project, mergeRequestWorker);
            DialogManager.show(createMergeRequestDialog);
        }
    }
}
