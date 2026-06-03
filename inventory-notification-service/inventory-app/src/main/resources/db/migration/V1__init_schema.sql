-- V1: Initial schema for inventory_db

CREATE TABLE IF NOT EXISTS inventory_items (
    id                  BIGSERIAL PRIMARY KEY,
    restaurant_id       BIGINT          NOT NULL,
    menu_item_id        BIGINT          NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    quantity            INT             NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    unit                VARCHAR(50),
    threshold           INT             NOT NULL DEFAULT 10,
    expiry_date         DATE,
    is_surplus          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (restaurant_id, menu_item_id)
);

CREATE TABLE IF NOT EXISTS surplus_alerts (
    id                  BIGSERIAL PRIMARY KEY,
    inventory_item_id   BIGINT          NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    restaurant_id       BIGINT          NOT NULL,
    reason              VARCHAR(50)     NOT NULL,   -- 'LOW_STOCK' or 'EXPIRY_SOON'
    triggered_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    notified            BOOLEAN         NOT NULL DEFAULT FALSE,
    notified_at         TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS notification_subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT          NOT NULL,
    customer_email  VARCHAR(255)    NOT NULL,
    restaurant_id   BIGINT          NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (customer_id, restaurant_id)
);

CREATE TABLE IF NOT EXISTS notification_logs (
    id              BIGSERIAL PRIMARY KEY,
    alert_id        BIGINT          NOT NULL REFERENCES surplus_alerts(id),
    customer_email  VARCHAR(255)    NOT NULL,
    subject         VARCHAR(512)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'SENT',  -- 'SENT', 'FAILED'
    sent_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    error_message   TEXT
);

-- Indexes
CREATE INDEX idx_inventory_restaurant     ON inventory_items(restaurant_id);
CREATE INDEX idx_inventory_surplus        ON inventory_items(is_surplus, restaurant_id);
CREATE INDEX idx_inventory_expiry         ON inventory_items(expiry_date) WHERE expiry_date IS NOT NULL;
CREATE INDEX idx_surplus_alerts_notified  ON surplus_alerts(notified) WHERE notified = FALSE;
CREATE INDEX idx_surplus_alerts_restaurant ON surplus_alerts(restaurant_id);
CREATE INDEX idx_subscriptions_restaurant ON notification_subscriptions(restaurant_id);
CREATE INDEX idx_subscriptions_customer   ON notification_subscriptions(customer_id);
