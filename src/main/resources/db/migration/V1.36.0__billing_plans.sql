-- Four plans (spec 018): FREE, BASIC, CHURCH and NETWORK, and the own WhatsApp number as an extra.
-- The church that already paid had the only paid plan there was, so it becomes CHURCH.
ALTER TABLE billing_subscription
    ADD COLUMN plan character varying(20),
    ADD COLUMN own_number boolean NOT NULL DEFAULT false;
UPDATE billing_subscription SET plan = 'CHURCH' WHERE paid_until IS NOT NULL;
ALTER TABLE billing_subscription
    ADD CONSTRAINT billing_subscription_plan_check CHECK (plan IN ('BASIC', 'CHURCH', 'NETWORK'));
-- The own number is no longer tried for free for 15 days: it is a paid extra or part of NETWORK.
ALTER TABLE billing_subscription DROP COLUMN whatsapp_trial_started_at;

ALTER TABLE billing_invoice
    ADD COLUMN plan character varying(20) NOT NULL DEFAULT 'CHURCH',
    ADD COLUMN own_number boolean NOT NULL DEFAULT false;
ALTER TABLE billing_invoice ALTER COLUMN plan DROP DEFAULT;
ALTER TABLE billing_invoice
    ADD CONSTRAINT billing_invoice_plan_check CHECK (plan IN ('BASIC', 'CHURCH', 'NETWORK'));

-- WhatsApp reminders (Meta templates) sent in the month, next to the messages of the AI assistant.
ALTER TABLE billing_ai_usage ADD COLUMN whatsapp_reminders integer NOT NULL DEFAULT 0;
