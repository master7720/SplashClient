package installer.gui;

import com.google.gson.JsonObject;
import installer.Main;
import installer.auth.network.Utils;
import installer.auth.network.Packets;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static installer.auth.network.Connection.network;

public class RegisterFrame extends JFrame{
    private JPanel panel_main;
    private JTextField field_name;
    private JPasswordField field_password;
    private JButton button_register;
    private JPasswordField field_confirm;
    private JPanel panel_register;
    private JTextField field_serial_key;
    private JLabel lbl_name;
    private JLabel lbl_password;
    private JLabel lbl_confirm;
    private JLabel lbl_register;
    private JLabel lbl_serial_key;
    private JLabel lbl_error;
    private JButton button_cancel;

    public RegisterFrame() {
        Frame.init(this,panel_main);

        button_cancel.addActionListener(e -> {
            new MenuNotLoggedIn();
        });

        AtomicBoolean debounce = new AtomicBoolean(false);
        button_register.addActionListener(e -> {
            if(debounce.get())return;
            debounce.set(true);

            String name = field_name.getText(),
                    serialKey = field_serial_key.getText(),
                    password = String.valueOf(field_password.getPassword()),
                    confirm = String.valueOf(field_confirm.getPassword());

            JsonObject registerData = new JsonObject();
            registerData.addProperty("name",name);
            registerData.addProperty("serialKey",serialKey);
            registerData.addProperty("password",password);
            registerData.addProperty("confirm",confirm);

            network.sendQueue(Packets.REGISTER,registerData);
            network.awaitPacket(Packets.REGISTER_RESPONSE, data -> {
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
