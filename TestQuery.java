import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestQuery {
    public static void main(String[] args) throws Exception {
        // 1. Get token
        String loginBody = "{\"email\":\"admin@mail.com\",\"password\":\"1\"}";
        HttpRequest loginReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build();
            
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> loginRes = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        
        String token = loginRes.body().split("\"accessToken\":\"")[1].split("\"")[0];
        
        // 2. Query
        String queryBody = "{\"query\":\"tôi muốn nghiên cứu về chủ đề đạo đức của AI\"}";
        HttpRequest queryReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/search/natural"))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(queryBody))
            .build();
            
        HttpResponse<String> queryRes = client.send(queryReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + queryRes.statusCode());
        System.out.println("Body: " + queryRes.body());
    }
}
