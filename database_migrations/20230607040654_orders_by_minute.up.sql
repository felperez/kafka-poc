CREATE TABLE orders_by_minute (
    product_id VARCHAR(255) NOT NULL,
    timegroup TIMESTAMP NOT NULL,
    orders INT,
    PRIMARY KEY (product_id, timegroup)
);
