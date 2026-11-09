CREATE INDEX idx_orders_feed_cursor
    ON orders (placed_at DESC, id DESC);
