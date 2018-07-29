package com.ppolivka.gitlabprojects.merge.info;

import com.ppolivka.gitlabprojects.common.MasterFutureTask;
import com.ppolivka.gitlabprojects.common.SlaveFutureTask;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Class containing info about branch
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class BranchInfo {

    @NotNull
    public final Object LOCK = new Object();
    @NotNull
    public final Object LOCK_FETCH = new Object();

    private String name;
    private String remoteName;
    private MasterFutureTask<Void> fetchTask;
    private SlaveFutureTask<DiffInfo> diffInfoTask;

    private boolean remoteOnly = false;

    public BranchInfo(String name, String remoteName) {
        this(name, remoteName, false);
    }

    public BranchInfo(String name, String remoteName, boolean remoteOnly) {
        this.name = name;
        this.remoteName = remoteName;
        this.remoteOnly = remoteOnly;
    }

    public String getFullName() {
        return remoteOnly ? getFullRemoteName() : getName();
    }

    public String getFullRemoteName() {
        return this.getRemoteName() + "/" + this.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public SlaveFutureTask<DiffInfo> getDiffInfoTask() {
        return diffInfoTask;
    }

    public void setDiffInfoTask(@NotNull SlaveFutureTask<DiffInfo> diffInfoTask) {
        this.diffInfoTask = diffInfoTask;
    }

    public MasterFutureTask<Void> getFetchTask() {
        return fetchTask;
    }

    public void setFetchTask(MasterFutureTask<Void> fetchTask) {
        this.fetchTask = fetchTask;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BranchInfo)) {
            return false;
        }
        BranchInfo that = (BranchInfo) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
