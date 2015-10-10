package com.ppolivka.gitlabprojects.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configuration of settings
 *
 * @author ppolivka
 * @since 9.10.2015
 */
public class SettingsConfigurable implements SearchableConfigurable {

    SettingsState settingsState = SettingsState.getInstance();

    SettingsView settingsView;

    @NotNull
    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @Nullable
    @Override
    public Runnable enableSearch(String s) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "GitLab Projects Settings";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsView = new SettingsView();
        reset();
        return settingsView;
    }

    @Override
    public boolean isModified() {
        String[] save = settingsView.save();
        return save == null
                || !save[0].equals(settingsState.getHost())
                || !save[1].equals(settingsState.getToken());
    }

    @Override
    public void apply() throws ConfigurationException {
        String[] save = settingsView.save();
        settingsState.setHost(save[0]);
        settingsState.setToken(save[1]);
    }

    @Override
    public void reset() {
        if (settingsView != null) {
            settingsView.fill(settingsState);
        }
    }

    @Override
    public void disposeUIResources() {
        settingsView = null;
    }
}
