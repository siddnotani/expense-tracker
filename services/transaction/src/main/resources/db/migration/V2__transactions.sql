CREATE TABLE txn.transactions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    account_id uuid NOT NULL,
    category_id uuid NULL,
    direction text NOT NULL,
    amount_minor bigint NOT NULL CHECK (amount_minor > 0),
    currency char(3) NOT NULL,
    occurred_on date NOT NULL,
    description text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_transactions_user_occurred_on
    ON txn.transactions (user_id, occurred_on DESC);
CREATE INDEX idx_transactions_user_account
    ON txn.transactions (user_id, account_id);
