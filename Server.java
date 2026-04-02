import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Server {
    
    // ЗАМЕНИТЕ НА ВАШ ДОМАШНИЙ IP АДРЕС
    // КОГДА СМЕНИТЕ IP - НУЖНО ОБНОВИТЬ КОД И ПЕРЕЗАЛИТЬ НА GITHUB
    private static final String DATA_SERVER = "http://192.168.0.235:8081";  // ← ВАШ IP!
    
    public static void main(String[] args) throws Exception {
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
                    response = fetchFromDataServer("GET", null);
                }
                else if (path.equals("/api/requests") && method.equals("POST")) {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        body.append(line);
                    }
                    response = fetchFromDataServer("POST", body.toString());
                }
                else if (path.equals("/api/stats") && method.equals("GET")) {
                    response = "{\"status\": \"proxy active\"}";
                }
                else {
                    response = "✅ Proxy server working!";
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
        System.out.println("🚀 Proxy server started on port " + port);
        System.out.println("📡 Connecting to data server: " + DATA_SERVER);
    }
    
    private static String fetchFromDataServer(String method, String data) {
        try {
            URL url = new URL(DATA_SERVER + "/api/data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            if (method.equals("POST") && data != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            
            return result.toString();
        } catch (Exception e) {
            System.out.println("Error connecting to data server: " + e.getMessage());
            return method.equals("GET") ? "[]" : "{\"success\": false}";
        }
    }
}