package com.ppolivka.gitlabprojects.common;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Future task acting as master task
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class MasterFutureTask<T> extends FutureTask<T> {
    @NotNull
    private final Object LOCK = new Object();
    private boolean myDone = false;

    @Nullable
    private List<SlaveFutureTask> mySlaves;

    public MasterFutureTask(@NotNull Callable<T> callable) {
        super(callable);
    }

    boolean addSlave(@NotNull SlaveFutureTask slave) {
        if (isDone()) {
            return false;
        } else {
            synchronized (LOCK) {
                if (myDone) {
                    return false;
                }
                if (mySlaves == null) {
                    mySlaves = new ArrayList<>();
                }
                mySlaves.add(slave);
                return true;
            }
        }
    }

    @Override
    protected void done() {
        synchronized (LOCK) {
            myDone = true;
            if (mySlaves != null) {
                for (final SlaveFutureTask slave : mySlaves) {
                    runSlave(slave);
                }
                mySlaves = null;
            }
        }
    }

    protected void runSlave(@NotNull final SlaveFutureTask slave) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                slave.run();
            }
        });
    }
}