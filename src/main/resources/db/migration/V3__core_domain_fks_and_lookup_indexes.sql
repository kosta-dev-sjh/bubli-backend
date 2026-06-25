ALTER TABLE ai_documents
    ADD CONSTRAINT fk_ai_documents_resource FOREIGN KEY (resource_id) REFERENCES resources(id),
    ADD CONSTRAINT fk_ai_documents_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);

ALTER TABLE agent_jobs
    ADD CONSTRAINT fk_agent_jobs_requested_by_user FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_agent_jobs_room FOREIGN KEY (room_id) REFERENCES project_rooms(id),
    ADD CONSTRAINT fk_agent_jobs_resource FOREIGN KEY (resource_id) REFERENCES resources(id);

ALTER TABLE agent_job_events
    ADD CONSTRAINT fk_agent_job_events_job FOREIGN KEY (job_id) REFERENCES agent_jobs(id);

ALTER TABLE agent_model_call_logs
    ADD CONSTRAINT fk_agent_model_call_logs_job FOREIGN KEY (job_id) REFERENCES agent_jobs(id);

ALTER TABLE agent_suggestions
    ADD CONSTRAINT fk_agent_suggestions_user FOREIGN KEY (user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_agent_suggestions_room FOREIGN KEY (room_id) REFERENCES project_rooms(id),
    ADD CONSTRAINT fk_agent_suggestions_job FOREIGN KEY (job_id) REFERENCES agent_jobs(id),
    ADD CONSTRAINT fk_agent_suggestions_resource FOREIGN KEY (resource_id) REFERENCES resources(id);

CREATE INDEX idx_room_members_user_status ON room_members (user_id, status);
CREATE INDEX idx_room_members_room_status ON room_members (room_id, status);
CREATE INDEX idx_invitations_room_status ON invitations (room_id, status);

CREATE INDEX idx_resources_owner_status ON resources (owner_id, status);
CREATE INDEX idx_resources_room_status ON resources (room_id, status);
CREATE INDEX idx_resource_comments_resource_created ON resource_comments (resource_id, created_at);

CREATE INDEX idx_agent_jobs_requested_status ON agent_jobs (requested_by_user_id, status);
CREATE INDEX idx_agent_jobs_room_status ON agent_jobs (room_id, status);
CREATE INDEX idx_agent_suggestions_user_status ON agent_suggestions (user_id, status);
CREATE INDEX idx_agent_suggestions_room_status ON agent_suggestions (room_id, status);
CREATE INDEX idx_ai_documents_room_status ON ai_documents (room_id, status);

CREATE INDEX idx_tasks_owner_status_due ON tasks (owner_user_id, status, due_at);
CREATE INDEX idx_tasks_room_status_due ON tasks (room_id, status, due_at);
CREATE INDEX idx_schedules_owner_starts ON schedules (owner_user_id, starts_at);
CREATE INDEX idx_schedules_room_starts ON schedules (room_id, starts_at);
CREATE INDEX idx_time_logs_user_status ON time_logs (user_id, status);
CREATE INDEX idx_time_logs_room_status ON time_logs (room_id, status);

CREATE INDEX idx_chat_room_members_user_status ON chat_room_members (user_id, status);
CREATE INDEX idx_chat_messages_chat_room_created ON chat_messages (chat_room_id, created_at);
