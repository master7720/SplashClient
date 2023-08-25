package installer.auth.network;

public class Packets {
    public static final int HANDSHAKKE = 0x00;
    public static final int DISCONNECT = 0x01;
    public static final int KEY_EXCHANGE = 0x02;
    public static final int CONNECTION_LOST = 0x03;
    public static final int SEND_KEY = 0x04;
    public static final int ENCRYPT_START = 0x05;
    public static final int SEND_SECRET = 0x06;
    public static final int LOGIN = 0x07;
    public static final int LOGIN_RESPONSE = 0x08;
    public static final int REGISTER = 0x09;
    public static final int REGISTER_RESPONSE = 0x0A;
    public static final int SAVE_DATA = 0x0B;
    public static final int USER_INFO = 0x0C;
    public static final int USER_INFO_RESPONSE = 0x0D;
    public static final int GET_DATA = 0x0E;
    public static final int GET_DATA_RESPONSE = 0x0F;
    public static final int SECRET_OK = 0x11;
    public static final int CONFIG_START_LISTENING_SEND = 0x12;
    public static final int CONFIG_STOP_LISTENING_SEND = 0x13;
    public static final int REQUEST_CONFIG = 0x14;
    public static final int CONFIG_DATA_FINISH = 0x15;
    public static final int CONFIG_CHUNK_SEND = 0x16;
    public static final int CONFIG_CHUNK_RECEIVE = 0x17;
    public static final int LIST_USERS = 0x18;
    public static final int LIST_USERS_RESPONSE = 0x19;
}
