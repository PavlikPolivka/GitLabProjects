package com.ppolivka.gitlabprojects.merge.request;

/**
 * Empty Searchable User implementation
 * User for query implementation and various other pplaceholder
 *
 * @author ppolivka
 * @since 1.4.0
 */
public class EmptyUser extends SearchableUser {
    private String user;

    public EmptyUser(String user) {
        super(null);
        this.user = user;
    }

    @Override
    public String toString() {
        return user;
    }
}
