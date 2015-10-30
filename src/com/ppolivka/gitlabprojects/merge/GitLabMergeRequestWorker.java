package com.ppolivka.gitlabprojects.merge;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.Convertor;
import com.ppolivka.gitlabprojects.common.GitLabUtils;
import git4idea.commands.Git;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GitLab Merge request worker
 *
 * @author ppolivka
 * @since 30.10.2015
 */
public class GitLabMergeRequestWorker {

    public static GitLabMergeRequestWorker create(@NotNull final Project project, @Nullable final VirtualFile file) {
        return GitLabUtils.computeValueInModal(project, "Loading data...", new Convertor<ProgressIndicator, GitLabMergeRequestWorker>() {

            @Override
            public GitLabMergeRequestWorker convert(ProgressIndicator indicator) {
                Git git = ServiceManager.getService(Git.class);
                return new GitLabMergeRequestWorker();
            }

        });

    }

}
