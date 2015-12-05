package com.ppolivka.gitlabprojects.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.*;

import static com.ppolivka.gitlabprojects.configuration.SettingsView.DIALOG_TITLE;

/**
 * Wrapper around settings view
 *
 * @author ppolivka
 * @since 17.11.2015
 */
public class SettingsDialog extends DialogWrapper {

    private SettingsView settingsView  = new SettingsView();

    public SettingsDialog(@Nullable Project project) {
        super(project);
        init();
    }

    public SettingsDialog(@NotNull Component parent, boolean canBeParent) {
        super(parent, canBeParent);
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle(DIALOG_TITLE);
        setSize(600, 300);
        setAutoAdjustable(false);
        settingsView.setup();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return settingsView.doValidate();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return settingsView.createComponent();
    }

    public boolean isModified() {
        return settingsView.isModified();
    }

    public void apply() throws ConfigurationException {
        settingsView.apply();
    }
}
