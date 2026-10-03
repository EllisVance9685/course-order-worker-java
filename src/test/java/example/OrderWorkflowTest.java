package example;

public final class OrderWorkflowTest {
    public static void main(String[] args) {
        String id = OrderWorkflow.messageId("{\"ok\":true,\"data\":{\"message_id\":\"m-42\"}}");
        if (!id.equals("m-42")) throw new AssertionError(id);
        System.out.println("message id decoded: " + id);
    }
}
