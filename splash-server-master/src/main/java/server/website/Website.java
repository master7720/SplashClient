package server.website;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import server.Data;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Website {
    public static Map<String, String> sessions = new HashMap<>();
    public static String SESSIONS_DIRECTORY = "website/sessions.s";

    public static void run() throws IOException {
        // decode sessions
        File file = new File(SESSIONS_DIRECTORY);
        if(file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] session = line.split(",");
                sessions.put(session[0], session[1]); // store each session in the map
            }

            reader.close();
        }

        HttpServer website = HttpServer.create(new InetSocketAddress(8080), 0);

        website.createContext("/", new HomeHandler());
        website.createContext("/home", new HomeHandler());
        website.createContext("/account", new AccountHandler());
        website.createContext("/buy", new BuyHandler());
        website.createContext("/register", new RegisterHandler());
        website.createContext("/login", new LoginHandler());
        website.createContext("/signup", new RegisterHandler());
        website.createContext("/signin", new LoginHandler());
        website.createContext("/download", new DownloadHandler());
        website.createContext("/images/logo", new LogoHandler());

        website.setExecutor(null);
        website.start();
        System.out.println("Website started on port 8080");

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            StringBuilder content = new StringBuilder();
            int i = 0;
            for(Map.Entry<String, String> entry : sessions.entrySet()){
                i++;
                content.append(entry.getKey()).append(",").append(entry.getValue());
                if(i<sessions.size()-1)content.append("\n");
            }
            new File(SESSIONS_DIRECTORY).delete();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(SESSIONS_DIRECTORY))) {
                writer.write(content.toString());
            } catch (IOException e) {
                System.err.println("Error while saving sessions!");
                e.printStackTrace();
            }
        }));
    }

    static class LogoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] response = readBinaryFile(new File("website/images/logo.png"));
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String sessionId = getSessionIdFromCookies(exchange.getRequestHeaders().getFirst("Cookie"));
            sendBack(exchange,sessions.containsKey(sessionId)?readHtml("home"):readHtml("home-unlogged"));
        }
    }

    static class BuyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendBack(exchange,readHtml("buy"));
        }
    }

    static class AccountHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String queryString = exchange.getRequestURI().getQuery();
            if (queryString==null) {
                if (!checkSession(exchange)) {
                    exchange.getResponseHeaders().add("Location", "/login");
                    exchange.sendResponseHeaders(302, -1);
                    return;
                }
                ;
                String sessionId = getSessionIdFromCookies(exchange.getRequestHeaders().getFirst("Cookie"));
                sendBack(exchange, readHtml("account", sessions.get(sessionId)));
            }else if(parseQueryParameters(queryString).get("logout").equals("true")){
                if (!checkSession(exchange)) return;

                String cookieValue = "sessionId=" + getSessionIdFromCookies(exchange.getRequestHeaders().getFirst("Cookie")) + "; Max-Age=0";

                exchange.getResponseHeaders().add("Set-Cookie", cookieValue);
                sendBack(exchange, "logged out");
            }
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String queryString = exchange.getRequestURI().getQuery();
            if (queryString==null) {
                if(checkSession(exchange)) {
                    exchange.getResponseHeaders().add("Location", "/");
                    exchange.sendResponseHeaders(302, -1);
                    return;
                };
                sendBack(exchange,readHtml("register"));
            }else{
                if (checkSession(exchange)) return;

                Map<String, String> paramMap = parseQueryParameters(queryString);

                String name = paramMap.get("n");
                String serialKey = paramMap.get("s");
                String password = paramMap.get("p");
                String confirm = paramMap.get("c");

                if (name != null && serialKey != null && password != null && confirm != null) {
                    File serialKeyFile = Data.getActiveSerialKey(serialKey);

                    String cause = Data.verifyRegisterCredentials(name, serialKeyFile, password, confirm);
                    boolean success = cause.equals("");

                    if (success) {
                        serialKeyFile.delete();
                        Data.makeUser(name, serialKey, password);
                        String sessionId = generateSessionId();
                        sessions.put(sessionId, name);
                        exchange.getResponseHeaders().add("Set-Cookie", "sessionId=" + sessionId);
                    }

                    sendBack(exchange, success?"looks legit":cause);
                }else{
                    sendBack(exchange, "");
                }
            }
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String queryString = exchange.getRequestURI().getQuery();
            if(queryString==null) {
                if (checkSession(exchange)) {
                    exchange.getResponseHeaders().add("Location", "/");
                    exchange.sendResponseHeaders(302, -1);
                    return;
                }
                sendBack(exchange, readHtml("login"));
            }else {
                if (checkSession(exchange)) return;

                Map<String, String> paramMap = parseQueryParameters(queryString);

                String name = paramMap.get("n");
                String password = paramMap.get("p");

                if (name != null && password != null) {
                    boolean isValid = Data.verifyLoginCredentials(name, password);
                    String response = isValid ? "" : "Invalid username or password.";
                    if (isValid) {
                        String sessionId = generateSessionId();
                        sessions.put(sessionId, name);
                        exchange.getResponseHeaders().add("Set-Cookie", "sessionId=" + sessionId);
                    }
                    sendBack(exchange, response);
                }
            }
        }
    }

    static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File fileToDownload = new File("download.txt");
            byte[] fileData = readBinaryFile(fileToDownload);

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/octet-stream");
            headers.set("Content-Disposition", "attachment; filename=download.txt");

            exchange.sendResponseHeaders(200, fileData.length);
            OutputStream os = exchange.getResponseBody();
            os.write(fileData);
            os.close();
        }
    }

    private static byte[] readBinaryFile(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fileInputStream.read(data);
        fileInputStream.close();
        return data;
    }

    private static String readHtml(String page) {
        return readHtml(page,"");
    }
    private static String readHtml(String page, String name) {
        File file = new File("website/"+page+".html");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            StringBuilder content = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                content.append(line.replace("&&name&&",name)).append("\n");
            }

            reader.close();

            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "error";
    }

    private static void sendBack(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static String generateSessionId() {
        return Long.toHexString(Double.doubleToLongBits(Math.random()));
    }

    private static boolean checkSession(HttpExchange exchange){
        return sessions.containsKey(getSessionIdFromCookies(exchange.getRequestHeaders().getFirst("Cookie")));
    }

    private static String getSessionIdFromCookies(String cookiesHeader) {
        if (cookiesHeader == null) {
            return null;
        }

        String[] cookies = cookiesHeader.split("; ");
        for (String cookie : cookies) {
            if (cookie.startsWith("sessionId=")) {
                return cookie.substring("sessionId=".length());
            }
        }
        return null;
    }

    private static Map<String, String> parseQueryParameters(String queryString) {
        Map<String, String> paramMap = new HashMap<>();

        String[] paramList = queryString.split("&");
        for (String param : paramList) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String paramName = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String paramValue = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                paramMap.put(paramName, paramValue);
            }
        }

        return paramMap;
    }
}