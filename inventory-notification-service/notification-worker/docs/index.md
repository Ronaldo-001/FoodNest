# Notification Worker

The **Notification Worker** is a headless Spring Boot background service that dispatches email notifications to customers when surplus conditions are detected in the inventory.

## Responsibilities

- Poll the `surplus_alerts` table every 60 seconds for unnotified alerts
- For each alert, look up all customers subscribed to that restaurant
- Send a plain-text email to each subscriber via SMTP
- Mark alerts as notified and log dispatch results

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 (no Web starter — background process only) |
| Database | PostgreSQL (`inventory_db`, port 5435) — shared with `inventory-app` |
| Scheduling | Spring `@Scheduled` |
| Email | Spring Mail (`JavaMailSender`) |

## Port

**None** — this service exposes no HTTP endpoints. It is a pure background worker.

## Architecture

```
[surplus_alerts table]
       ↓  (poll every 60s)
[NotificationWorker]
       ↓
[notification_subscriptions table]  →  find subscribers
       ↓
[JavaMailSender]  →  SMTP (Mailhog in dev / real SMTP in prod)
       ↓
[notification_logs table]  →  audit log
       ↓
surplus_alert.notified = true
```

## Shared Database

This service connects to the same `inventory_db` PostgreSQL instance as `inventory-app`. **It does not run Flyway migrations** — schema ownership belongs to `inventory-app`.

## Related Pages

- [Email Dispatch](email-dispatch.md)
- [Configuration](configuration.md)
