package com.ppolivka.gitlabprojects.common;

/**
 * Editable View Interface
 *
 * @author ppolivka
 * @since 9.10.2015
 */
public interface EditableView<INPUT, OUTPUT> {

    void fill(INPUT input);

    OUTPUT save();

}
