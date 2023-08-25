package installer.gui;

import installer.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CreditsFrame extends JFrame{
    private JPanel panel_main;
    private JLabel lbl_login;
    private JLabel label_alberto_name;
    private JPanel panel_credits;
    private JPanel panel_names;
    private JPanel panel_desc;
    private JLabel label_alberto_desc;
    private JLabel label_splashani_name;
    private JLabel label_pegasus_name;
    private JLabel label_master7720_name;
    private JLabel label_crosby_name;
    private JLabel label_splash_desc;
    private JLabel label_pegasus_desc;
    private JLabel label_crosby_desc;
    private JLabel label_master7720_desc;
    private JPanel panel_login;
    private JButton button_cancel;
    private JLabel label_headzz_desc;
    private JLabel label_headzz_name;

    public CreditsFrame() {
        Frame.init(this,panel_main);

        button_cancel.addActionListener(e -> {
            new MenuNotLoggedIn();
        });
    }
}
