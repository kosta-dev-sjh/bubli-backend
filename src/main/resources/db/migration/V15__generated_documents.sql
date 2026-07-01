CREATE TABLE generated_documents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    room_id UUID,
    suggestion_id UUID NOT NULL,
    resource_id UUID,
    title VARCHAR(255) NOT NULL,
    document_type VARCHAR(60) NOT NULL,
    content_markdown TEXT NOT NULL,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_generated_documents_suggestion UNIQUE (suggestion_id),
    CONSTRAINT fk_generated_documents_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_generated_documents_room FOREIGN KEY (room_id) REFERENCES project_rooms(id),
    CONSTRAINT fk_generated_documents_suggestion FOREIGN KEY (suggestion_id) REFERENCES agent_suggestions(id),
    CONSTRAINT fk_generated_documents_resource FOREIGN KEY (resource_id) REFERENCES resources(id)
);

CREATE INDEX idx_generated_documents_user ON generated_documents (user_id);
CREATE INDEX idx_generated_documents_room ON generated_documents (room_id);
CREATE INDEX idx_generated_documents_resource ON generated_documents (resource_id);
