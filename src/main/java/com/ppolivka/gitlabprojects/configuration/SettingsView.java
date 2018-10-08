package com.ppolivka.gitlabprojects.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.ValidationInfo;
import com.ppolivka.gitlabprojects.api.dto.ServerDto;
import com.ppolivka.gitlabprojects.common.ReadOnlyTableModel;
import com.ppolivka.gitlabprojects.dto.GitlabServer;
import git4idea.DialogManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Dialog for GitLab setting configuration
 *
 * @author ppolivka
 * @since 27.10.2015
 */
public class SettingsView implements SearchableConfigurable {

    public static final String DIALOG_TITLE = "GitLab Settings";
    SettingsState settingsState = SettingsState.getInstance();

    private JPanel mainPanel;
    private JTable serverTable;
    private JButton addNewOneButton;
    private JButton editButton;
    private JButton deleteButton;

    public void setup() {
        addNewOneButton.addActionListener(e -> {
            ServerConfiguration serverConfiguration = new ServerConfiguration(null);
            DialogManager.show(serverConfiguration);
            reset();
        });
        deleteButton.addActionListener(e -> {
            GitlabServer server = getSelectedServer();
            if(server != null) {
                settingsState.deleteServer(server);
                reset();
            }
        });
        editButton.addActionListener(e -> {
            GitlabServer server = getSelectedServer();
            ServerConfiguration serverConfiguration = new ServerConfiguration(server);
            DialogManager.show(serverConfiguration);
            reset();
        });
    }

    private GitlabServer getSelectedServer() {
        if(serverTable.getSelectedRow() >= 0) {
            return (GitlabServer) serverTable.getValueAt(serverTable.getSelectedRow(), 0);
        }
        return null;
    }

    private TableModel serverModel(Collection<GitlabServer> servers) {
        Object[] columnNames = {"", "Server", "Token", "Checkout Method"};
        Object[][] data = new Object[servers.size()][columnNames.length];
        int i = 0;
        for (GitlabServer server : servers) {
            Object[] row = new Object[columnNames.length];
            row[0] = server;
            row[1] = server.getApiUrl();
            row[2] = server.getApiToken();
            row[3] = server.getPreferredConnection().name();
            data[i] = row;
            i++;
        }
        return new ReadOnlyTableModel(data, columnNames);
    }

    @NotNull
    @Override
    public String getId() {
        return DIALOG_TITLE;
    }

    @Nullable
    @Override
    public Runnable enableSearch(String s) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return DIALOG_TITLE;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        reset();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Override
    public void reset() {
        fill(settingsState);
    }

    @Override
    public void disposeUIResources() {

    }

    public void fill(SettingsState settingsState) {
        serverTable.setModel(serverModel(settingsState.getGitlabServers()));
        serverTable.getColumnModel().getColumn(0).setMinWidth(0);
        serverTable.getColumnModel().getColumn(0).setMaxWidth(0);
        serverTable.getColumnModel().getColumn(0).setWidth(0);
        serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverTable.getSelectionModel().addListSelectionListener(event -> {
            editButton.setEnabled(true);
            deleteButton.setEnabled(true);
        });

    }
}