-- WhatsApp of a church, connected through Kapso (spec 014). One per church node.
CREATE TABLE whatsapp_connection (
    organization_id uuid PRIMARY KEY REFERENCES organization(id),
    -- The customer Kapso keeps for the church; created with the first setup link.
    kapso_customer_id character varying(100) NOT NULL UNIQUE,
    mode character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    -- Known once the pastor finishes connecting.
    phone_number_id character varying(100),
    display_number character varying(50),
    connected_at timestamp(6) without time zone,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    CHECK (mode IN ('COEXISTENCE', 'DEDICATED')),
    CHECK (status IN ('PENDING', 'CONNECTED', 'DISCONNECTED'))
);

-- Webhooks Kapso already delivered, by the key it keeps across retries, so a repeat does nothing.
CREATE TABLE whatsapp_webhook_event (
    idempotency_key character varying(200) PRIMARY KEY,
    received_at timestamp(6) without time zone NOT NULL
);
