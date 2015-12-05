package com.ppolivka.gitlabprojects.merge.info;

import git4idea.util.GitCommitCompareInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Class containing info about diff
 *
 * @author ppolivka
 * @since 31.10.2015
 */
public class DiffInfo {
    @NotNull
    private final GitCommitCompareInfo myInfo;
    @NotNull
    private final String myFrom;
    @NotNull
    private final String myTo;

    public DiffInfo(@NotNull GitCommitCompareInfo info, @NotNull String from, @NotNull String to) {
        myInfo = info;
        myFrom = from; // HEAD
        myTo = to;     // BASE
    }

    @NotNull
    public GitCommitCompareInfo getInfo() {
        return myInfo;
    }

    @NotNull
    public String getFrom() {
        return myFrom;
    }

    @NotNull
    public String getTo() {
        return myTo;
    }
}
