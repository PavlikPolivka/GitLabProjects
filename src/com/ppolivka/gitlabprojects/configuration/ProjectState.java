package com.ppolivka.gitlabprojects.configuration;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    }

}
