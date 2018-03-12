package com.ppolivka.gitlabprojects.common;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Abstract Action class that provides method for validating GitLab API Settings
 *
 * @author ppolivka
 * @since 22.12.2015
 */
public abstract class NoGitLabApiAction extends DumbAwareAction {

    protected static SettingsState settingsState = SettingsState.getInstance();

    protected Project project;
    protected VirtualFile file;

    public NoGitLabApiAction() {
    }

    public NoGitLabApiAction(@Nullable String text) {
        super(text);
    }

    public NoGitLabApiAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        project = anActionEvent.getData(CommonDataKeys.PROJECT);
        file = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || project.isDisposed()) {
            return;
        }

        if(settingsState.getAllServers().size() == 0) {
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

}
