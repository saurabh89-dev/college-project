
ALTER TABLE messages ADD COLUMN edited_at TIMESTAMP NULL;
ALTER TABLE messages ADD COLUMN deleted_at TIMESTAMP NULL;
CREATE INDEX idx_messages_visible ON messages (conversation_id, deleted_at, sent_at);
