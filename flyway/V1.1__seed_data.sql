-- Insert sample currencies with their respective cron expressions
-- Seed Data for Currency Table with Settlement Times

INSERT INTO currency (code, precision,  settlement_time)
VALUES
    ('USD',  2, INTERVAL '3 seconds'),
    ('EUR',  2, INTERVAL '2 seconds'),
    ('JPY', 0,  INTERVAL '3 seconds'),
    ('GBP', 2,  INTERVAL '2 seconds'),
    ('AUD', 2,  INTERVAL '3 seconds');

-- Balance Snapshot for USD
INSERT INTO liquidity_pool (currency_code, available_balance, locked_balance)
VALUES ( 'USD', 1000000.00, 0);

-- Balance Snapshot for EUR
INSERT INTO liquidity_pool ( currency_code, available_balance, locked_balance)
VALUES ('EUR', 921658.00, 0);

-- Balance Snapshot for JPY
INSERT INTO liquidity_pool ( currency_code, available_balance, locked_balance)
VALUES ('JPY', 109890110.00, 0);

-- Balance Snapshot for GBP
INSERT INTO liquidity_pool ( currency_code, available_balance, locked_balance)
VALUES ('GBP', 750000.00, 0);

-- Balance Snapshot for AUD
INSERT INTO liquidity_pool (currency_code, available_balance, locked_balance)
VALUES ('AUD', 1349528.00, 0);

