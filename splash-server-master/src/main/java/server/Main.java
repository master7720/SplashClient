package server;

import com.google.gson.JsonObject;
import server.website.Website;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main{
    public static int PORT = 6974;
    public static int MAX_CONNECTIONS = 5000;
    public static ArrayList<ServerConnection> connections = new ArrayList<>();
    public static HashMap<String,ServerConnection> loggedInConenctions = new HashMap<>();
    public static KeyPair keyPair;
    public static String publicEncoded;
    public static Logs logger;

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        logger = new Logs();

        Data.folder();

        DiscordBot.init();
        Website.run();

        ExecutorService clientThreads = Executors.newFixedThreadPool(MAX_CONNECTIONS);

        ServerSocket server = new ServerSocket(PORT);
        logger.log("Started server with ip: "+server.getInetAddress().getHostAddress());

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        publicEncoded = Arrays.toString(keyPair.getPublic().getEncoded());
        logger.log("Server's public key is "+keyPair.getPublic());

        AtomicBoolean stop = new AtomicBoolean(false);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            stop.set(true);
            connections.forEach(connection->connection.sendPacket(Packets.CONNECTION_LOST,new JsonObject()));
        }));

        while(!stop.get()){
            Socket client;
            boolean disconnect = false;

            try{
                client = server.accept();

                if(connections.size()>=MAX_CONNECTIONS){
                    disconnect = true;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ServerConnection connection = new ServerConnection(client);
            clientThreads.submit(connection::start);
            connections.add(connection);

            if(disconnect){
                logger.log("Disconnecting "+connection.address+": Server is full!");
                connection.sendPacket(Packets.CONNECTION_LOST,new JsonObject());
                connection.handleDisconnect();
            }else if(Data.isIPBanned(connection.address)){
                logger.log("Disconnecting "+connection.address+": IP is banned!");
                connection.sendPacket(Packets.CONNECTION_LOST,new JsonObject());
                connection.handleDisconnect();
            }
        }
    }
}
