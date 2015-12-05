package com.ppolivka.gitlabprojects.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;

/**
 * Notification utils
 *
 * @author ppolivka
 * @since 5.11.2015
 */
public class MessageUtil {

    public static void showErrorDialog(Project project, String message, String title) {
        VcsNotifier.getInstance(project).notifyError(title, message);
    }

    public static void showWarningDialog(Project project, String message, String title) {
        VcsNotifier.getInstance(project).notifyWarning(title, message);
    }

    public static void showInfoMessage(Project project, String message, String title) {
        VcsNotifier.getInstance(project).notifyInfo(title, message);
    }

    public static VcsNotifier getNotifier(Project project) {
        return VcsNotifier.getInstance(project);
    }

}
