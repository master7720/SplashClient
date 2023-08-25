package installer.gui;

import installer.auth.network.Packets;

import javax.swing.*;

import static installer.auth.network.Connection.network;

public class Menu extends JFrame{
    private JPanel panel_main;
    private JTabbedPane tabbed_main;
    private JPanel panel_play;
    private JPanel panel_account;
    private JPanel panel_credits_main;
    private JPanel panel_names;
    private JLabel label_alberto_name;
    private JLabel label_splashani_name;
    private JLabel label_pegasus_name;
    private JLabel label_master7720_name;
    private JLabel label_crosby_name;
    private JLabel label_headzz_name;
    private JPanel panel_desc;
    private JLabel label_alberto_desc;
    private JLabel label_splash_desc;
    private JLabel label_pegasus_desc;
    private JLabel label_master7720_desc;
    private JLabel label_crosby_desc;
    private JLabel label_headzz_desc;
    private JPanel panel_credits;
    private JButton button_launch;
    private JPanel panel_account_main;
    private JLabel label_welcome;
    private JButton button_logout;
    private JPanel panel_launch;
    private JPanel panel_logout;
    private JLabel label_uid;
    private JLabel label_play_time;
    private JLabel uid;
    private JLabel label_friends;
    private JLabel friend_count;
    private JLabel time;

    public Menu(){
        Frame.init(this,panel_main);
        network.sendQueue(Packets.USER_INFO);
        network.awaitPacket(Packets.USER_INFO_RESPONSE,data->{
            uid.setText(data.get("uid").getAsString());
            label_welcome.setText("Welcome, "+data.get("name").getAsString());
        });
    }
}
