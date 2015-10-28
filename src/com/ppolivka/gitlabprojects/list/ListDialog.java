package com.ppolivka.gitlabprojects.list;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.util.ImageLoader;
import com.intellij.util.ui.JBImageIcon;
import com.ppolivka.gitlabprojects.api.dto.ProjectDto;
import com.ppolivka.gitlabprojects.common.Function;
import com.ppolivka.gitlabprojects.configuration.ConfigurationDialog;
import com.ppolivka.gitlabprojects.configuration.SettingsState;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.ui.JBColor.WHITE;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class ListDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JTree allProjects;
    private JButton refreshButton;
    private JButton settingsButton;
    private JButton checkoutButton;
    private SettingsState settingsState = SettingsState.getInstance();
    private DefaultTreeCellRenderer listingCellRenderer = new DefaultTreeCellRenderer();
    private DefaultTreeCellRenderer loadingCellRenderer = new DefaultTreeCellRenderer();
    private ConfigurationDialog configurationDialog;
    private Function<String> selectAction;
    private String lastUsedUrl = "";
    private Project project;
    ActionListener settingsDialogActionListener;

    public ListDialog(final Project project, final Function<String> selectAction) {
        this.project = project;
        this.selectAction = selectAction;
        this.configurationDialog = new ConfigurationDialog(project);
        setContentPane(contentPane);
        setModal(true);
        final Component that = this;

        settingsDialogActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                configurationDialog = new ConfigurationDialog(that, false);
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

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        listingCellRenderer.setBackgroundNonSelectionColor(WHITE);
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
                checkoutButton.setEnabled(false);
                int selRow = allProjects.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = allProjects.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    DefaultMutableTreeNode selectedNode =
                            ((DefaultMutableTreeNode) selPath.getLastPathComponent());
                    String url = "";
                    if (selectedNode.getChildCount() == 0 && !allProjects.isRootVisible()) {
                        url = selectedNode.toString();
                        checkoutButton.setEnabled(true);
                        if (e.getClickCount() == 2) {
                            selectAction.execute(url);
                        } else if(e.getClickCount() == 1) {
                            lastUsedUrl = url;
                        }
                    }
                }
            }
        };
        allProjects.addMouseListener(ml);
        checkoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isNotBlank(lastUsedUrl)) {
                    selectAction.execute(lastUsedUrl);
                }
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshTree();

            }
        });
        settingsButton.addActionListener(settingsDialogActionListener);
        Collection<ProjectDto> projectDtos = settingsState.getProjects();
        reDrawTree(projectDtos == null ? noProjects() : projectDtos);

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

    public void start() {
        this.setTitle("Listing GitLab Projects");
        this.setSize(500, 600);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }


    private void onCancel() {
        dispose();
    }

}
