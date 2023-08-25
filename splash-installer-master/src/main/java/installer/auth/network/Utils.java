package installer.auth.network;

import installer.Main;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;


public class Utils {
    public static void saveCredentials(String username, String password){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Main.credentialsFile))) {
            writer.write(username+"\n"+password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] getCredentials() {
        String username = null;
        String password = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(Main.credentialsFile))) {
            username = reader.readLine();
            password = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new String[]{username,password};
    }

    public static boolean openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean openWebpage(URL url) {
        try {
            return openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String generateSHA1Hash(String input) {
        try {
            // Create a SHA-1 MessageDigest instance
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");

            // Convert the input string to bytes and compute the hash
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = sha1Digest.digest(inputBytes);

            // Convert the byte array to a hexadecimal string representation
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static SecretKey generateSecret(){
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

            keyGenerator.init(256);

            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // ENCRYPTING
    public static String encryptRSA(String message, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    public static String decryptRSA(String encryptedMessage, PrivateKey privateKey) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static String encrypt(String text, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(text.getBytes());

            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String base64Encrypted, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(base64Encrypted));
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static ArrayList<String> parseStringIntoChunks(String input, int chunkSize) {
        ArrayList<String> chunks = new ArrayList<>();

        if (input == null || input.isEmpty()) {
            return chunks;
        }

        int length = input.length();
        for (int i = 0; i < length; i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, length);
            chunks.add(input.substring(i, endIndex));
        }

        return chunks;
    }

    public static byte[] stringToByteArray(String raw) {
        // Remove the square brackets and any whitespaces, then split the string by commas
        String[] bytesArray = raw.replaceAll("[\\[\\]\\s]", "").split(",");

        byte[] byteArray = new byte[bytesArray.length];
        for (int i = 0; i < bytesArray.length; i++) {
            byteArray[i] = Byte.parseByte(bytesArray[i].trim());
        }

        return byteArray;
    }
}
