CREATE TABLE budget.categories (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    name text NOT NULL,
    color char(7),
    archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX categories_user_lower_name_idx
    ON budget.categories (user_id, lower(name));
CREATE INDEX categories_user_idx ON budget.categories (user_id);

CREATE TABLE budget.monthly_budgets (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    category_id uuid NOT NULL REFERENCES budget.categories(id) ON DELETE CASCADE,
    period date NOT NULL,
    amount_minor bigint NOT NULL CHECK (amount_minor >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT monthly_budgets_user_category_period_key
        UNIQUE (user_id, category_id, period)
);

CREATE INDEX monthly_budgets_user_period_idx
    ON budget.monthly_budgets (user_id, period);
