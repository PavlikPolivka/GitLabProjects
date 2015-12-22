package com.ppolivka.gitlabprojects.merge.list;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.ppolivka.gitlabprojects.common.GitLabApiAction;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import git4idea.DialogManager;

/**
 * TODO:Descibe
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class GitLabMergeRequestListAction extends GitLabApiAction {

    public GitLabMergeRequestListAction() {
        super("_List Merge Requests", "List of all merge requests for this project", AllIcons.Vcs.MergeSourcesTree);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        final VirtualFile file = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || project.isDisposed() || !GitLabUtil.testGitExecutable(project)) {
            return;
        }

        if(!validateGitLabApi(project)) {
            return;
        }

        GitLabMergeRequestListWorker mergeRequestListWorker = GitLabMergeRequestListWorker.create(project, file);
        GitLabMergeRequestListDialog gitLabMergeRequestListDialog = new GitLabMergeRequestListDialog(project, mergeRequestListWorker);
        DialogManager.show(gitLabMergeRequestListDialog);

    }
}
