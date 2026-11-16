-- The buckets of Bucket4j for the rate of the public endpoints per IP (spec 018, phase 4), shared by
-- every instance of the API. The columns are the ones Bucket4j expects; a row is one IP.
CREATE TABLE rate_limit_bucket (
    id character varying(100) PRIMARY KEY,
    state bytea,
    -- Milliseconds since the epoch after which the bucket is full again and the row can go.
    expires_at bigint
);
