import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server {

    private static List<String> requests = new ArrayList<>();
    private static int nextNumber = 1;
    private static final String DATA_FILE = "requests.txt";
    private static final String NUMBER_FILE = "number.txt";

    public static void main(String[] args) throws Exception {
        // Загружаем сохраненные заявки
        loadData();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();

                System.out.println(method + " " + path);

                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

                if (method.equals("OPTIONS")) {
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                    return;
                }

                String response;
                int statusCode = 200;

                if (path.equals("/api/requests") && method.equals("GET")) {
                    response = "[" + String.join(",", requests) + "]";
                    if (requests.isEmpty()) response = "[]";
                }
                else if (path.equals("/api/requests") && method.equals("POST")) {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        body.append(line);
                    }

                    String newRequest = body.toString();
                    String numberedRequest = newRequest.replace("\"number\":0", "\"number\":" + nextNumber);
                    requests.add(numberedRequest);
                    nextNumber++;
                    saveData();

                    response = "{\"success\": true, \"message\": \"Заявка №" + (nextNumber-1) + " создана\"}";
                }
                else if (path.equals("/api/stats") && method.equals("GET")) {
                    response = "{\"requests\": " + requests.size() + "}";
                }
                else {
                    response = "Server is running! Requests: " + requests.size();
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                }

                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
            }
        });

        server.start();
        System.out.println("Server started on port " + port);
        System.out.println("Requests count: " + requests.size());
    }

    private static void loadData() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        requests.add(line);
                    }
                }
                reader.close();
            }

            File numFile = new File(NUMBER_FILE);
            if (numFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(numFile));
                nextNumber = Integer.parseInt(reader.readLine());
                reader.close();
            }
        } catch (Exception e) {
            System.out.println("Error loading: " + e.getMessage());
        }
    }

    private static void saveData() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE));
            for (String req : requests) {
                writer.write(req);
                writer.newLine();
            }
            writer.close();

            BufferedWriter numWriter = new BufferedWriter(new FileWriter(NUMBER_FILE));
            numWriter.write(String.valueOf(nextNumber));
            numWriter.close();
        } catch (Exception e) {
            System.out.println("Error saving: " + e.getMessage());
        }
    }
}