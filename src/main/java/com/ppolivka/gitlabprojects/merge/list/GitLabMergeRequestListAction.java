package com.ppolivka.gitlabprojects.merge.list;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.ppolivka.gitlabprojects.common.GitLabApiAction;
import git4idea.DialogManager;

/**
 * Action for accepting merge request
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class GitLabMergeRequestListAction extends GitLabApiAction {

    public GitLabMergeRequestListAction() {
        super("_List Merge Requests", "List of all merge requests for this project", AllIcons.Vcs.MergeSourcesTree);
    }

    @Override
    public void apiValidAction(AnActionEvent anActionEvent) {
        GitLabMergeRequestListWorker mergeRequestListWorker = GitLabMergeRequestListWorker.create(project, file);
        GitLabMergeRequestListDialog gitLabMergeRequestListDialog = new GitLabMergeRequestListDialog(project, mergeRequestListWorker);
        DialogManager.show(gitLabMergeRequestListDialog);

    }
}
