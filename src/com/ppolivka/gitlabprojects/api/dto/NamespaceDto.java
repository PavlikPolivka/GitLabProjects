package com.ppolivka.gitlabprojects.api.dto;

/**
 * Dto Class Representing one namespace
 *
 * @author ppolivka
 * @since 28.10.2015
 */
public class NamespaceDto {

    private int id;
    private String path;
    private String kind;

    public NamespaceDto() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        return path;
    }
}
