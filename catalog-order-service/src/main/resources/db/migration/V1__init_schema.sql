-- V1: Initial schema for catalog_db

CREATE TABLE IF NOT EXISTS menu_items (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT          NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT,
    price           NUMERIC(10,2)   NOT NULL CHECK (price >= 0),
    category        VARCHAR(100),
    available       BOOLEAN         NOT NULL DEFAULT TRUE,
    image_url       VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT          NOT NULL,
    restaurant_id   BIGINT          NOT NULL,
    status          VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12,2)   NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id    BIGINT          NOT NULL,
    menu_item_name  VARCHAR(255)    NOT NULL,
    quantity        INT             NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(10,2)   NOT NULL CHECK (unit_price >= 0),
    subtotal        NUMERIC(12,2)   NOT NULL
);

-- Indexes
CREATE INDEX idx_menu_items_restaurant ON menu_items(restaurant_id);
CREATE INDEX idx_menu_items_available  ON menu_items(restaurant_id, available);
CREATE INDEX idx_orders_customer       ON orders(customer_id);
CREATE INDEX idx_orders_restaurant     ON orders(restaurant_id);
CREATE INDEX idx_orders_status         ON orders(status);
CREATE INDEX idx_order_items_order     ON order_items(order_id);
