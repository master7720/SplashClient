package installer.auth.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class NetworkConnection {
    private final long updateDelay = 5;
    public Socket connection;
    DataInputStream connectionInput;
    DataOutputStream connectionOutput;
    public ArrayList<Packet> queue = new ArrayList<>();
    public ArrayList<PacketAwait> awaitQueue = new ArrayList<>();
    public ArrayList<PacketBind> packetBinds = new ArrayList<>();

    boolean connected = false;
    public boolean connectionFailed = false;
    public String connectionFailedCause = "";
    ExecutorService threadPool = Executors.newFixedThreadPool(3);
    public NetworkConnection(String ip, int port){
        this.connect(ip,port);
    }

    public void connect(String ip, int port){
        try {
            System.out.println("Trying to connect to: "+ip+":"+port);
            connection = new Socket(ip, port); // try to establish a new socket connection to the server
            System.out.println("Connected!");

            connectionInput = new DataInputStream(connection.getInputStream());
            connectionOutput = new DataOutputStream(connection.getOutputStream());

            connected = true;

            startListening();
        } catch (UnknownHostException e) {
            connectionFailed = true;
            connectionFailedCause = "Unknown Host";
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public void startListening(){
        Runnable read_task = this::startReading;
        Runnable write_task = this::startWriting;

        threadPool.submit(read_task);
        threadPool.submit(write_task);
    }

    public void startReading(){
        System.out.println("Started reading.");

        while(connected){
            try {
                String raw = connectionInput.readUTF();
                handleIncomingMessage(raw);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            sleep();
        }

        System.out.println("Stopped reading.");
    }

    public void handleIncomingMessage(String message) throws Exception {
        String decrypted = Connection.rsaPackets?Utils.decryptRSA(message,Connection.keyPair.getPrivate())
                :Connection.securePackets?Utils.decrypt(message,Connection.secret)
                :message;
        JsonObject json = (JsonObject) JsonParser.parseString(decrypted);

        new Thread(()->{ // i had to make it a thread cuz it kept receiving the packet before it actually add the packet to the await queue
            sleep();
            try {
                handleIncomingPacket(json.get("id").getAsInt(),json.get("data").getAsJsonObject());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void handleIncomingPacket(int id, JsonObject data) throws IOException {
        System.out.println(id+": "+data);
        for(PacketBind bind : packetBinds){
            if(bind.id == id)bind.onReceive.accept(data);
        }

        for(PacketAwait await : awaitQueue){
            if(await.i == id){
                await.r = true;
                await.d=data;
            }
        }

        if(id == Packets.CONNECTION_LOST){
            System.out.println("Disconnected.");
            connected = false;
            connectionOutput.close();
            connectionInput.close();
            connection.close();
        }
    }

    public void startWriting(){
        System.out.println("Started writing.");

        while(connected){
            for (Packet p : queue) {
                sendServer(p);
            }
            queue.clear();

            sleep();
        }

        System.out.println("Stopped writing.");
    }

    private void sleep(){
        try {
            Thread.sleep(updateDelay);
        } catch (InterruptedException ignored) {
        }
    }

    private void sendServer(Packet p) {
        try {
            System.out.println("Sending packet: " + p);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject packetJson = new JsonObject();
            packetJson.addProperty("id", p.id);
            packetJson.add("data", p.data);
            String message = gson.toJson(packetJson);
            String encrypted = Connection.rsaPackets ? Utils.encryptRSA(message, Connection.serverPublicKey)
                    : Connection.securePackets ? Utils.encrypt(message, Connection.secret)
                    : message;

            connectionOutput.writeUTF(encrypted);
            connectionOutput.flush();
            System.out.println("Sent packet: " + p);
        } catch (Exception e) {
            System.out.println("Failed packet: " + p + " (" + p.id + ")");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public void sendServer(int id, JsonObject data){
        sendServer(new Packet(id,data));
    }

    public void sendServer(int id){
        sendServer(new Packet(id,new JsonObject()));
    }

    public void disconnect(){
        System.out.println("Disconnecting...");
        sendServer(Packets.DISCONNECT); // tell server to disconnect the client
    }

    public void sendQueue(int id, JsonObject data){
        queue.add(new Packet(id,data));
    }

    public void sendQueue(int id){
        queue.add(new Packet(id,new JsonObject()));
    }

    public void awaitPacket(int packetId, Consumer<JsonObject> onRecived){
        PacketAwait await = new PacketAwait(packetId, onRecived);
        awaitQueue.add(await);

        while(!await.r){
            sleep();
        }

        System.out.println(await.d);
        await.o.accept(await.d);

        awaitQueue.remove(await);
    }

    public PacketBind bindPacket(int packetId, Consumer<JsonObject> onReceive){
        PacketBind bind = new PacketBind(packetId,onReceive);
        packetBinds.add(bind);
        return bind;
    }

    public void unbindPacket(PacketBind bind){
        packetBinds.remove(bind);
    }

    static class PacketAwait {
        int i;
        Consumer<JsonObject> o;
        boolean r;
        JsonObject d;
        public PacketAwait(int i, Consumer<JsonObject> o){
            this.i = i;
            this.o = o;
            this.r = false;
        }
    }

    static class Packet{
        int id;
        JsonObject data;
        public Packet(int id, JsonObject data){
            this.id = id;
            this.data = data;
        }
    }

    public static class PacketBind{
        int id;
        Consumer<JsonObject> onReceive;
        public PacketBind(int id, Consumer<JsonObject> onReceive){
            this.id = id;
            this.onReceive = onReceive;
        }
    }
}