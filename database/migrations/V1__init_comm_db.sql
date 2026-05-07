CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE chats (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id    UUID        NOT NULL,
    owner_id   UUID        NOT NULL,
    renter_id  UUID        NOT NULL,
    deal_id    UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (item_id, owner_id, renter_id)
);

CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id         UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL,
    text            TEXT NOT NULL,
    message_type    VARCHAR(20)  NOT NULL DEFAULT 'USER'
            CHECK (message_type IN ('USER', 'SYSTEM', 'DEAL_STATUS', 'PAYMENT_STATUS')),
    system_payload  JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX messages_chat_id_created_at_idx
    ON messages(chat_id, created_at);

CREATE INDEX messages_sender_id_idx
    ON messages(sender_id);

CREATE TABLE message_reads (
    message_id UUID        NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL,
    read_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id)
);

CREATE INDEX message_reads_user_id_idx
    ON message_reads(user_id);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_chats_updated_at
BEFORE UPDATE ON chats
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();