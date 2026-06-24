CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    google_sub VARCHAR(120) NOT NULL UNIQUE,
    bubli_id VARCHAR(40) UNIQUE,
    name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    locale VARCHAR(20),
    timezone VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    refresh_token VARCHAR(500) NOT NULL UNIQUE,
    client_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_user_sessions_user_client UNIQUE (user_id, client_type)
);

CREATE TABLE user_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    theme VARCHAR(30),
    font_scale DOUBLE PRECISION,
    default_room_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE user_notification_preferences (
    user_id UUID NOT NULL,
    notification_type VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    PRIMARY KEY (user_id, notification_type)
);

CREATE TABLE user_privacy_consents (
    user_id UUID NOT NULL,
    consent_type VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, consent_type)
);

CREATE TABLE friend_requests (
    id UUID PRIMARY KEY,
    requester_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_friend_requests_pair UNIQUE (requester_id, receiver_id)
);

CREATE TABLE friendships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    friend_user_id UUID NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_friendships_pair UNIQUE (user_id, friend_user_id)
);

CREATE TABLE project_rooms (
    id UUID PRIMARY KEY,
    created_by_user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    client_name VARCHAR(120),
    contract_amount NUMERIC(15, 2),
    payment_status VARCHAR(30) NOT NULL,
    payment_due_date DATE,
    paid_at DATE,
    status VARCHAR(30) NOT NULL,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE room_members (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_room_members_room_user UNIQUE (room_id, user_id)
);

CREATE TABLE invitations (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    inviter_user_id UUID NOT NULL,
    invitee_user_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE project_room_events (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    sequence BIGINT NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    actor_user_id UUID,
    payload_json JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_project_room_events_room_sequence UNIQUE (room_id, sequence)
);

CREATE TABLE resources (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    room_id UUID,
    title VARCHAR(200) NOT NULL,
    kind VARCHAR(30) NOT NULL,
    visibility VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE resource_files (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE resource_versions (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    file_id UUID NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_resource_versions_resource_version UNIQUE (resource_id, version_no)
);

CREATE TABLE resource_summaries (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    job_id UUID NOT NULL,
    summary_json JSONB NOT NULL,
    checklist_json JSONB,
    status VARCHAR(30) NOT NULL,
    prompt_version VARCHAR(40),
    schema_version VARCHAR(40),
    model_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE resource_embeddings (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    room_id UUID,
    visibility VARCHAR(30) NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(1024) NOT NULL,
    chunk_metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_resource_embeddings_resource_chunk UNIQUE (resource_id, chunk_index)
);

CREATE TABLE resource_comments (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    author_id UUID NOT NULL,
    parent_id UUID,
    body TEXT NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE resource_relations (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    related_resource_id UUID NOT NULL,
    reason TEXT,
    score NUMERIC(8, 5),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_resource_relations_pair UNIQUE (resource_id, related_resource_id)
);

CREATE TABLE ai_documents (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL UNIQUE,
    room_id UUID,
    document_type VARCHAR(40) NOT NULL,
    detected_confidence NUMERIC(5, 4),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_jobs (
    id UUID PRIMARY KEY,
    requested_by_user_id UUID NOT NULL,
    room_id UUID,
    resource_id UUID,
    job_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL,
    error_code VARCHAR(80),
    error_message TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_job_events (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_model_call_logs (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    prompt_version VARCHAR(40),
    schema_version VARCHAR(40),
    model_name VARCHAR(100),
    latency_ms BIGINT,
    input_tokens INTEGER,
    output_tokens INTEGER,
    error_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_suggestions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    room_id UUID,
    job_id UUID,
    resource_id UUID,
    suggestion_type VARCHAR(40) NOT NULL,
    payload_json JSONB NOT NULL,
    evidence_json JSONB,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE room_memory_summaries (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    from_sequence BIGINT NOT NULL,
    to_sequence BIGINT NOT NULL,
    summary_json JSONB NOT NULL,
    created_by_user_id UUID,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE daily_summaries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    summary_date DATE NOT NULL,
    summary_json JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_daily_summaries_user_date UNIQUE (user_id, summary_date)
);

CREATE TABLE wbs_items (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    parent_id UUID,
    title VARCHAR(200) NOT NULL,
    order_no INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_wbs_items_room_parent_order UNIQUE (room_id, parent_id, order_no)
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    owner_user_id UUID,
    assignee_user_id UUID,
    room_id UUID,
    wbs_item_id UUID,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    due_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE schedules (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    room_id UUID,
    task_id UUID,
    wbs_item_id UUID,
    google_event_id VARCHAR(255) UNIQUE,
    title VARCHAR(200) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    is_all_day BOOLEAN NOT NULL,
    sync_status VARCHAR(30) NOT NULL,
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE memos (
    id UUID PRIMARY KEY,
    author_user_id UUID NOT NULL,
    room_id UUID,
    body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE time_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    room_id UUID,
    timer_type VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    recovered_from_time_log_id UUID,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    last_started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    duration_seconds BIGINT NOT NULL,
    last_heartbeat_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE activity_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    room_id UUID,
    app_name VARCHAR(120),
    window_title VARCHAR(500),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    duration_seconds BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chat_rooms (
    id UUID PRIMARY KEY,
    room_id UUID,
    chat_type VARCHAR(30) NOT NULL,
    name VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chat_room_members (
    id UUID PRIMARY KEY,
    chat_room_id UUID NOT NULL,
    user_id UUID NOT NULL,
    last_read_message_id UUID,
    last_read_sequence BIGINT,
    last_read_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_chat_room_members_room_user UNIQUE (chat_room_id, user_id)
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    chat_room_id UUID NOT NULL,
    sender_user_id UUID NOT NULL,
    client_message_id VARCHAR(120) NOT NULL,
    room_sequence BIGINT NOT NULL,
    message_type VARCHAR(30) NOT NULL,
    body JSONB NOT NULL,
    resource_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_chat_messages_room_sequence UNIQUE (chat_room_id, room_sequence),
    CONSTRAINT uk_chat_messages_room_client_message UNIQUE (chat_room_id, client_message_id)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id UUID,
    title VARCHAR(200) NOT NULL,
    body TEXT,
    status VARCHAR(30) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE voice_rooms (
    id UUID PRIMARY KEY,
    room_id UUID,
    chat_room_id UUID NOT NULL,
    livekit_room_name VARCHAR(120) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE voice_participants (
    id UUID PRIMARY KEY,
    voice_room_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    left_at TIMESTAMPTZ,
    mic_status VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE storage_usage (
    id UUID PRIMARY KEY,
    user_id UUID,
    room_id UUID,
    storage_scope VARCHAR(30) NOT NULL,
    used_bytes BIGINT NOT NULL,
    limit_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_storage_usage_scope UNIQUE (user_id, room_id, storage_scope)
);

CREATE TABLE widget_context_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    selected_room_id UUID,
    mode VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE widget_bubble_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    bubble_type VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL,
    x INTEGER,
    y INTEGER,
    width INTEGER,
    height INTEGER,
    minimized BOOLEAN NOT NULL,
    opacity NUMERIC(4, 2),
    ghost_mode BOOLEAN NOT NULL,
    alert_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_widget_bubble_settings_user_type UNIQUE (user_id, bubble_type)
);

CREATE TABLE widget_item_states (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    bubble_type VARCHAR(30) NOT NULL,
    item_type VARCHAR(30) NOT NULL,
    item_id UUID NOT NULL,
    state VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_widget_item_states_user_item UNIQUE (user_id, bubble_type, item_type, item_id)
);

CREATE TABLE widget_daily_summaries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    device_id VARCHAR(120) NOT NULL,
    rollup_key VARCHAR(160) NOT NULL UNIQUE,
    summary_date DATE NOT NULL,
    bubble_setting_id UUID NOT NULL,
    open_count INTEGER NOT NULL,
    interaction_count INTEGER NOT NULL,
    visible_seconds BIGINT NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_widget_daily_summaries_rollup UNIQUE (user_id, device_id, summary_date, bubble_setting_id)
);
