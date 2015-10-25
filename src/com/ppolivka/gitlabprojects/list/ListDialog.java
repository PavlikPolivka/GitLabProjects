package com.ppolivka.gitlabprojects.list;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.ppolivka.gitlabprojects.api.dto.Project;
import com.ppolivka.gitlabprojects.common.Function;
import com.ppolivka.gitlabprojects.configuration.SettingsDialog;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import org.apache.commons.lang.exception.ExceptionUtils;

import javax.swing.*;
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

public class ListDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JTree allProjects;
    private JButton refreshButton;
    private JButton settingsButton;
    private SettingsState settingsState = SettingsState.getInstance();
    DefaultTreeCellRenderer listingCellRenderer = new DefaultTreeCellRenderer();
    DefaultTreeCellRenderer loadingCellRenderer = new DefaultTreeCellRenderer();
    SettingsDialog settingsDialog = new SettingsDialog();
    private Function selectAction;

    public ListDialog(final Function selectAction) {
        this.selectAction = selectAction;
        setContentPane(contentPane);
        setModal(true);

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
        loadingCellRenderer.setLeafIcon(AllIcons.RunConfigurations.LoadingTree);
        loadingCellRenderer.setTextNonSelectionColor(JBColor.GRAY);

        allProjects.setCellRenderer(listingCellRenderer);
        allProjects.setScrollsOnExpand(true);
        allProjects.setAutoscrolls(true);
        allProjects.setDragEnabled(false);
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = allProjects.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = allProjects.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    if (e.getClickCount() == 2) {
                        DefaultMutableTreeNode selectedNode =
                                ((DefaultMutableTreeNode) selPath.getLastPathComponent());
                        if (selectedNode.getChildCount() == 0 && !allProjects.isRootVisible()) {
                            String url = selectedNode.toString();
                            selectAction.execute(url);
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
        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean refresh = settingsDialog.start("");
                if(refresh) {
                    refreshTree();
                }

            }
        });
        Collection<Project> projects = settingsState.getProjects();
        reDrawTree(projects == null ? noProjects() : projects);

    }

    private void refreshTree() {
        treeLoading();
        Runnable refreshAction = new Runnable() {
            @Override
            public void run() {
                try {
                    settingsState.reloadProjects();
                    reDrawTree(settingsState.getProjects());
                } catch (Throwable e) {
                    if(settingsDialog.start(ExceptionUtils.getRootCauseMessage(e))) {
                        refreshTree();
                    }
                }

            }
        };
        new Thread(refreshAction).start();
    }

    private Collection<Project> noProjects() {
        return new ArrayList<>();
    }

    private void treeLoading() {
        allProjects.setCellRenderer(loadingCellRenderer);
        allProjects.setRootVisible(true);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("loading...");
        allProjects.setModel(new DefaultTreeModel(root));
    }

    private void reDrawTree(Collection<Project> projects) {
        allProjects.setCellRenderer(listingCellRenderer);
        allProjects.setRootVisible(false);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("My Projects");
        Map<String, DefaultMutableTreeNode> namespaceMap = new HashMap<>();
        for (Project project : projects) {
            String namespace = project.getNamespace();
            DefaultMutableTreeNode namespaceNode;
            if (namespaceMap.containsKey(namespace)) {
                namespaceNode = namespaceMap.get(namespace);
            } else {
                namespaceNode = new DefaultMutableTreeNode(namespace);
                namespaceMap.put(namespace, namespaceNode);
            }

            DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(project.getName());
            DefaultMutableTreeNode sshNode = new DefaultMutableTreeNode(project.getSshUrl());
            projectNode.add(sshNode);
            DefaultMutableTreeNode httpNode = new DefaultMutableTreeNode(project.getHttpUrl());
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
