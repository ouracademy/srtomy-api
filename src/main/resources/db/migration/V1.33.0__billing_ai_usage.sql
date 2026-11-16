-- What the AI assistant used per church and month (spec 018-plans-billing, phase 2). The agent
-- reserves one message before calling the model and records the tokens after it; the counters are
-- only ever changed with one atomic statement, so several instances never go past the limit.
CREATE TABLE billing_ai_usage (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organization(id),
    -- First day of the month the usage belongs to.
    month date NOT NULL,
    messages integer NOT NULL DEFAULT 0,
    input_tokens bigint NOT NULL DEFAULT 0,
    output_tokens bigint NOT NULL DEFAULT 0,
    cache_read_tokens bigint NOT NULL DEFAULT 0,
    cache_write_tokens bigint NOT NULL DEFAULT 0,
    -- Estimated with the prices of billing.ai.prices; what Anthropic bills is the real figure.
    cost_usd numeric(12, 6) NOT NULL DEFAULT 0,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    UNIQUE (organization_id, month)
);
