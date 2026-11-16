-- Plans and payments (spec 018-plans-billing). A church with no row is on the free plan.
CREATE TABLE billing_subscription (
    organization_id uuid PRIMARY KEY REFERENCES organization(id),
    -- Where the church paid from last time: PE or CO.
    country character varying(2),
    -- Last day of the paid period; null while the church never paid.
    paid_until date,
    -- The free plan may try WhatsApp for a few days, once.
    whatsapp_trial_started_at timestamp(6) without time zone,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

-- One payment of the paid plan. The period is known once paid: it starts where the paid time ends.
CREATE TABLE billing_invoice (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organization(id),
    country character varying(2) NOT NULL,
    currency character varying(3) NOT NULL,
    amount numeric(12, 2) NOT NULL,
    people integer NOT NULL,
    months integer NOT NULL,
    period_start date,
    period_end date,
    status character varying(20) NOT NULL,
    gateway_payment_id character varying(100) UNIQUE,
    checkout_url text,
    paid_at timestamp(6) without time zone,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    CHECK (status IN ('PENDING', 'PAID', 'REJECTED', 'CANCELLED', 'EXPIRED')),
    CHECK (months IN (1, 12))
);

CREATE INDEX billing_invoice_organization_idx ON billing_invoice(organization_id);

-- Paying is for the owner and the administrators.
UPDATE iam_role
SET permissions = array_append(permissions, 'billing.manage')
WHERE id IN ('0195f0a0-0000-7000-8000-000000000001', '0195f0a0-0000-7000-8000-000000000002')
  AND NOT ('billing.manage' = ANY (permissions));
