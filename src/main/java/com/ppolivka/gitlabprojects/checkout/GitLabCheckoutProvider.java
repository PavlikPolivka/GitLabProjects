package com.ppolivka.gitlabprojects.checkout;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import git4idea.actions.BasicAction;
import git4idea.checkout.GitCheckoutProvider;
import git4idea.checkout.GitCloneDialog;
import git4idea.commands.Git;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static com.ppolivka.gitlabprojects.util.MessageUtil.showInfoMessage;
import static org.apache.commons.lang.StringUtils.isNotBlank;


/**
 * Checkourt provider for GitLab
 *
 * @author ppolivka
 * @since 9.10.2015
 */
public class GitLabCheckoutProvider extends GitCheckoutProvider {

    private Git myGit;
    private SettingsState settingsState = SettingsState.getInstance();


    public GitLabCheckoutProvider() {
        super(ServiceManager.getService(Git.class));
        myGit = ServiceManager.getService(Git.class);
    }

    @Override
    public void doCheckout(final Project project, final Listener listener) {
        if(settingsState.getAllServers().size() == 0) {
            showInfoMessage(
                    project,
                    "No Gitlab Servers are configured. Please add your server in settings.",
                    "No Gitlab Servers"
            );
        } else {
            GitLabCheckoutDialog gitLabCheckoutDialog = new GitLabCheckoutDialog(project);
            gitLabCheckoutDialog.show();
            if (gitLabCheckoutDialog.isOK() && isNotBlank(gitLabCheckoutDialog.getLastUsedUrl())) {
                showGitCheckoutDialog(project, listener, gitLabCheckoutDialog.getLastUsedUrl());
            }
        }
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
            }
        }
    }

    @Override
    public String getVcsName() {
        return "Git_Lab";
    }
}
