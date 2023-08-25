package installer.gui;

import com.google.gson.JsonObject;
import installer.Main;
import installer.auth.network.Utils;
import installer.auth.network.Packets;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

import static installer.auth.network.Connection.network;

public class MenuNotLoggedIn extends JFrame{
    private JPanel panel_main;
    private JLabel lbl_login;
    private JButton button_login;
    private JButton button_register;
    private JButton button_discord;
    private JButton button_credits;

    public MenuNotLoggedIn() {
        if(Main.credentialsFile.isFile()) {
            String[] credentials = Utils.getCredentials();
            JsonObject loginData = new JsonObject();
            loginData.addProperty("name", credentials[0]);
            loginData.addProperty("password", credentials[1]);
            network.sendQueue(Packets.LOGIN, loginData);
            network.awaitPacket(Packets.LOGIN_RESPONSE,data->{
                if(data.get("success").getAsBoolean()) {
                    Main.isLoggedIn = true;
                }else
                    Main.credentialsFile.delete();
            });
            if(Main.isLoggedIn) {
                new Menu();
                return;
            }
        }
        Frame.init(this,panel_main);

        button_login.addActionListener(e -> {
            new LoginFrame();
        });

        button_register.addActionListener(e -> {
            new RegisterFrame();
        });

        button_discord.addActionListener(e -> {
            try {
                Utils.openWebpage(new URL("https://discord.gg/ds2WYhvWuH"));
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        });

        button_credits.addActionListener(e -> {
            new CreditsFrame();
        });
    }
}
