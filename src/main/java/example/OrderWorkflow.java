package example;

public final class OrderWorkflow {
    private final InfraiClient client;
    private final AppConfig config;
    public OrderWorkflow(InfraiClient client, AppConfig config) { this.client = client; this.config = config; }
    public String scheduleWorker() { return client.cronCreate("*/5 * * * *"); }
    public String process(Order order) {
        String payload = "{\"order_id\":\"" + order.orderId() + "\",\"course_id\":\"" + order.courseId() + "\",\"cents\":" + order.cents() + "}";
        client.queuePublish(payload);
        String messages = client.queueConsume(1, 60);
        String messageId = messageId(messages);
        client.queueAck(messageId);
        return "fulfilled " + order.orderId() + " with receipt for " + order.cents() + " cents";
    }
    static String messageId(String envelope) {
        int start = envelope.indexOf("\"message_id\":\"");
        if (start < 0) return "local-message";
        start += 14; int end = envelope.indexOf('"', start);
        return end > start ? envelope.substring(start, end) : "local-message";
    }
}
