package com.ppolivka.gitlabprojects.configuration;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstraints;
import com.ppolivka.gitlabprojects.common.EditableView;

import javax.swing.*;

/**
 * Settings View
 *
 * @author ppolivka
 * @since 9.10.2015
 */
public class SettingsView extends JPanel implements EditableView<SettingsState, String[] > {

    private final JLabel labelHost = new JLabel( "GitLab Host" );
    private final JTextField textHost = new JTextField();

    private final JLabel labelAPI = new JLabel( "GitLab API Key" );
    private final JTextField textAPI = new JTextField();

    public SettingsView( ) {
        setupLayout();
    }

    private void setupLayout() {
        this.setLayout(new TableLayout(
                new double[]{TableLayout.FILL},
                new double[]{
                        TableLayout.MINIMUM, TableLayout.MINIMUM, TableLayout.MINIMUM, TableLayout.MINIMUM, TableLayout.MINIMUM
                }
        ));
        this.add( labelHost, new TableLayoutConstraints( 0, 0, 0, 0, TableLayout.FULL, TableLayout.FULL ) );
        this.add( textHost, new TableLayoutConstraints( 0, 1, 0, 1, TableLayout.FULL, TableLayout.FULL ) );
        this.add( labelAPI, new TableLayoutConstraints( 0, 2, 0, 2, TableLayout.FULL, TableLayout.FULL ) );
        this.add( textAPI, new TableLayoutConstraints( 0, 3, 0, 3, TableLayout.FULL, TableLayout.FULL ) );
    }

    @Override
    public void fill( SettingsState state ) {
        textHost.setText( state == null ? "" : state.getHost());
        textAPI.setText( state == null ? "" : state.getToken() );
    }

    @Override
    public String[] save() {
        return new String[] { textHost.getText(), textAPI.getText() };
    }

}
