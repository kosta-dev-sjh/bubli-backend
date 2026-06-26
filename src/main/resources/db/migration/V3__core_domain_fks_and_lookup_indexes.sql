ALTER TABLE user_sessions ADD CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_preferences ADD CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_preferences ADD CONSTRAINT fk_user_preferences_default_room FOREIGN KEY (default_room_id) REFERENCES project_rooms(id);
ALTER TABLE user_notification_preferences ADD CONSTRAINT fk_user_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_privacy_consents ADD CONSTRAINT fk_user_privacy_consents_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE friend_requests ADD CONSTRAINT fk_friend_requests_requester FOREIGN KEY (requester_id) REFERENCES users(id);
ALTER TABLE friend_requests ADD CONSTRAINT fk_friend_requests_receiver FOREIGN KEY (receiver_id) REFERENCES users(id);
ALTER TABLE friendships ADD CONSTRAINT fk_friendships_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE friendships ADD CONSTRAINT fk_friendships_friend_user FOREIGN KEY (friend_user_id) REFERENCES users(id);

ALTER TABLE project_rooms ADD CONSTRAINT fk_project_rooms_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users(id);
ALTER TABLE room_members ADD CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE room_members ADD CONSTRAINT fk_room_members_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE invitations ADD CONSTRAINT fk_invitations_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE invitations ADD CONSTRAINT fk_invitations_inviter FOREIGN KEY (inviter_user_id) REFERENCES users(id);
ALTER TABLE invitations ADD CONSTRAINT fk_invitations_invitee FOREIGN KEY (invitee_user_id) REFERENCES users(id);
ALTER TABLE project_room_events ADD CONSTRAINT fk_project_room_events_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE project_room_events ADD CONSTRAINT fk_project_room_events_actor FOREIGN KEY (actor_user_id) REFERENCES users(id);

ALTER TABLE resources ADD CONSTRAINT fk_resources_owner FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE resources ADD CONSTRAINT fk_resources_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE resource_files ADD CONSTRAINT fk_resource_files_resource FOREIGN KEY (resource_id) REFERENCES resources(id);
ALTER TABLE resource_versions ADD CONSTRAINT fk_resource_versions_resource FOREIGN KEY (resource_id) REFERENCES resources(id);
ALTER TABLE resource_versions ADD CONSTRAINT fk_resource_versions_file FOREIGN KEY (file_id) REFERENCES resource_files(id);
ALTER TABLE resource_versions ADD CONSTRAINT fk_resource_versions_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE resource_summaries ADD CONSTRAINT fk_resource_summaries_resource FOREIGN KEY (resource_id) REFERENCES resources(id);
ALTER TABLE resource_summaries ADD CONSTRAINT fk_resource_summaries_job FOREIGN KEY (job_id) REFERENCES agent_jobs(id);
ALTER TABLE resource_embeddings ADD CONSTRAINT fk_resource_embeddings_resource FOREIGN KEY (resource_id) REFERENCES resources(id);
ALTER TABLE resource_embeddings ADD CONSTRAINT fk_resource_embeddings_owner FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE resource_embeddings ADD CONSTRAINT fk_resource_embeddings_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE resource_comments ADD CONSTRAINT fk_resource_comments_resource FOREIGN KEY (resource_id) REFERENCES resources(id);
ALTER TABLE resource_comments ADD CONSTRAINT fk_resource_comments_author FOREIGN KEY (author_id) REFERENCES users(id);
ALTER TABLE resource_comments ADD CONSTRAINT fk_resource_comments_parent FOREIGN KEY (parent_id) REFERENCES resource_comments(id);
ALTER TABLE resource_relations ADD CONSTRAINT fk_resource_relations_resource FOREIGN KEY (resource_id) REFERENCES resources(id);
ALTER TABLE resource_relations ADD CONSTRAINT fk_resource_relations_related FOREIGN KEY (related_resource_id) REFERENCES resources(id);

ALTER TABLE room_memory_summaries ADD CONSTRAINT fk_room_memory_summaries_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE room_memory_summaries ADD CONSTRAINT fk_room_memory_summaries_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id);
ALTER TABLE daily_summaries ADD CONSTRAINT fk_daily_summaries_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE wbs_items ADD CONSTRAINT fk_wbs_items_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE wbs_items ADD CONSTRAINT fk_wbs_items_parent FOREIGN KEY (parent_id) REFERENCES wbs_items(id);
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id);
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_assignee_user FOREIGN KEY (assignee_user_id) REFERENCES users(id);
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_wbs_item FOREIGN KEY (wbs_item_id) REFERENCES wbs_items(id);
ALTER TABLE schedules ADD CONSTRAINT fk_schedules_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id);
ALTER TABLE schedules ADD CONSTRAINT fk_schedules_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE schedules ADD CONSTRAINT fk_schedules_task FOREIGN KEY (task_id) REFERENCES tasks(id);
ALTER TABLE schedules ADD CONSTRAINT fk_schedules_wbs_item FOREIGN KEY (wbs_item_id) REFERENCES wbs_items(id);
ALTER TABLE memos ADD CONSTRAINT fk_memos_author_user FOREIGN KEY (author_user_id) REFERENCES users(id);
ALTER TABLE memos ADD CONSTRAINT fk_memos_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE time_logs ADD CONSTRAINT fk_time_logs_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE time_logs ADD CONSTRAINT fk_time_logs_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE time_logs ADD CONSTRAINT fk_time_logs_recovered_from FOREIGN KEY (recovered_from_time_log_id) REFERENCES time_logs(id);
ALTER TABLE activity_logs ADD CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE activity_logs ADD CONSTRAINT fk_activity_logs_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);

ALTER TABLE chat_rooms ADD CONSTRAINT fk_chat_rooms_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE chat_room_members ADD CONSTRAINT fk_chat_room_members_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id);
ALTER TABLE chat_room_members ADD CONSTRAINT fk_chat_room_members_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE chat_room_members ADD CONSTRAINT fk_chat_room_members_last_read_message FOREIGN KEY (last_read_message_id) REFERENCES chat_messages(id);
ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_messages_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id);
ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_messages_sender_user FOREIGN KEY (sender_user_id) REFERENCES users(id);
ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_messages_resource FOREIGN KEY (resource_id) REFERENCES resources(id);
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE voice_rooms ADD CONSTRAINT fk_voice_rooms_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE voice_rooms ADD CONSTRAINT fk_voice_rooms_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id);
ALTER TABLE voice_participants ADD CONSTRAINT fk_voice_participants_voice_room FOREIGN KEY (voice_room_id) REFERENCES voice_rooms(id);
ALTER TABLE voice_participants ADD CONSTRAINT fk_voice_participants_user FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE storage_usage ADD CONSTRAINT fk_storage_usage_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE storage_usage ADD CONSTRAINT fk_storage_usage_room FOREIGN KEY (room_id) REFERENCES project_rooms(id);
ALTER TABLE widget_context_settings ADD CONSTRAINT fk_widget_context_settings_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE widget_context_settings ADD CONSTRAINT fk_widget_context_settings_selected_room FOREIGN KEY (selected_room_id) REFERENCES project_rooms(id);
ALTER TABLE widget_bubble_settings ADD CONSTRAINT fk_widget_bubble_settings_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE widget_item_states ADD CONSTRAINT fk_widget_item_states_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE widget_daily_summaries ADD CONSTRAINT fk_widget_daily_summaries_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE widget_daily_summaries ADD CONSTRAINT fk_widget_daily_summaries_bubble_setting FOREIGN KEY (bubble_setting_id) REFERENCES widget_bubble_settings(id);

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
