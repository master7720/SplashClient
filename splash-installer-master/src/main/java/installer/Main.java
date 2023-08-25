package installer;

import com.formdev.flatlaf.FlatDarculaLaf;
import installer.auth.network.Connection;
import installer.gui.MenuNotLoggedIn;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static installer.auth.network.Connection.network;

public class Main {
  public static String VERSION = "b0";
  public static String NAME = "Diesel Launcher";
  public static ImageIcon icon = new ImageIcon(Main.class.getResource("/indeed.png"));

  public static Point guiPos = new Point(50,50);
  public static Dimension guiSize = new Dimension(700,500);
  public static File dataFolder = new File(System.getProperty("user.home") + File.separator + "diesel-client");
  public static File credentialsFile = new File(dataFolder.getPath()+File.separator+"credentials.diesel");
  public static boolean isLoggedIn = false;

  public static void main(String[] args) throws IOException {
    FlatDarculaLaf.setup();

    if (!dataFolder.exists()) {
      dataFolder.mkdir();
    }

    // CONNECT
    try {
      if(!Connection.init())System.exit(-1);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    if (network.connectionFailed) return;

    new MenuNotLoggedIn();
  }
}