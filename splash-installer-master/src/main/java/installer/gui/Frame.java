package installer.gui;

import installer.Main;

import javax.swing.*;
import java.awt.*;

public class Frame{
    public static JFrame currentFrame;
    public static void init(JFrame frame, JPanel panel_main){
        if(currentFrame!=null){
            Main.guiPos = currentFrame.getLocation();
            Main.guiSize = currentFrame.getSize();
            currentFrame.dispose();
        }
        frame.setTitle(Main.NAME+" "+Main.VERSION);
        frame.setIconImage(Main.icon.getImage());
        frame.setContentPane(panel_main);
        frame.setSize(Main.guiSize);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocation(Main.guiPos);
        frame.setVisible(true);
        currentFrame = frame;
    }
}

