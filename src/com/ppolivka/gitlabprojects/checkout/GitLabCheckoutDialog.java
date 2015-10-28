package com.ppolivka.gitlabprojects.checkout;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.util.ImageLoader;
import com.intellij.util.ui.JBImageIcon;
import com.ppolivka.gitlabprojects.api.dto.ProjectDto;
import com.ppolivka.gitlabprojects.configuration.ConfigurationDialog;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.ui.JBColor.WHITE;

/**
 * TODO:Descibe
 *
 * @author ppolivka
 * @since 28.10.2015
 */
public class GitLabCheckoutDialog extends DialogWrapper {

    private JPanel mainView;
    private JButton refreshButton;
    private JButton settingsButton;
    private JTree allProjects;

    private SettingsState settingsState = SettingsState.getInstance();

    private ConfigurationDialog configurationDialog;
    private String lastUsedUrl = "";
    private Project project;
    private ActionListener settingsDialogActionListener;

    private DefaultTreeCellRenderer listingCellRenderer = new DefaultTreeCellRenderer();
    private DefaultTreeCellRenderer loadingCellRenderer = new DefaultTreeCellRenderer();

    public GitLabCheckoutDialog(@Nullable Project project) {
        super(project);
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle("GitLab Checkout");
        setSize(500, 600);
        setAutoAdjustable(false);
        setOKButtonText("Checkout");

        Border emptyBorder = BorderFactory.createCompoundBorder();
        refreshButton.setBorder(emptyBorder);
        settingsButton.setBorder(emptyBorder);

        settingsDialogActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                configurationDialog = new ConfigurationDialog(getContentPane(), false);
                configurationDialog.show();
                if(configurationDialog.isOK() && configurationDialog.isModified()){
                    try {
                        configurationDialog.apply();
                    } catch (ConfigurationException ignored) {
                    }
                    refreshTree();
                }
            }
        };

        listingCellRenderer.setClosedIcon(AllIcons.Nodes.TreeClosed);
        listingCellRenderer.setOpenIcon(AllIcons.Nodes.TreeOpen);
        listingCellRenderer.setLeafIcon(IconLoader.findIcon("/icons/gitlab.png"));

        loadingCellRenderer.setBackgroundNonSelectionColor(WHITE);
        JBImageIcon loadingIcon = new JBImageIcon(ImageLoader.loadFromResource("/icons/loading.gif"));
        loadingIcon.setImageObserver(allProjects);
        loadingCellRenderer.setLeafIcon(loadingIcon);
        loadingCellRenderer.setTextNonSelectionColor(JBColor.GRAY);

        allProjects.setCellRenderer(listingCellRenderer);
        allProjects.setScrollsOnExpand(true);
        allProjects.setAutoscrolls(true);
        allProjects.setDragEnabled(false);
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                setOKActionEnabled(false);
                int selRow = allProjects.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = allProjects.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    DefaultMutableTreeNode selectedNode =
                            ((DefaultMutableTreeNode) selPath.getLastPathComponent());
                    String url = "";
                    if (selectedNode.getChildCount() == 0 && !allProjects.isRootVisible()) {
                        url = selectedNode.toString();
                        setOKActionEnabled(true);
                        lastUsedUrl = url;
                        if (e.getClickCount() == 2) {
                            close(OK_EXIT_CODE);
                        }
                    }
                }
            }
        };
        allProjects.addMouseListener(ml);
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshTree();

            }
        });
        settingsButton.addActionListener(settingsDialogActionListener);
        Collection<ProjectDto> projectDtos = settingsState.getProjects();
        reDrawTree(projectDtos == null ? noProjects() : projectDtos);

    }

    public String getLastUsedUrl() {
        return lastUsedUrl;
    }

    private void refreshTree() {
        treeLoading();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Refreshing tree..") {
            boolean isError = false;

            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    settingsState.reloadProjects();
                    reDrawTree(settingsState.getProjects());
                } catch (Throwable e) {
                    Notifications.Bus.notify(new Notification("GitLab Projects Plugin", "Tree Refresh", "Tree refreshing failed, plese correct your plugin settings.", NotificationType.ERROR));
                    isError = true;
                }
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                if(isError) {
                    settingsDialogActionListener.actionPerformed(null);
                }
            }
        });
    }

    private Collection<ProjectDto> noProjects() {
        return new ArrayList<>();
    }

    private void treeLoading() {
        allProjects.setCellRenderer(loadingCellRenderer);
        allProjects.setRootVisible(true);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("loading...");
        allProjects.setModel(new DefaultTreeModel(root));
    }

    private void reDrawTree(Collection<ProjectDto> projectDtos) {
        allProjects.setCellRenderer(listingCellRenderer);
        allProjects.setRootVisible(false);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("My Projects");
        Map<String, DefaultMutableTreeNode> namespaceMap = new HashMap<>();
        for (ProjectDto projectDto : projectDtos) {
            String namespace = projectDto.getNamespace();
            DefaultMutableTreeNode namespaceNode;
            if (namespaceMap.containsKey(namespace)) {
                namespaceNode = namespaceMap.get(namespace);
            } else {
                namespaceNode = new DefaultMutableTreeNode(namespace);
                namespaceMap.put(namespace, namespaceNode);
            }

            DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(projectDto.getName());
            DefaultMutableTreeNode sshNode = new DefaultMutableTreeNode(projectDto.getSshUrl());
            projectNode.add(sshNode);
            DefaultMutableTreeNode httpNode = new DefaultMutableTreeNode(projectDto.getHttpUrl());
            projectNode.add(httpNode);
            namespaceNode.add(projectNode);
        }
        for (DefaultMutableTreeNode namespaceNode : namespaceMap.values()) {
            root.add(namespaceNode);
        }

        allProjects.setModel(new DefaultTreeModel(root));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainView;
    }
}
