package com.ppolivka.gitlabprojects.common;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * TODO:Descibe
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class SlaveFutureTask<T> extends FutureTask<T> {
    @NotNull
    private final MasterFutureTask myMaster;

    public SlaveFutureTask(@NotNull MasterFutureTask master, @NotNull Callable<T> callable) {
        super(callable);
        myMaster = master;
    }

    @Override
    public void run() {
        if (myMaster.isDone()) {
            super.run();
        } else {
            if (!myMaster.addSlave(this)) {
                super.run();
            }
        }
    }

    public T safeGet() {
        try {
            return super.get();
        } catch (InterruptedException e) {
            return null;
        } catch (CancellationException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }
}
