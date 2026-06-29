# Configuration — Notification Worker

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `INVENTORY_DB_URL` | — | **Required.** JDBC URL for the shared `inventory_db`, e.g. `jdbc:postgresql://localhost:5435/inventory_db` |
| `INVENTORY_DB_USER` | — | PostgreSQL username |
| `INVENTORY_DB_PASSWORD` | — | PostgreSQL password |
| `NOTIFICATION_POLL_INTERVAL_MS` | `60000` | How often to poll for unnotified alerts (milliseconds) |
| `MAIL_HOST` | `mailhog` | SMTP server hostname |
| `MAIL_PORT` | `1025` | SMTP server port |
| `MAIL_USERNAME` | _(empty)_ | SMTP username (not required for Mailhog) |
| `MAIL_PASSWORD` | _(empty)_ | SMTP password |
| `MAIL_FROM` | `noreply@foodwise.local` | Sender email address |

## application.yml (key excerpts)

```yaml
spring:
  datasource:
    url: ${INVENTORY_DB_URL}
    username: ${INVENTORY_DB_USER}
    password: ${INVENTORY_DB_PASSWORD}
  flyway:
    enabled: false  # migrations managed by inventory-app
  mail:
    host: ${MAIL_HOST:mailhog}
    port: ${MAIL_PORT:1025}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}

app:
  notification:
    poll-interval-ms: ${NOTIFICATION_POLL_INTERVAL_MS:60000}
    from-address: ${MAIL_FROM:noreply@foodwise.local}
```

## Development Setup (Mailhog)

In the Docker Compose development environment, all emails are captured by Mailhog instead of being delivered. View captured emails at:

```
http://localhost:8025
```

No SMTP credentials are required — Mailhog accepts all connections.

## Production Considerations

- Replace Mailhog with a real SMTP provider (SendGrid, AWS SES, Postmark, etc.)
- Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` accordingly
- Consider replacing the polling mechanism with Kafka or RabbitMQ for lower latency and better scalability
