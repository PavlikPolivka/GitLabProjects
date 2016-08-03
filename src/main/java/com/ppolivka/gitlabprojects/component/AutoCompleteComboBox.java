package com.ppolivka.gitlabprojects.component;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO:Describe
 *
 * @author ppolivka
 * @since $_version_$
 */
public class AutoCompleteComboBox<E> extends JComboBox<E> {

  private Searchable<E, String> searchable;

  public AutoCompleteComboBox() {
    super();
    setEditable(true);
    Component c = getEditor().getEditorComponent();
    if (c instanceof JTextComponent) {
      final JTextComponent tc = (JTextComponent) c;
      tc.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void changedUpdate(DocumentEvent arg0) {

        }

        @Override
        public void insertUpdate(DocumentEvent arg0) {
          update();
        }

        @Override
        public void removeUpdate(DocumentEvent arg0) {
          update();
        }

        public void update() {
          //perform separately, as listener conflicts between the editing component
          //and JComboBox will result in an IllegalStateException due to editing
          //the component when it is locked.
          SwingUtilities.invokeLater(() -> {
            List<E> founds = new ArrayList<>(searchable.search(tc.getText()));
            setEditable(false);
            removeAllItems();

            for (E e : founds) {
              addItem(e);
            }
            setEditable(true);
            setPopupVisible(true);
          });
        }
      });

      //When the text component changes, focus is gained
      //and the menu disappears. To account for this, whenever the focus
      //is gained by the JTextComponent and it has searchable values, we show the popup.
      tc.addFocusListener(new FocusListener() {
        @Override
        public void focusGained(FocusEvent arg0) {
          if (tc.getText().length() > 0) {
            setPopupVisible(true);
          }
        }

        @Override
        public void focusLost(FocusEvent arg0) {
        }

      });
    } else {
      throw new IllegalStateException("Editing component is not a JTextComponent!");
    }
  }

  public Searchable<E, String> getSearchable() {
    return searchable;
  }

  public void setSearchable(Searchable<E, String> searchable) {
    this.searchable = searchable;
  }
}
