package com.ppolivka.gitlabprojects.common;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.Convertor;
import com.ppolivka.gitlabprojects.configuration.SettingsDialog;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import com.ppolivka.gitlabprojects.util.GitLabUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;

/**
 * Abstract Action class that provides method for validating GitLab API Settings
 *
 * @author ppolivka
 * @since 22.12.2015
 */
public abstract class GitLabApiAction extends DumbAwareAction {

    protected static SettingsState settingsState = SettingsState.getInstance();

    protected Project project;
    protected VirtualFile file;

    public GitLabApiAction() {
    }

    public GitLabApiAction(@Nullable String text) {
        super(text);
    }

    public GitLabApiAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        project = anActionEvent.getData(CommonDataKeys.PROJECT);
        file = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || project.isDisposed()) {
            return;
        }

        if(!validateGitLabApi(project, file)) {
            return;
        }

        apiValidAction(anActionEvent);
    }

    /**
     * Abstract method that is called after GitLab Api is validated,
     * we can assume that login credentials are there and api valid
     *
     * @param anActionEvent event information
     */
    public abstract void apiValidAction(AnActionEvent anActionEvent);

    /**
     * Validate git lab api settings
     * If API is not valid, Setting dialog will be displayed
     * If API is still not configured after that false is returned
     *
     * @param project the project
     * @return true if API is OK, false if not
     */
    public static boolean validateGitLabApi(@NotNull Project project, VirtualFile virtualFile) {
        Boolean isApiSetup = GitLabUtil.computeValueInModal(project, "Validating GitLab Api...",false, new Convertor<ProgressIndicator, Boolean>() {
            @Override
            public Boolean convert(ProgressIndicator progressIndicator) {
                try {
                    settingsState.isApiValid(project, virtualFile);
                    return true;
                } catch (Throwable e) {
                    return false;
                }
            }
        });
        boolean isOk = true;
        if (!isApiSetup) {
            //Git Lab Not configured
            SettingsDialog configurationDialog = new SettingsDialog(project);
            configurationDialog.show();
            if (configurationDialog.isOK() && configurationDialog.isModified()) {
                try {
                    configurationDialog.apply();
                } catch (ConfigurationException ignored) {
                    isOk = false;
                }
            }
            if(isOk) {
                isOk = configurationDialog.isOK();
            }
        }
        if(!isOk) {
            showErrorDialog(project, "Cannot log-in to GitLab Server with provided token", "Cannot Login To GitLab");
        }
        return isOk;
    }

}
