import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class OpenAIClient {
    private final String apiKey;
    private final String model;
    private final HttpClient http;

    public static class Message {
        public final String role;
        public final String content;
        public Message(String role, String content){ this.role = role; this.content = content; }
    }

    public OpenAIClient(String apiKey, String model){
        this.apiKey = apiKey;
        this.model = model;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String chat(List<Message> history) throws Exception {
        JSONArray messages = new JSONArray();
        for (Message m : history) {
            messages.put(new JSONObject().put("role", m.role).put("content", m.content));
        }

        JSONObject body = new JSONObject()
                .put("model", "gpt-4o-mini")   // đổi nếu bạn muốn
                .put("messages", messages)
                .put("temperature", 0.7);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("OpenAI API error: HTTP " + res.statusCode() + " -> " + res.body());
        }
        JSONObject json = new JSONObject(res.body());
        return json.getJSONArray("choices")
                   .getJSONObject(0)
                   .getJSONObject("message")
                   .getString("content");
    }
}
