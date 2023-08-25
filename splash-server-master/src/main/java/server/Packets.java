package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class Packets {
    public static final int HANDSHAKE = 0x00;
    public static final int DISCONNECT = 0x01;
    public static final int SEND_KEY_TO_CLIENT = 0x02;
    public static final int CONNECTION_LOST = 0x03;
    public static final int PUBLIC_KEY_RECEIVE = 0x04;
    public static final int START_ENCRYPT_ARS = 0x05;
    public static final int RECEIVE_SECRET = 0x06;
    public static final int CLIENT_REQUESTED_LOGIN = 0x07;
    public static final int LOGIN_RESPONSE = 0x08;
    public static final int CLIENT_REQUESTED_REGISTER = 0x09;
    public static final int REGISTER_RESPONSE = 0x0A;
    public static final int SAVE_DATA = 0x0B;
    public static final int USER_INFO = 0x0C;
    public static final int USER_INFO_RESPONSE = 0x0D;
    public static final int GET_DATA = 0x0E;
    public static final int GET_DATA_RESPONSE = 0x0F;
    public static final int SECRET_OK = 0x11;
    public static final int START_LISTEN_CONFIG = 0x12;
    public static final int STOP_LISTEN_CONFIG = 0x13;
    public static final int CLIENT_REQUESTED_CONFIG = 0x14;
    public static final int CONFIG_REQUEST_FINISHED = 0x15;
    public static final int CLIENT_SENT_CONFIG_CHUNK = 0x16;
    public static final int SEND_CONFIG_CHUNK = 0x17;
    public static final int LIST_USERS = 0x18;
    public static final int LIST_USERS_RESPONSE = 0x19;
    public static final int ACCOUNT_ACTION = 0x1A;
    public static final int PROFILE_INFO = 0x1B;
    public static final int PROFILE_ACTION = 0x1C;
    public static final int INBOX_DATA = 0x1D;
    public static final int PROFILE_INFO_RESPONSE = 0x1E;

    public static JsonObject makeJson(String message){
        return (JsonObject) JsonParser.parseString(message);
    }

    public static String makeRaw(JsonObject packetJson){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(packetJson);
    }

    // ENCRYPTING
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

    public static String encrypt(String plainText, String secretKey) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
