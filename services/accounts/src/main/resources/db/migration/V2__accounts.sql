CREATE TABLE accounts.accounts (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    name text NOT NULL,
    type text NOT NULL,
    currency char(3) NOT NULL,
    balance_minor bigint NOT NULL DEFAULT 0,
    archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_accounts_user_id ON accounts.accounts (user_id);
CREATE UNIQUE INDEX uq_accounts_user_lower_name ON accounts.accounts (user_id, lower(name));
