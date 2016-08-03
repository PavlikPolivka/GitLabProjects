package com.ppolivka.gitlabprojects.component;

import java.util.Collection;

/**
 * TODO:Describe
 *
 * @author ppolivka
 * @since $_version_$
 */
public interface Searchable<R, T> {

  Collection<R> search(T toSearch);

}
