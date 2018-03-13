package com.ppolivka.gitlabprojects.configuration;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Project specific setting
 *
 * @author ppolivka
 * @since 30.10.2015
 */
@State(
        name = "GitlabProjectsProjectSettings",
        storages = {
                @Storage(file = StoragePathMacros.WORKSPACE_FILE)
        }
)
public class ProjectState implements PersistentStateComponent<ProjectState.State> {

    private State projectState = new State();

    public static ProjectState getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, ProjectState.class);
    }

    @Nullable
    @Override
    public State getState() {
        return projectState;
    }

    @Override
    public void loadState(State state) {
        projectState = state;
    }

    public static class State {
        public String lastMergedBranch;
        public Boolean deleteMergedBranch;
        public Boolean mergeAsWorkInProgress;
        public Map<Integer, Integer> projectIdMap = new HashMap();
    }

    public Integer getProjectId(String gitRepository) {
        if(projectState.projectIdMap != null) {
            return projectState.projectIdMap.get(gitRepository.hashCode());
        }
        return null;
    }

    public void setProjectId(String gitRepository, Integer projectId) {
        if(projectState.projectIdMap == null) {
            projectState.projectIdMap = new HashMap<>();
        }
        projectState.projectIdMap.put(gitRepository.hashCode(), projectId);
    }

    public String getLastMergedBranch() {
        return projectState.lastMergedBranch;
    }

    public void setLastMergedBranch(String lastMergedBranch) {
        projectState.lastMergedBranch = lastMergedBranch;
    }

    public Boolean getDeleteMergedBranch() {
        return projectState.deleteMergedBranch;
    }

    public void setDeleteMergedBranch(Boolean deleteMergedBranch) {
        projectState.deleteMergedBranch = deleteMergedBranch;
    }

    public Boolean getMergeAsWorkInProgress() {
        return projectState.mergeAsWorkInProgress;
    }

    public void setMergeAsWorkInProgress(Boolean mergeAsWorkInProgress) {
        projectState.mergeAsWorkInProgress = mergeAsWorkInProgress;
    }


}
