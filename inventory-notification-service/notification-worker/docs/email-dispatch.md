# Email Dispatch

## Polling Loop

Every 60 seconds (configurable via `NOTIFICATION_POLL_INTERVAL_MS`), the worker:

1. Queries `surplus_alerts WHERE notified = false`
2. For each alert, queries `notification_subscriptions WHERE restaurant_id = alert.restaurant_id`
3. Sends one email per subscriber
4. Writes a record to `notification_logs` (success or failure)
5. Sets `surplus_alert.notified = true` and `notified_at = now()`

Failures on individual emails do not abort the batch — the worker logs the failure and continues with the next subscriber.

## Email Format

Emails are **plain text only** — no HTML templates. This prevents email injection attacks.

**Subject:**
```
FoodWise Surplus Alert: {itemName} available at reduced price!
```

**Body example:**
```
Hello,

{restaurantName} has a surplus alert for {itemName}.

Reason: EXPIRY_SOON
Detected at: 2025-06-01 11:30:00

Visit the app to place an order before stock runs out.

To unsubscribe, log in to FoodWise and manage your alert subscriptions.
```

## Notification Log

Each dispatch attempt writes a row to `notification_logs`:

| Column | Description |
|--------|-------------|
| `alert_id` | FK to `surplus_alerts` |
| `subscriber_email` | Recipient address |
| `sent_at` | Timestamp |
| `success` | Boolean |
| `error_message` | Failure reason if `success = false` |

## SMTP Configuration

In development, Mailhog intercepts all outgoing mail. Access the Mailhog UI at `http://localhost:8025` to inspect sent emails without actually delivering them.

In production, configure a real SMTP provider via environment variables (see [Configuration](configuration.md)).

## Idempotency

Each `SurplusAlert` record is processed exactly once — marked `notified = true` after dispatch. Re-running the worker after a crash may re-send emails for alerts that were being processed at the time of the crash (at-least-once delivery). Consider adding a distributed lock or transactional outbox pattern for strict exactly-once semantics in production.
