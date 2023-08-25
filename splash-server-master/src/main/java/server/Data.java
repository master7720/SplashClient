package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Data {
    public static final File USERS_FOLDER = new File("users");
    public static final File SERIAL_KEYS_FOLDER = new File("active-serial-keys");
    public static final File CONFIGS_FOLDER = new File("configs");
    public static final File BAN_FOLDER = new File("banned");
    public static final String passwordCharacters = (
            "abcdefghijklmnopqrstuvwxyz"+
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"+
                    "0123456789"+
                    "!@#$%^&*()-=_+[]{}|;:,.<>?"+
                    "\"'\\/");
    public static final String nameCharacters = "abcdefghijklmnopqrstuvwxyz"+"ABCDEFGHIJKLMNOPQRSTUVWXYZ"+"0123456789"+"_";
    public static void folder(){
        if(!USERS_FOLDER.isFile()) USERS_FOLDER.mkdir();
        if(!SERIAL_KEYS_FOLDER.isFile())SERIAL_KEYS_FOLDER.mkdir();
        if(!BAN_FOLDER.isFile())BAN_FOLDER.mkdir();
        File[] users = USERS_FOLDER.listFiles();
        for(File f : users) {
            Data.checkUser(f.getName().substring(5));
        }
    }

    public static String getUserDirectory(String name){
        return USERS_FOLDER.getPath()+"/user-"+name;
    }

    public static void makeUser(String name, String serialKey, String password){
        String path = getUserDirectory(name);
        String infoPath = path+"/info.json";
        String configsPath = path+"/configs";
        String currentConfigPath = path+"/current-config.json";
        String friendsPath = path+"/friends";
        String tagsPath = path+"/tags";

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject data = new JsonObject();

        data.addProperty("name",name);
        data.addProperty("uid", USERS_FOLDER.listFiles().length);
        data.addProperty("serialKey",serialKey);
        data.addProperty("password",hashPassword(password));
        data.addProperty("hidden",false);
        data.addProperty("messages","Everyone");

        String content = gson.toJson(data);
        new File(path).mkdir();
        new File(configsPath).mkdir();
        new File(friendsPath).mkdir();
        new File(tagsPath).mkdir();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(infoPath))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentConfigPath))) {
            writer.write("{}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void checkUser(String user){
        File tags = new File(getUserDirectory(user)+"/tags");
        File inbox = new File(getUserDirectory(user)+"/inbox");
        if(!tags.exists())tags.mkdir();
        if(!inbox.exists())inbox.mkdir();
        JsonObject data = readUserByName(user);
        if(data.get("hidden")==null)data.addProperty("hidden",false);
        if(data.get("messages")==null)data.addProperty("messages","Everyone");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getUserDirectory(user)+"/info.json"))) {
            writer.write(Packets.makeRaw(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void ban(String address){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BAN_FOLDER.getPath()+"/"+address+".gofuckurself"))) {
            writer.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isIPBanned(String address) {
        return new File(BAN_FOLDER.getPath()+"/"+address+".gofuckurself").exists();
    }

    public static boolean verifyLoginCredentials(String name, String rawPassword){
        JsonObject json = readUserByName(name);
        System.out.println(Packets.makeRaw(json));

        if(json==null)return false;
        return verifyPassword(rawPassword,json.get("password").getAsString());
    }

    public static String verifyRegisterCredentials(String name, File serialKeyFile, String password, String confirm){
        String cause = "";
        if(containsIllegalCharacters(name,nameCharacters)) {
            cause = "Name contains illegal characters";
        }else if(name.length()<3||name.length()>20){
            cause = "Name must be between 3 and 20 characters long";
        }if(Data.isNameFound(name)){
            cause = "Name is already taken";
        }else if(containsIllegalCharacters(password,passwordCharacters)){
            cause = "Password contains illegal characters";
        }else if(password.length()<8||password.length()>128){
            cause = "Password must be between 8 and 128 characters long";
        }else if(!password.equals(confirm)){
            cause = "Passwords don't match";
        }else if(serialKeyFile==null){
            cause = "Invalid serial key";
        }
        return cause;
    }

    public static boolean containsIllegalCharacters(String input, String chars) {
        for (char c : input.toCharArray()) {
            if (chars.indexOf(c) == -1) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNameFound(String nameToCompare){
        return new File(getUserDirectory(nameToCompare)).exists();
    }

    public static JsonObject readUserByName(String name){
        File file = new File(getUserDirectory(name)+"/info.json");
        if(!file.exists())return null;
        try {
            String content = Files.readString(file.toPath());
            return Packets.makeJson(content);
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public static String readUserFile(String name, String fileName){
        File file = new File(getUserDirectory(name)+"/"+fileName);
        if(!file.exists())return null;
        try {
            return Files.readString(Paths.get(file.getPath()));
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public static String hashPassword(String password) {
        String salt = BCrypt.gensalt();

        String hashedPassword = BCrypt.hashpw(password, salt);

        return hashedPassword;
    }

    public static boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

    // SERIAL KEYS
    private static final int SERIAL_KEY_LENGTH = 64;

    private static final String UPPER_CASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;:'<>,.?/";

    public static boolean isActiveSerialKey(String serialKey){
        return getActiveSerialKey(serialKey)!=null;
    }

    public static File getActiveSerialKey(String serialKey){
        for(File file : Objects.requireNonNull(SERIAL_KEYS_FOLDER.listFiles())){
            try {
                List<String> lines = Files.readAllLines(Paths.get(file.getPath()));
                for (String line : lines) {
                    if(line.equals(serialKey))return file;
                }
            }catch(IOException ignored){
                return null;
            }
        }
        return null;
    }

    public static String saveNewSerialKey(){
        LocalDateTime d = LocalDateTime.now();
        String ms = String.valueOf(System.currentTimeMillis());
        String path = SERIAL_KEYS_FOLDER.getPath()+"/key-"+ms+"-"+d.getDayOfMonth()+"-"+d.getMonth()+"-"+d.getYear()+".txt";

        String generatedSerialKey = generateKey();

        String content = generatedSerialKey;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return generatedSerialKey;
    }

    public static String generateKey(){
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        password.append(getRandomChar(UPPER_CASE_CHARS, random));
        password.append(getRandomChar(LOWER_CASE_CHARS, random));
        password.append(getRandomChar(NUMBERS, random));
        password.append(getRandomChar(SPECIAL_CHARS, random));

        int remainingLength = SERIAL_KEY_LENGTH - password.length();
        for (int i = 0; i < remainingLength; i++) {
            String randomCategory = getRandomCategory(random);
            String randomChar = getRandomChar(randomCategory, random);
            password.append(randomChar);
        }

        char[] shuffledPassword = password.toString().toCharArray();
        for (int i = 0; i < shuffledPassword.length; i++) {
            int randomIndex = random.nextInt(shuffledPassword.length);
            char temp = shuffledPassword[i];
            shuffledPassword[i] = shuffledPassword[randomIndex];
            shuffledPassword[randomIndex] = temp;
        }

        return new String(shuffledPassword);
    }

    private static String getRandomChar(String charSet, SecureRandom random) {
        int index = random.nextInt(charSet.length());
        return charSet.substring(index, index + 1);
    }

    private static String getRandomCategory(SecureRandom random) {
        int category = random.nextInt(2);
        if (category == 0) {
            return UPPER_CASE_CHARS;
        }
        return SPECIAL_CHARS;
    }
}
