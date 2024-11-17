CREATE TABLE currency (
  code CHAR(3) PRIMARY KEY,
  settlement_time INTERVAL NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  precision INT NOT NULL,
  CONSTRAINT valid_settlement_time CHECK (settlement_time > INTERVAL '0 seconds')
);

CREATE TABLE exchange_rate (
  currency_pair CHAR(7) NOT NULL,
  rate DECIMAL(18, 6) NOT NULL,
  effective_date TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (currency_pair, effective_date)
);

CREATE INDEX idx_exchange_rate_lookup
    ON exchange_rate(currency_pair, effective_date DESC);

CREATE TYPE transaction_type AS ENUM (
    'debit',    -- Funds debited from account
    'credit',   -- Funds credited to account
    'lock',     -- Funds locked for transaction
    'unlock',   -- Funds unlocked after transaction
    'margin_lock'       -- margin charged for transaction
    'margin_unlock'       -- margin charged for transaction
);

CREATE TYPE transaction_status AS ENUM (
    'initiated',      -- Initial state
    'funds_locked',   -- Source funds are locked
    'processing',     -- In settlement process
    'completed',      -- Successfully settled
    'failed',        -- Settlement failed
    'expired'        -- Settlement window expired
);
-- Balance snapshot table - current state of balances
CREATE TABLE liquidity_pool (
  currency_code CHAR(3) NOT NULL REFERENCES currency(code) PRIMARY KEY,
  available_balance DECIMAL(18,6) NOT NULL DEFAULT 0,
  locked_balance DECIMAL(18,6) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ,
  CONSTRAINT positive_available_balance CHECK (available_balance >= 0),
  CONSTRAINT positive_locked_balance CHECK (locked_balance >= 0)
);

CREATE TABLE ledger (
 id BIGSERIAL PRIMARY KEY,
 margin DECIMAL(18, 6) DEFAULT 0, --FOR record sake
 currency_code CHAR(3) NOT NULL REFERENCES currency(code),
 from_account VARCHAR(50) NOT NULL,
 to_account VARCHAR(50) NOT NULL,
 transaction_type VARCHAR(50) NOT NULL,
 amount DECIMAL(18,6) NOT NULL,
 transaction_id VARCHAR(50) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 description TEXT
);

CREATE TABLE transaction (
     id BIGSERIAL PRIMARY KEY,
     transfer_id VARCHAR(50) NOT NULL, -- Provided by the client
     internal_transfer_id VARCHAR(50),  -- Used internally
     sender_account VARCHAR(50) NOT NULL,
     receiver_account VARCHAR(50) NOT NULL,
     from_amount DECIMAL(18,6) NOT NULL,
     to_amount DECIMAL(18,6) NOT NULL,
     from_currency CHAR(3) NOT NULL REFERENCES currency(code),
     locked_id BIGINT,
     unlocked_id BIGINT,
     to_currency CHAR(3) NOT NULL REFERENCES currency(code),
     margin DECIMAL(18,6) NOT NULL,
     margin_currency CHAR(3) NOT NULL REFERENCES currency(code),
     fx_rate DECIMAL(18,6),
     rate_effective_date TIMESTAMPTZ NOT NULL,
     margin_rate DECIMAL(5,2) NOT NULL,
     status transaction_status NOT NULL,
     scheduled_settlement_time TIMESTAMPTZ NOT NULL,
     settlement_window INTERVAL NOT NULL,
     settlement_attempts INT DEFAULT 0,
     actual_settlement_time TIMESTAMPTZ,
     settlement_message TEXT,
     settlement_status VARCHAR(50),
     failure_reason TEXT,
     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
     updated_at TIMESTAMPTZ,
     description TEXT NOT NULL ,

     CONSTRAINT valid_settlement_window
         CHECK (settlement_window > INTERVAL '0 seconds'),
    CONSTRAINT valid_scheduled_time
        CHECK (scheduled_settlement_time > created_at),
    CONSTRAINT unique_transfer_currency
        UNIQUE (transfer_id, from_currency, to_currency),
        CONSTRAINT unique_transfer_id UNIQUE (internal_transfer_id)
);

-- failed transaction event table
CREATE TABLE failed_transaction_event (
    id BIGSERIAL PRIMARY KEY,
    transfer_id VARCHAR(50) NOT NULL,
    sender_account VARCHAR(50) NOT NULL,
    receiver_account VARCHAR(50) NOT NULL,
    from_amount DECIMAL(18,6) NOT NULL,
    from_currency CHAR(3) NOT NULL,
    to_currency CHAR(3) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    description TEXT NOT NULL
);

-- Index for transfer lookup with currencies
CREATE INDEX idx_transfer_currency_lookup
    ON transaction(transfer_id, from_currency, to_currency);

-- Indexes for settlement processing
CREATE INDEX idx_transaction_status_settlement
    ON transaction(status, scheduled_settlement_time)
    WHERE status IN ('funds_locked', 'processing');