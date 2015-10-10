package com.ppolivka.gitlabprojects.list;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.ppolivka.gitlabprojects.common.Function;
import git4idea.actions.BasicAction;
import git4idea.checkout.GitCheckoutProvider;
import git4idea.checkout.GitCloneDialog;
import git4idea.commands.Git;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;


/**
 * Checkourt provider for GitLab
 *
 * @author ppolivka
 * @since 9.10.2015
 */
public class GitLabCheckoutProvider extends GitCheckoutProvider {

    private Git myGit;
    private ListDialog listDialog;

    public GitLabCheckoutProvider() {
        super(ServiceManager.getService(Git.class));
        myGit = ServiceManager.getService(Git.class);
    }

    @Override
    public void doCheckout(final Project project, final Listener listener) {

        listDialog = new ListDialog(new Function<String>() {
            @Override
            public void execute(String value) {
                showGitCheckoutDialog(project, listener, value);
            }
        });
        listDialog.start();
    }

    public void showGitCheckoutDialog(@NotNull Project project, @Nullable Listener listener, String preselectedUrl) {
        BasicAction.saveAll();
        GitCloneDialog dialog = new GitCloneDialog(project);
        dialog.prependToHistory(preselectedUrl);
        if(dialog.showAndGet()) {
            dialog.rememberSettings();
            LocalFileSystem lfs = LocalFileSystem.getInstance();
            File parent = new File(dialog.getParentDirectory());
            VirtualFile destinationParent = lfs.findFileByIoFile(parent);
            if(destinationParent == null) {
                destinationParent = lfs.refreshAndFindFileByIoFile(parent);
            }

            if(destinationParent != null) {
                String sourceRepositoryURL = dialog.getSourceRepositoryURL();
                String directoryName = dialog.getDirectoryName();
                String parentDirectory = dialog.getParentDirectory();
                clone(project, this.myGit, listener, destinationParent, sourceRepositoryURL, directoryName, parentDirectory);
                listDialog.setVisible(false);
            }
        }
    }

    @Override
    public String getVcsName() {
        return "Git_Lab";
    }
}
