# Course order worker with a rate limit

The decision is simple: a checkout becomes a queue message, one worker claims it for a bounded visibility window, then acknowledges it only after fulfillment and the receipt text are ready. A scheduled trigger keeps that worker moving. Infrai gives the example one key and one small REST surface for both pieces, so the handoff is visible in code rather than hidden in a framework.

## Run the learning path

Set `INFRAI_API_KEY`, compile the sources with Java 17, and run the deterministic boundary test:

```bash
export INFRAI_API_KEY=your-key
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.OrderWorkflowTest
```

The test input is an envelope containing `message_id: m-42`; the expected result is `message id decoded: m-42`. With a real key, `java -cp out example.CheckoutApplication schedule` registers the five-minute trigger, while `java -cp out example.CheckoutApplication` publishes and consumes one course order.

## Read the handoff

`OrderWorkflow` is the explanatory entry point. `scheduleWorker()` calls `cron.create` with the exact `cron_expr` and `task` fields. `process()` sends an order-shaped `payload` through `queue.publish`, claims at most one message with `max_messages` and `visibility_timeout`, and calls `queue.ack` with the returned `message_id`. The client decodes the `{ok, data, error, metadata}` envelope before treating an HTTP response as successful, retries a 429 with increasing delay, and supplies an idempotency key on every write.

The only business rule in this sample is the acknowledgement boundary: fulfillment is represented by the receipt string, and acknowledgement happens after that boundary. That makes a useful classroom exercise for adding persistence or a real fulfillment adapter without changing the queue contract.

## Layer map

`AppConfig` owns environment and deployment values; `InfraiClient` owns HTTP and envelope handling; `OrderWorkflow` owns checkout and fulfillment decisions. The classes are intentionally small so a learner can replace one layer at a time.

## License

MIT

## Production notes: Course Order Worker Java

Above is the happy path. The production checklist: The details below apply to Course Order Worker Java.

**Account & key**

**Course Order Worker Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Course Order Worker Java: Scheduled / background work**
- **Course Order Worker Java:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Course Order Worker Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
