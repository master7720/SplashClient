package server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Map;

import static server.Data.USERS_FOLDER;
import static server.Data.ban;
import static server.Main.logger;

public class ServerConnection {
    public Socket client;
    public String address;
    private PublicKey clientKey;
    private SecretKey clientSecret;
    private DataInputStream connectionInput;
    private DataOutputStream connectionOutput;
    private boolean encryptARS = false;
    private boolean secure = false;
    private String listeningOnConfig;
    private boolean listeningOnCurrent = true;
    private JsonObject configBuffer;
    public int userId;
    public String username;
    public boolean loggedIn;
    private final ArrayList<String> buffer = new ArrayList<>();

    public ServerConnection(Socket client){
        this.client = client;
        String realipok = client.getInetAddress().getHostAddress();
        this.address = Packets.generateSHA1Hash(realipok);
        this.userId = -1;
    }
    public void handleClient() throws Exception {
        logger.log("Connected: "+address);

        Thread bufferThread = new Thread(()->{
            while (sleep(5)&&!client.isClosed()) {

                if(buffer.size()==0)continue;

                try {
                    readBuffer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        bufferThread.start();

        while (client.isConnected() && !client.isClosed()) {;
            String message = connectionInput.readUTF();
            buffer.add(message);
        }

        handleDisconnect();
    }

    public void readBuffer() throws Exception {
        String message = buffer.get(0);
        String decrypted = encryptARS?Packets.decryptRSA(message,Main.keyPair.getPrivate())
                :secure?Packets.decrypt(message,clientSecret)
                :message;
        buffer.remove(0);

        logger.log("Raw message: "+message);

        JsonObject packet = Packets.makeJson(decrypted);

        handlePacket(packet);
    }

    public void handlePacket(JsonObject packet) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        logger.log(address+" sent packet: "+ packet);

        int id = packet.get("id").getAsInt();
        JsonObject data = packet.get("data").getAsJsonObject();
        switch (id){ // TODO
            case Packets.HANDSHAKE -> {
                JsonObject sendKeyPacket = new JsonObject();
                sendKeyPacket.addProperty("id",Packets.SEND_KEY_TO_CLIENT);

                JsonObject packetData = new JsonObject();
                packetData.addProperty("key", Main.publicEncoded);

                sendKeyPacket.add("data",packetData);

                sendClient(Packets.makeRaw(sendKeyPacket));
            }
            case Packets.DISCONNECT -> {
                sendPacket(Packets.CONNECTION_LOST,new JsonObject());
                handleDisconnect();
            }
            case Packets.PUBLIC_KEY_RECEIVE -> { // will handle the response of the client containing the key and the secret
                byte[] keyBytes = Packets.stringToByteArray(data.get("key").getAsString());
                clientKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
                sendPacket(Packets.START_ENCRYPT_ARS,new JsonObject());
                encryptARS = true;
                logger.log("Server can now start sending encrypted packets with: "+address);
            }
            case Packets.RECEIVE_SECRET -> {
                clientSecret = new SecretKeySpec(Packets.stringToByteArray(data.get("secret").getAsString()),"AES");
                sendPacket(Packets.SECRET_OK,new JsonObject());
                encryptARS = false;
                secure = true;
            }
            case Packets.CLIENT_REQUESTED_LOGIN -> {
                if(loggedIn){
                    tamperDetected();
                    return;
                }

                System.out.println(data.toString());
                String name = data.get("name").getAsString();
                String password = data.get("password").getAsString();

                if(name==null||password==null){ // eureka stop being sussy
                    tamperDetected();
                    return;
                }

                boolean success = Data.verifyLoginCredentials(name,password);

                JsonObject c08data = new JsonObject();
                c08data.addProperty("cause",!success?"Invalid username or password.":"");
                c08data.addProperty("success",success);

                if(success) {
                    userId = Data.readUserByName(name).get("uid").getAsInt();
                    username = Data.readUserByName(name).get("name").getAsString();
                    loggedIn = true;
                    Main.loggedInConenctions.put(username,this);
                }
                sendPacket(Packets.LOGIN_RESPONSE,c08data);
            }
            case Packets.CLIENT_REQUESTED_REGISTER -> {
                String name = data.get("name").getAsString();
                String serialKey = data.get("serialKey").getAsString();
                String password = data.get("password").getAsString();
                String confirm = data.get("confirm").getAsString();

                File serialKeyFile = Data.getActiveSerialKey(serialKey);

                String cause = Data.verifyRegisterCredentials(name,serialKeyFile,password,confirm);
                boolean success = cause.equals("");

                JsonObject responsePacket = new JsonObject();
                responsePacket.addProperty("success",success);
                responsePacket.addProperty("cause",cause);

                if(success){
                    serialKeyFile.delete();
                    Data.makeUser(name,serialKey,password);
                    loggedIn = true;
                }

                sendPacket(Packets.REGISTER_RESPONSE,responsePacket);
            }
            case Packets.SAVE_DATA -> {} // for other things might not need rn
            case Packets.GET_DATA -> {}
            case Packets.START_LISTEN_CONFIG -> {
                listeningOnConfig = data.get("config-name").getAsString();
                listeningOnCurrent = data.get("current").getAsBoolean();
                configBuffer = new JsonObject();
            }
            case Packets.STOP_LISTEN_CONFIG -> {
                String path = listeningOnCurrent?Data.getUserDirectory(username)+"/current-config.json":Data.getUserDirectory(username)+"/configs/"+listeningOnConfig+".json";

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
                    writer.write(Packets.makeRaw(configBuffer));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listeningOnConfig = null;
                configBuffer = null;
                listeningOnCurrent = false;
            }
            case Packets.CLIENT_SENT_CONFIG_CHUNK -> {
                configBuffer.add(data.get("module").getAsString(),data.get("settings").getAsJsonObject());
            }
            case Packets.CLIENT_REQUESTED_CONFIG -> {
                String configName = data.get("config-name").getAsString();
                listeningOnCurrent = data.get("current").getAsBoolean();
                JsonObject config = Packets.makeJson(Data.readUserFile(username,listeningOnCurrent?"current-config.json":"configs/"+configName+".json"));

                for(final Map.Entry<String, JsonElement> entry : config.entrySet()){
                    JsonObject moduleData = new JsonObject();
                    moduleData.addProperty("module",entry.getKey());
                    moduleData.add("settings",entry.getValue());
                    sendPacket(Packets.SEND_CONFIG_CHUNK, moduleData);
                }
                sendPacket(Packets.CONFIG_REQUEST_FINISHED, new JsonObject());
            }
            case Packets.USER_INFO -> {
                if(!loggedIn){
                    tamperDetected();
                    return;
                }

                String dir = Data.getUserDirectory(username);
                JsonObject userData = Data.readUserByName(username);

                JsonObject c0dData = new JsonObject();
                c0dData.addProperty("uid",userData.get("uid").getAsString());
                c0dData.addProperty("name",userData.get("name").getAsString());
                c0dData.addProperty("hidden",userData.get("hidden").getAsBoolean());
                c0dData.addProperty("messages",userData.get("messages").getAsString());

                JsonObject c1dData = new JsonObject();
                JsonObject messages = new JsonObject();

                File[] inbox = new File(dir+"/inbox").listFiles();
                for(File f : inbox){
                    messages.add(f.getName(),Packets.makeJson(Files.readString(f.toPath())));
                }

                c1dData.addProperty("type","list");
                c1dData.add("messages",messages);

                sendPacket(Packets.USER_INFO_RESPONSE,c0dData);
                sendPacket(Packets.INBOX_DATA,c1dData);
            }
            case Packets.LIST_USERS -> {
                File[] users = USERS_FOLDER.listFiles();
                JsonObject c19data = new JsonObject();
                for(File f : users){
                    if(new File(f.getPath()+"/tags/hidden.tag").exists())continue;
                    System.out.println(f.getName());
                    c19data.addProperty(f.getName().substring(5),"");
                }
                sendPacket(Packets.LIST_USERS_RESPONSE,c19data);
            }
            case Packets.ACCOUNT_ACTION -> {
                File tags = new File(Data.getUserDirectory(username)+"/tags");
                String t = data.get("type").getAsString();
                JsonObject userData = Data.readUserByName(username);
                if(t.equals("hidden")){
                    boolean v = data.get("value").getAsBoolean();
                    userData.addProperty("hidden",v);
                    File tag = new File(tags+"/hidden.tag");
                    if(v&&!tag.exists())tag.mkdir();
                    if(!v&&tag.exists())tag.delete();
                }else if(t.equals("message")){
                    userData.addProperty("message",data.get("value").getAsString());
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(Data.getUserDirectory(username)+"/info.json"))) {
                    writer.write(Packets.makeRaw(userData));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            case Packets.PROFILE_INFO -> {
                if(!loggedIn)return;
                String n = data.get("name").getAsString();
                JsonObject d = Data.readUserByName(n);
                if(d==null)return;

                JsonObject r = new JsonObject();
                r.addProperty("message-availability",d.get("messages").getAsString());

                sendPacket(Packets.PROFILE_INFO_RESPONSE,r);
            }
            case Packets.PROFILE_ACTION -> {
                String type = data.get("type").getAsString();
                String user = data.get("user").getAsString();
                JsonObject d = Data.readUserByName(user);
                if(type.equals("message")){
                    if(d.get("messages").getAsString().equals("NoOne"))return;
                    File[] inbox = new File(Data.getUserDirectory(user)+"/inbox").listFiles();

                    String txt = data.get("text").getAsString();

                    JsonObject inboxData = new JsonObject();
                    inboxData.addProperty("author",username);
                    inboxData.addProperty("text",txt);

                    String fileName = "0.json";
                    if(inbox.length>0){
                        String l = inbox[inbox.length-1].getName();
                        fileName = (Integer.parseInt(l.substring(0,l.length()-5))+1)+".json";
                    }
                    Files.writeString(new File(Data.getUserDirectory(user)+"/inbox/"+fileName).toPath(),Packets.makeRaw(inboxData));
                    if(Main.loggedInConenctions.get(user)!=null){
                        Main.loggedInConenctions.get(user).sendPacket(Packets.INBOX_DATA,inboxData);
                    }
                }
            }
        }
    }

    private void tamperDetected() throws IOException {
        logger.log("Bad packets from: "+address+"\nBanned!");
        Data.ban(address);
        sendPacket(Packets.CONNECTION_LOST,new JsonObject());
        handleDisconnect();
    }

    public void handleDisconnect() throws IOException {
        if(!client.isConnected()||client.isClosed())return;
        connectionInput.close();
        connectionOutput.close();
        client.close();
        logger.log("Disconnected: "+address);
        Main.connections.remove(this);
    }

    public void sendPacket(int id,JsonObject packetData){
        JsonObject packet = new JsonObject();
        packet.addProperty("id",id);
        packet.add("data",packetData);
        sendClient(Packets.makeRaw(packet));
    }

    public void sendClient(String message){
        try {
            String encrypted = encryptARS?Packets.encryptRSA(message,clientKey)
                    :secure?Packets.encrypt(message,clientSecret)
                    :message;
            connectionOutput.writeUTF(encrypted);
            connectionOutput.flush();
            logger.log("Sent packet to "+address+": "+message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean sleep(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
        return true;
    }

    public void start(){
        logger.log("Connecting "+address+" to the network.");
        try {
            connectionInput = new DataInputStream(client.getInputStream());
            connectionOutput = new DataOutputStream(client.getOutputStream());

            handleClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
