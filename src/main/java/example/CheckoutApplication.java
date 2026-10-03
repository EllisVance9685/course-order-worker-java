package example;

public final class CheckoutApplication {
    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnvironment();
        InfraiClient client = new InfraiClient(config);
        OrderWorkflow workflow = new OrderWorkflow(client, config);
        if (args.length > 0 && args[0].equals("schedule")) {
            System.out.println("scheduled job: " + workflow.scheduleWorker());
            return;
        }
        System.out.println(workflow.process(new Order("order-100", "course-7", 1299)));
    }
}
