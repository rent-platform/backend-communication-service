CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE chats (
id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
item_id    UUID,
deal_id    UUID,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
CHECK (item_id IS NOT NULL OR deal_id IS NOT NULL)
);

CREATE TABLE chat_participants (
chat_id UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
user_id UUID NOT NULL,
PRIMARY KEY (chat_id, user_id)
);

CREATE INDEX chat_participants_user_id_idx
    ON chat_participants(user_id);

CREATE TABLE messages (
id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
chat_id    UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
sender_id  UUID NOT NULL,
text       TEXT NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX messages_chat_id_idx
    ON messages(chat_id);

CREATE INDEX messages_sender_id_idx
    ON messages(sender_id);

CREATE INDEX messages_created_at_idx
    ON messages(created_at);

CREATE TABLE message_reads (
message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
user_id    UUID NOT NULL,
read_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
PRIMARY KEY (message_id, user_id)
);

CREATE INDEX message_reads_user_id_idx
    ON message_reads(user_id);

CREATE TABLE reviews (
id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
deal_id      UUID        NOT NULL,
from_user_id UUID        NOT NULL,
to_user_id   UUID        NOT NULL,
rating       INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
text         TEXT,
created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
UNIQUE (deal_id, from_user_id),
CHECK (from_user_id <> to_user_id)
);

CREATE INDEX reviews_to_user_id_idx
    ON reviews(to_user_id);

CREATE INDEX reviews_from_user_id_idx
    ON reviews(from_user_id);

CREATE INDEX reviews_deal_id_idx
    ON reviews(deal_id);

CREATE TABLE complaints (
id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
complainant_id    UUID         NOT NULL,
target_type       VARCHAR(20)  NOT NULL
CHECK (target_type IN ('user', 'item', 'review')),
target_id         UUID NOT NULL,
reason            VARCHAR(100) NOT NULL,
description       TEXT,
status            VARCHAR(20)  NOT NULL DEFAULT 'new'
CHECK (status IN ('new', 'in_progress', 'resolved', 'rejected')),
moderator_comment TEXT,
created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
resolved_at       TIMESTAMPTZ
);

CREATE INDEX complaints_complainant_id_idx
    ON complaints(complainant_id);

CREATE INDEX complaints_target_idx
    ON complaints(target_type, target_id);

CREATE INDEX complaints_status_idx
    ON complaints(status);

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

CREATE TRIGGER trg_reviews_updated_at
BEFORE UPDATE ON reviews
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
