package com.ppolivka.gitlabprojects.component;

import java.util.Collection;

/**
 * Interface for searching actions
 *
 * @author ppolivka
 * @since 1.4.0
 */
public interface Searchable<R, T> {

  /**
   * Returns collections of R objects based on T
   * @param toSearch
   * @return
   */
  Collection<R> search(T toSearch);

}
