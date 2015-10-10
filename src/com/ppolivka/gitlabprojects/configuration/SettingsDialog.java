package com.ppolivka.gitlabprojects.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.uiDesigner.core.GridConstraints;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.event.*;

public class SettingsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private SettingsConfigurable settingsConfigurable = new SettingsConfigurable();
    private JPanel content;
    private JLabel errorMessage;

    boolean ok = false;

    public SettingsDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        GridConstraints gridConstraints = new GridConstraints();
        gridConstraints.setFill(1);
        content.add(settingsConfigurable.createComponent(), gridConstraints);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

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
    }

    public boolean start(String errorMessage) {
        this.setTitle("GitLab Projects Settings");
        this.setSize(500, 200);
        this.setLocationRelativeTo(null);
        if(StringUtils.isNotEmpty(errorMessage)) {
            this.errorMessage.setText("ERROR");
            this.errorMessage.setToolTipText(errorMessage);
        } else {
            this.errorMessage.setText("");
            this.errorMessage.setToolTipText("");
        }

        this.setVisible(true);
        return ok;
    }

    private void onOK() {
        if(settingsConfigurable.isModified()) {
            ok = true;
            try {
                settingsConfigurable.apply();
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
