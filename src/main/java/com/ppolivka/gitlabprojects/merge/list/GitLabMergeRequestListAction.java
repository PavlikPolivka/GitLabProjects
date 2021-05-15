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
        /*
         * Fix missed icon Mikhail Zakharov <zmey20000@yahoo.com>, 2020.04.21
         */
        super("_List Merge Requests...", "List of all merge requests for this project", AllIcons.Vcs.Merge);
    }

    @Override
    public void apiValidAction(AnActionEvent anActionEvent) {
        GitLabMergeRequestListWorker mergeRequestListWorker = GitLabMergeRequestListWorker.create(project, file);
        GitLabMergeRequestListDialog gitLabMergeRequestListDialog = new GitLabMergeRequestListDialog(project, mergeRequestListWorker, file);
        DialogManager.show(gitLabMergeRequestListDialog);

    }
}
