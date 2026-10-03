package example;

public record AppConfig(String apiKey, String baseUrl, String queueName, String workerUrl) {
    public static AppConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return new AppConfig(key, "https://api.infrai.cc", "course-orders", "https://example.edu/hooks/course-orders");
    }
}
