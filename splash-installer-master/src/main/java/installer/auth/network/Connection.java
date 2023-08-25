package installer.auth.network;

import com.google.gson.JsonObject;

import javax.crypto.SecretKey;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class Connection {
    public static String SERVER = "13.38.118.193";
    public static int PORT = 6974;
    public static NetworkConnection network;
    public static PublicKey serverPublicKey;
    public static boolean rsaPackets = false;
    public static boolean securePackets = false;
    public static SecretKey secret;
    public static KeyPair keyPair;

    public static boolean init() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
        System.out.println(keyPair.getPublic());

        // CONNECT
        network = new NetworkConnection(SERVER, PORT); // connect to server
        if (network.connectionFailed) {
            System.out.println("Unable to connect to server: " + network.connectionFailedCause);
            return false;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(network::disconnect));

        network.sendQueue(Packets.HANDSHAKKE);
        network.awaitPacket(Packets.KEY_EXCHANGE, data -> { // wait for server to respond with it's public key
            String raw = data.get("key").getAsString();

            try {
                serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Utils.stringToByteArray(raw)));
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            secret = Utils.generateSecret();

            JsonObject c04data = new JsonObject();
            PublicKey clientPublicKey = keyPair.getPublic();
            c04data.addProperty("key", Arrays.toString(clientPublicKey.getEncoded()));

            network.sendQueue(Packets.SEND_KEY,c04data);
            network.awaitPacket(Packets.ENCRYPT_START, unused -> {
                rsaPackets = true;

                JsonObject c06data = new JsonObject();
                c06data.addProperty("secret", Arrays.toString(secret.getEncoded()));
                network.sendQueue(Packets.SEND_SECRET,c06data);
                network.awaitPacket(Packets.SECRET_OK, u -> {
                    rsaPackets = false;
                    securePackets = true;
                });
            });
        });

        return true;
    }
}
