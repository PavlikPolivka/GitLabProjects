package com.ppolivka.gitlabprojects.common;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
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

    public GitLabApiAction() {
    }

    public GitLabApiAction(@Nullable String text) {
        super(text);
    }

    public GitLabApiAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    /**
     * Validate git lab api settings
     * If API is not valid, Setting dialog will be displayed
     * If API is still not configured after that false is returned
     *
     * @param project the project
     * @return true if API is OK, false if not
     */
    public static boolean validateGitLabApi(@NotNull Project project) {
        Boolean isApiSetup = GitLabUtil.computeValueInModal(project, "Validating GitLab Api...",false, new Convertor<ProgressIndicator, Boolean>() {
            @Override
            public Boolean convert(ProgressIndicator progressIndicator) {
                try {
                    settingsState.isApiValid(settingsState.host, settingsState.token);
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
