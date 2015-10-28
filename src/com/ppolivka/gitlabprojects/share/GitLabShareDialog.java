package com.ppolivka.gitlabprojects.share;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.CollectionComboBoxModel;
import com.ppolivka.gitlabprojects.api.dto.NamespaceDto;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog that is displayed when sharing project to git lab
 *
 * @author ppolivka
 * @since 28.10.2015
 */
public class GitLabShareDialog extends DialogWrapper {

    private static SettingsState settingsState = SettingsState.getInstance();

    private JPanel mainView;
    private JRadioButton isPrivate;
    private JRadioButton isPublic;
    private JTextField projectName;
    private JTextArea commitMessage;
    private JRadioButton isInternal;
    private JComboBox groupList;
    private JButton refreshButton;
    private JRadioButton isSSHAuth;
    private JRadioButton isHTTPAuth;
    private final Project project;

    public GitLabShareDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle("Share on GitLab");
        setOKButtonText("Share");

        Border emptyBorder = BorderFactory.createCompoundBorder();
        refreshButton.setBorder(emptyBorder);

        commitMessage.setText("Initial commit");

        isInternal.setSelected(true);

        ButtonGroup visibilityGroup = new ButtonGroup();
        visibilityGroup.add(isPrivate);
        visibilityGroup.add(isInternal);
        visibilityGroup.add(isPublic);

        isSSHAuth.setSelected(true);

        ButtonGroup authGroup = new ButtonGroup();
        authGroup.add(isHTTPAuth);
        authGroup.add(isSSHAuth);

        reloadGroupList();

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadGroupList();
            }
        });
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if(StringUtils.isBlank(projectName.getText())) {
            return new ValidationInfo("Project name cannot be empty", projectName);
        }
        if(StringUtils.isBlank(commitMessage.getText())) {
            return new ValidationInfo("Initial commit message cannot be empty", commitMessage);
        }
        return null;
    }

    private void reloadGroupList() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Refreshing group list..") {
            boolean isError = false;
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    List<NamespaceDto> namespaces = new ArrayList<>();
                    namespaces.add(new NamespaceDto() {{
                        setId(0);
                        setPath("Default");
                    }});
                    List<NamespaceDto> remoteNamespaces = settingsState.api().getNamespaces();
                    if(remoteNamespaces != null) {
                        namespaces.addAll(remoteNamespaces);
                    }
                    CollectionComboBoxModel collectionComboBoxModel = new CollectionComboBoxModel(namespaces, namespaces.get(0));
                    groupList.setModel(collectionComboBoxModel);

                } catch (IOException e) {
                    isError = true;
                }
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                if(isError) {
                    Messages.showErrorDialog(project, "Groups cannot be refreshed", "Error Loading Groups");
                    close(CLOSE_EXIT_CODE);
                }
            }
        });


    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainView;
    }

    public JRadioButton getIsPrivate() {
        return isPrivate;
    }

    public JRadioButton getIsPublic() {
        return isPublic;
    }

    public JTextField getProjectName() {
        return projectName;
    }

    public JTextArea getCommitMessage() {
        return commitMessage;
    }

    public JRadioButton getIsInternal() {
        return isInternal;
    }

    public JComboBox getGroupList() {
        return groupList;
    }

    public JRadioButton getIsSSHAuth() {
        return isSSHAuth;
    }

    public JRadioButton getIsHTTPAuth() {
        return isHTTPAuth;
    }
}
