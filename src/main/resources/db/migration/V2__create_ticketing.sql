
-- V2: Tables du module billetterie

CREATE TABLE customers (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(150) NOT NULL,
                           email VARCHAR(255) NOT NULL UNIQUE,
                           street VARCHAR(255) NOT NULL,
                           city VARCHAR(150) NOT NULL
);

CREATE TABLE loyalty_cards (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               customer_id BIGINT NOT NULL,
                               purchased_at DATE NOT NULL,
                               expires_at DATE NOT NULL,

                               CONSTRAINT fk_loyalty_customer
                                   FOREIGN KEY (customer_id)
                                       REFERENCES customers(id),

                               INDEX idx_loyalty_customer (customer_id)
);

CREATE TABLE ticket_purchases (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  code VARCHAR(100) NOT NULL UNIQUE,
                                  customer_id BIGINT NOT NULL,
                                  concert_id BIGINT NOT NULL,
                                  quantity INT NOT NULL,
                                  total_amount DECIMAL(10,2) NOT NULL,
                                  created_at DATETIME(6) NOT NULL,

                                  CONSTRAINT fk_purchase_customer
                                      FOREIGN KEY (customer_id)
                                          REFERENCES customers(id),

                                  CONSTRAINT fk_purchase_concert
                                      FOREIGN KEY (concert_id)
                                          REFERENCES concerts(id),

                                  CONSTRAINT chk_purchase_quantity
                                      CHECK (quantity BETWEEN 1 AND 6),

                                  INDEX idx_purchase_customer (customer_id),
                                  INDEX idx_purchase_concert (concert_id)
);
