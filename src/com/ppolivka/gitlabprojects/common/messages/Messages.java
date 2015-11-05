package com.ppolivka.gitlabprojects.common.messages;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

/**
 * Notification utils
 *
 * @author ppolivka
 * @since 5.11.2015
 */
public class Messages {

    static {
        Notifications.Bus.register("GitLab Projects", NotificationDisplayType.BALLOON);
    }

    public static void showErrorDialog(Project project, String message, String title) {
        notify(project, message, title, NotificationType.ERROR);
    }

    public static void showWarningDialog(Project project, String message, String title) {
        notify(project, message, title, NotificationType.WARNING);
    }

    public static void notify(Project project, String message, String title, NotificationType notificationType) {
        Notifications.Bus.notify(new Notification("GitLab Projects", title, message, notificationType ), project);
    }

}
