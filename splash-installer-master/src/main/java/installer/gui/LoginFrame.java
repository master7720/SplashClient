package installer.gui;

import com.google.gson.JsonObject;
import installer.Main;
import installer.auth.network.Utils;
import installer.auth.network.Packets;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static installer.auth.network.Connection.network;

public class LoginFrame extends JFrame{
    private JLabel lbl_login;
    private JLabel lbl_name;
    private JLabel lbl_password;
    private JTextField field_name;
    private JPasswordField field_password;
    private JLabel lbl_error;
    private JPanel panel_login;
    private JButton button_login;
    private JPanel panel_main;
    private JButton button_cancel;

    public LoginFrame() {
        Frame.init(this,panel_main);

        button_cancel.addActionListener(e -> {
            new MenuNotLoggedIn();
        });

        AtomicBoolean debounce = new AtomicBoolean(false);
        button_login.addActionListener(e -> {
            if(debounce.get())return;
            debounce.set(true);

            String name = field_name.getText(),
                    password = String.valueOf(field_password.getPassword());

            JsonObject loginData = new JsonObject();
            loginData.addProperty("name",name);
            loginData.addProperty("password",password);

            network.sendQueue(Packets.LOGIN,loginData);
            network.awaitPacket(Packets.LOGIN_RESPONSE, data -> {
                boolean success = data.get("success").getAsBoolean();
                String cause = data.get("cause").getAsString();

                if(!success){
                    lbl_error.setText(cause);
                }else{
                    Main.isLoggedIn = true;
                    lbl_error.setText("");
                    Utils.saveCredentials(name,password);
                    new Menu();
                }
            });

            debounce.set(false);
        });
    }
}
