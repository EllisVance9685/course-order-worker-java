package example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private final AppConfig config;
    private final HttpClient http = HttpClient.newHttpClient();
    public InfraiClient(AppConfig config) { this.config = config; }

    // cron.create is the scheduled trigger that starts the queue worker.
    public String cronCreate(String expression) { return post("/v1/cron/create", "{\"cron_expr\":\"" + expression + "\",\"task\":\"" + config.workerUrl() + "\"}", "cron-" + expression); }
    public String queuePublish(String payload) { return post("/v1/queue/publish", "{\"queue\":\"" + config.queueName() + "\",\"payload\":" + payload + "}", "order-" + payload.hashCode()); }
    public String queueConsume(int maxMessages, int visibilityTimeout) { return post("/v1/queue/consume", "{\"queue\":\"" + config.queueName() + "\",\"max_messages\":" + maxMessages + ",\"visibility_timeout\":" + visibilityTimeout + "}", "consume-" + maxMessages); }
    public String queueAck(String messageId) { return post("/v1/queue/ack", "{\"queue\":\"" + config.queueName() + "\",\"message_id\":\"" + messageId + "\"}", "ack-" + messageId); }

    private String post(String path, String body, String idempotencyKey) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json").header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                String envelope = response.body();
                if (envelope.contains("\"ok\":false")) throw new IllegalStateException("Infrai rejected request: " + envelope);
                if (response.statusCode() == 429) {
                    long delay = response.headers().firstValue("Retry-After").map(this::retryAfterMillis).orElse(250L * (1L << attempt));
                    Thread.sleep(delay); continue;
                }
                if (response.statusCode() >= 500) throw new IllegalStateException("Infrai service response: " + response.statusCode());
                return envelope;
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
              catch (java.io.IOException e) { throw new IllegalStateException(e); }
        }
        throw new IllegalStateException("request was rate limited");
    }
    private long retryAfterMillis(String value) {
        try { return Math.max(100L, Long.parseLong(value) * 1000L); }
        catch (NumberFormatException ignored) { return 250L; }
    }
}
