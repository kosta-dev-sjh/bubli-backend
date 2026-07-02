-- Bubli local development seed.
-- Target: Docker Postgres database created by this backend repo.
-- Purpose: shared project-room data for frontend/API integration checks.
-- Safe to rerun: it deletes only the deterministic demo IDs below, then inserts them again.

BEGIN;

SET LOCAL TIME ZONE 'Asia/Seoul';

-- Demo IDs
-- user: 00000000-0000-4000-8000-000000000132 / bubli_id maren_local
-- user: 00000000-0000-4000-8000-000000000133 / bubli_id minji_local
-- room: 22222222-2222-4222-8222-222222222222 / 브랜드 상세페이지

CREATE TEMP TABLE seed_room_ids (id uuid PRIMARY KEY) ON COMMIT DROP;

INSERT INTO seed_room_ids (id)
VALUES
    ('22222222-2222-4222-8222-222222222222'),
    ('00000000-0000-4000-8000-000000000201')
ON CONFLICT DO NOTHING;

INSERT INTO seed_room_ids (id)
SELECT id
FROM project_rooms
WHERE created_by_user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
)
ON CONFLICT DO NOTHING;

DELETE FROM generated_documents
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
)
OR suggestion_id IN (
    SELECT id
    FROM agent_suggestions
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR user_id IN (
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133'
    )
);

DELETE FROM widget_daily_summaries
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM widget_item_states
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM widget_bubble_settings
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM widget_context_settings
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
)
OR selected_room_id IN (
    SELECT id FROM seed_room_ids
);

DELETE FROM notifications
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM agent_suggestions
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM resource_summaries
WHERE job_id IN (
    SELECT id
    FROM agent_jobs
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR requested_by_user_id IN (
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133'
    )
)
OR resource_id IN (
    SELECT id
    FROM resources
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR owner_id IN (
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133'
    )
);

DELETE FROM agent_dispatch_outbox
WHERE job_id IN (
    SELECT id
    FROM agent_jobs
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR requested_by_user_id IN (
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133'
    )
);

DELETE FROM agent_model_call_logs
WHERE job_id IN (
    SELECT id
    FROM agent_jobs
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR requested_by_user_id IN (
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133'
    )
);

DELETE FROM agent_job_events
WHERE job_id IN (
    SELECT id
    FROM agent_jobs
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR requested_by_user_id IN (
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133'
    )
);

DELETE FROM agent_jobs
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR requested_by_user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM room_memory_summaries
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR created_by_user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

UPDATE chat_room_members
SET last_read_message_id = NULL,
    last_read_at = NULL,
    updated_at = now()
WHERE chat_room_id IN (
    SELECT id
    FROM chat_rooms
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR id IN (
        '00000000-0000-4000-8000-000000000801',
        '00000000-0000-4000-8000-000000000802'
    )
);

DELETE FROM chat_messages
WHERE chat_room_id IN (
    SELECT id
    FROM chat_rooms
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR id IN (
        '00000000-0000-4000-8000-000000000801',
        '00000000-0000-4000-8000-000000000802'
    )
);

DELETE FROM voice_participants
WHERE voice_room_id IN (
    SELECT id
    FROM voice_rooms
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR chat_room_id IN (
        SELECT id
        FROM chat_rooms
        WHERE room_id IN (SELECT id FROM seed_room_ids)
        OR id IN (
            '00000000-0000-4000-8000-000000000801',
            '00000000-0000-4000-8000-000000000802'
        )
    )
);

DELETE FROM voice_rooms
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR chat_room_id IN (
    SELECT id
    FROM chat_rooms
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR id IN (
        '00000000-0000-4000-8000-000000000801',
        '00000000-0000-4000-8000-000000000802'
    )
);

DELETE FROM chat_room_members
WHERE chat_room_id IN (
    SELECT id
    FROM chat_rooms
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR id IN (
        '00000000-0000-4000-8000-000000000801',
        '00000000-0000-4000-8000-000000000802'
    )
);

DELETE FROM chat_rooms
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR id IN (
    '00000000-0000-4000-8000-000000000801',
    '00000000-0000-4000-8000-000000000802'
);

DELETE FROM activity_logs
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM time_logs
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM memos
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR author_user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM schedules
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR owner_user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM tasks
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR owner_user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
)
OR assignee_user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM wbs_items
WHERE room_id IN (SELECT id FROM seed_room_ids);

DELETE FROM resource_files
WHERE resource_id IN (
    SELECT id
    FROM resources
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR owner_id IN (
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133'
    )
);

DELETE FROM resource_versions
WHERE resource_id IN (
    SELECT id
    FROM resources
    WHERE room_id IN (SELECT id FROM seed_room_ids)
    OR owner_id IN (
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133'
    )
);

DELETE FROM resources
WHERE room_id IN (SELECT id FROM seed_room_ids)
OR owner_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM room_members
WHERE room_id IN (SELECT id FROM seed_room_ids);

DELETE FROM project_room_events
WHERE room_id IN (SELECT id FROM seed_room_ids);

DELETE FROM invitations
WHERE room_id IN (SELECT id FROM seed_room_ids);

UPDATE user_preferences
SET default_project_room_id = NULL,
    updated_at = now()
WHERE default_project_room_id IN (SELECT id FROM seed_room_ids);

DELETE FROM project_rooms
WHERE id IN (SELECT id FROM seed_room_ids);

DELETE FROM user_sessions
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM user_notification_preferences
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM user_privacy_consents
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM user_preferences
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM daily_summaries
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM friendships
WHERE user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
)
OR friend_user_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM friend_requests
WHERE requester_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
)
OR receiver_id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

DELETE FROM users
WHERE id IN (
    '00000000-0000-4000-8000-000000000132',
    '00000000-0000-4000-8000-000000000133'
);

INSERT INTO users (id, google_sub, bubli_id, name, avatar_url, locale, timezone, status, deleted_at, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000132',
        'local-widget-qa-132',
        'maren_local',
        'Maren',
        NULL,
        'ko',
        'Asia/Seoul',
        'ACTIVE',
        NULL,
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000133',
        'local-widget-friend-133',
        'minji_local',
        '민지',
        NULL,
        'ko',
        'Asia/Seoul',
        'ACTIVE',
        NULL,
        now() - interval '2 days',
        now()
    );

INSERT INTO user_notification_preferences (user_id, notification_type, enabled)
VALUES
    ('00000000-0000-4000-8000-000000000132', 'MESSAGE', true),
    ('00000000-0000-4000-8000-000000000132', 'COMMENT', true),
    ('00000000-0000-4000-8000-000000000132', 'RESOURCE', true),
    ('00000000-0000-4000-8000-000000000132', 'AGENT', true),
    ('00000000-0000-4000-8000-000000000132', 'STORAGE', true);

INSERT INTO user_privacy_consents (user_id, consent_type, enabled, updated_at)
VALUES
    ('00000000-0000-4000-8000-000000000132', 'ACTIVITY_CONTEXT', true, now()),
    ('00000000-0000-4000-8000-000000000132', 'MANAGED_FOLDER', true, now());

INSERT INTO friendships (id, user_id, friend_user_id, accepted_at, created_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000151',
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133',
        now() - interval '1 day',
        now() - interval '1 day'
    ),
    (
        '00000000-0000-4000-8000-000000000152',
        '00000000-0000-4000-8000-000000000133',
        '00000000-0000-4000-8000-000000000132',
        now() - interval '1 day',
        now() - interval '1 day'
    );

INSERT INTO project_rooms (
    id,
    created_by_user_id,
    name,
    client_name,
    contract_amount,
    payment_status,
    payment_due_date,
    paid_at,
    status,
    closed_at,
    created_at,
    updated_at
)
VALUES
    (
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000132',
        '브랜드 상세페이지',
        'Bubli 데모',
        NULL,
        'PENDING',
        NULL,
        NULL,
        'ACTIVE',
        NULL,
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000201',
        '00000000-0000-4000-8000-000000000132',
        '개인 작업 정리',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL,
        'ACTIVE',
        NULL,
        now() - interval '1 day',
        now()
    );

INSERT INTO user_preferences (id, user_id, theme, default_home_type, default_project_room_id, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000141',
        '00000000-0000-4000-8000-000000000132',
        'system',
        'DASHBOARD',
        '22222222-2222-4222-8222-222222222222',
        now(),
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000142',
        '00000000-0000-4000-8000-000000000133',
        'system',
        'DASHBOARD',
        '22222222-2222-4222-8222-222222222222',
        now(),
        now()
    );

INSERT INTO room_members (id, room_id, user_id, role, status, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000211',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000132',
        'PROJECT_LEADER',
        'ACTIVE',
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000212',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000133',
        'MEMBER',
        'ACTIVE',
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000213',
        '00000000-0000-4000-8000-000000000201',
        '00000000-0000-4000-8000-000000000132',
        'PROJECT_LEADER',
        'ACTIVE',
        now() - interval '1 day',
        now()
    );

INSERT INTO resources (id, owner_id, room_id, title, kind, visibility, status, deleted_at, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000301',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '요구사항 정리.pdf',
        'FILE',
        'ROOM_SHARED',
        'ANALYZED',
        NULL,
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000302',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '회의록 06-28.md',
        'FILE',
        'ROOM_SHARED',
        'READY',
        NULL,
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000303',
        '00000000-0000-4000-8000-000000000132',
        NULL,
        '개인 메모 정리.md',
        'FILE',
        'PERSONAL',
        'READY',
        NULL,
        now() - interval '3 hours',
        now()
    );

INSERT INTO resource_files (
    id,
    resource_id,
    storage_key,
    original_name,
    mime_type,
    size_bytes,
    checksum,
    created_at,
    updated_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000000311',
        '00000000-0000-4000-8000-000000000301',
        'local-seed/brand-detail/requirements.pdf',
        '요구사항 정리.pdf',
        'application/pdf',
        184320,
        'local-seed-requirements',
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000312',
        '00000000-0000-4000-8000-000000000302',
        'local-seed/brand-detail/minutes-06-28.md',
        '회의록 06-28.md',
        'text/markdown',
        41984,
        'local-seed-minutes',
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000313',
        '00000000-0000-4000-8000-000000000303',
        'local-seed/personal/private-note.md',
        '개인 메모 정리.md',
        'text/markdown',
        12045,
        'local-seed-personal',
        now() - interval '3 hours',
        now()
    );

INSERT INTO wbs_items (id, room_id, parent_id, title, order_no, status, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000321',
        '22222222-2222-4222-8222-222222222222',
        NULL,
        '자료 기준 정리',
        1,
        'IN_PROGRESS',
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000322',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000321',
        '업무 범위 확인',
        1,
        'IN_PROGRESS',
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000323',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000321',
        '요구사항 정리',
        2,
        'TODO',
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000324',
        '22222222-2222-4222-8222-222222222222',
        NULL,
        '1차 시안 정리',
        2,
        'IN_PROGRESS',
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000325',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000324',
        '메인 배너 구조',
        1,
        'IN_PROGRESS',
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000326',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000324',
        '반응형 점검',
        2,
        'TODO',
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000327',
        '22222222-2222-4222-8222-222222222222',
        NULL,
        '검수 요청',
        3,
        'TODO',
        now() - interval '20 hours',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000328',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000327',
        '확인 질문 정리',
        1,
        'TODO',
        now() - interval '20 hours',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000329',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000327',
        '제출 파일 정리',
        2,
        'TODO',
        now() - interval '20 hours',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000330',
        '22222222-2222-4222-8222-222222222222',
        NULL,
        '최종 공유',
        4,
        'TODO',
        now() - interval '12 hours',
        now()
    );

INSERT INTO tasks (
    id,
    owner_user_id,
    assignee_user_id,
    room_id,
    wbs_item_id,
    title,
    description,
    status,
    due_at,
    created_at,
    updated_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000000401',
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000322',
        '업무 범위 확인',
        '자료 기준 정리에서 빠진 범위를 확인합니다.',
        'IN_PROGRESS',
        now() + interval '1 day',
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000402',
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000323',
        '요구사항 문장 정리',
        '회의록 표현을 화면 요구사항 문장으로 다듬습니다.',
        'TODO',
        now() + interval '2 days',
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000403',
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000325',
        '메인 배너 1차 시안',
        '브랜드 첫 화면의 카피와 배치 기준을 정리합니다.',
        'REVIEW',
        now() + interval '3 days',
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000404',
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000326',
        '모바일 간격 확인',
        '작은 화면에서 제목과 카드가 겹치지 않는지 확인합니다.',
        'BLOCKED',
        now() + interval '4 days',
        now() - interval '18 hours',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000405',
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000328',
        '확인 질문 남기기',
        '확정 전 필요한 질문을 프로젝트룸 대화에 남깁니다.',
        'TODO',
        now() + interval '5 days',
        now() - interval '12 hours',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000406',
        '00000000-0000-4000-8000-000000000132',
        '00000000-0000-4000-8000-000000000133',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000329',
        '제출 파일 묶기',
        '공유 자료와 확인된 산출물을 한 번에 검토할 수 있게 묶습니다.',
        'TODO',
        now() + interval '6 days',
        now() - interval '8 hours',
        now()
    );

INSERT INTO schedules (
    id,
    owner_user_id,
    room_id,
    task_id,
    wbs_item_id,
    google_event_id,
    title,
    starts_at,
    ends_at,
    is_all_day,
    sync_status,
    last_synced_at,
    created_at,
    updated_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000000501',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000401',
        '00000000-0000-4000-8000-000000000322',
        NULL,
        '업무 범위 검토',
        date_trunc('day', now()) + interval '15 hours',
        date_trunc('day', now()) + interval '16 hours',
        false,
        'PENDING',
        NULL,
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000502',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000403',
        '00000000-0000-4000-8000-000000000325',
        NULL,
        '시안 리뷰 미팅',
        date_trunc('day', now()) + interval '2 days 18 hours',
        date_trunc('day', now()) + interval '2 days 19 hours',
        false,
        'PENDING',
        NULL,
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000503',
        '00000000-0000-4000-8000-000000000132',
        NULL,
        NULL,
        NULL,
        NULL,
        '개인 자료 정리',
        date_trunc('day', now()) + interval '1 day 10 hours',
        date_trunc('day', now()) + interval '1 day 11 hours',
        false,
        'PENDING',
        NULL,
        now() - interval '5 hours',
        now()
    );

INSERT INTO memos (id, author_user_id, room_id, body, status, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000601',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '자료 기준 정리 후 WBS 행 단위로 먼저 확인한다.',
        'ACTIVE',
        now() - interval '4 hours',
        now()
    );

INSERT INTO time_logs (
    id,
    user_id,
    room_id,
    timer_type,
    idempotency_key,
    recovered_from_time_log_id,
    status,
    started_at,
    last_started_at,
    ended_at,
    duration_seconds,
    last_heartbeat_at,
    created_at,
    updated_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000000701',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        'WORK',
        'local-seed-work-timer-001',
        NULL,
        'STOPPED',
        now() - interval '2 hours',
        now() - interval '2 hours',
        now() - interval '1 hour',
        3600,
        now() - interval '1 hour',
        now() - interval '2 hours',
        now()
    );

INSERT INTO activity_logs (id, user_id, room_id, app_name, window_title, started_at, ended_at, duration_seconds, created_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000711',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        'Figma',
        '브랜드 상세페이지 시안',
        now() - interval '3 hours',
        now() - interval '2 hours 30 minutes',
        1800,
        now() - interval '2 hours 30 minutes'
    );

INSERT INTO chat_rooms (id, room_id, chat_type, name, status, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000801',
        '22222222-2222-4222-8222-222222222222',
        'PROJECT_ROOM',
        '브랜드 상세페이지 대화',
        'ACTIVE',
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000802',
        NULL,
        'DIRECT',
        'Maren, 민지',
        'ACTIVE',
        now() - interval '1 day',
        now()
    );

INSERT INTO chat_room_members (id, chat_room_id, user_id, last_read_message_id, last_read_at, status, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000811',
        '00000000-0000-4000-8000-000000000801',
        '00000000-0000-4000-8000-000000000132',
        NULL,
        now() - interval '1 hour',
        'ACTIVE',
        now() - interval '2 days',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000812',
        '00000000-0000-4000-8000-000000000801',
        '00000000-0000-4000-8000-000000000133',
        NULL,
        now() - interval '2 hours',
        'ACTIVE',
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000813',
        '00000000-0000-4000-8000-000000000802',
        '00000000-0000-4000-8000-000000000132',
        NULL,
        now() - interval '1 hour',
        'ACTIVE',
        now() - interval '1 day',
        now()
    ),
    (
        '00000000-0000-4000-8000-000000000814',
        '00000000-0000-4000-8000-000000000802',
        '00000000-0000-4000-8000-000000000133',
        NULL,
        now() - interval '2 hours',
        'ACTIVE',
        now() - interval '1 day',
        now()
    );

INSERT INTO chat_messages (
    id,
    chat_room_id,
    sender_user_id,
    client_message_id,
    room_sequence,
    message_type,
    body,
    resource_id,
    created_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000000821',
        '00000000-0000-4000-8000-000000000801',
        '00000000-0000-4000-8000-000000000132',
        'local-seed-room-msg-001',
        1,
        'TEXT',
        '{"text":"요구사항 정리 자료는 자료보드에 올려뒀습니다."}'::jsonb,
        '00000000-0000-4000-8000-000000000301',
        now() - interval '5 hours'
    ),
    (
        '00000000-0000-4000-8000-000000000822',
        '00000000-0000-4000-8000-000000000801',
        '00000000-0000-4000-8000-000000000133',
        'local-seed-room-msg-002',
        2,
        'TEXT',
        '{"text":"메인 배너 구조는 리뷰 상태로 옮겨둘게요."}'::jsonb,
        NULL,
        now() - interval '4 hours'
    ),
    (
        '00000000-0000-4000-8000-000000000823',
        '00000000-0000-4000-8000-000000000802',
        '00000000-0000-4000-8000-000000000133',
        'local-seed-direct-msg-001',
        1,
        'TEXT',
        '{"text":"오늘 검수 질문 먼저 정리해둘게요."}'::jsonb,
        NULL,
        now() - interval '2 hours'
    );

INSERT INTO voice_rooms (id, room_id, chat_room_id, livekit_room_name, status, created_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000901',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000801',
        'local-seed-brand-detail-voice',
        'OPEN',
        now() - interval '1 hour'
    );

INSERT INTO voice_participants (id, voice_room_id, user_id, status, joined_at, left_at, mic_status, created_at)
VALUES
    (
        '00000000-0000-4000-8000-000000000911',
        '00000000-0000-4000-8000-000000000901',
        '00000000-0000-4000-8000-000000000132',
        'JOINED',
        now() - interval '55 minutes',
        NULL,
        'UNMUTED',
        now() - interval '55 minutes'
    ),
    (
        '00000000-0000-4000-8000-000000000912',
        '00000000-0000-4000-8000-000000000901',
        '00000000-0000-4000-8000-000000000133',
        'JOINED',
        now() - interval '50 minutes',
        NULL,
        'MUTED',
        now() - interval '50 minutes'
    );

INSERT INTO agent_suggestions (
    id,
    user_id,
    room_id,
    job_id,
    resource_id,
    suggestion_type,
    payload_json,
    evidence_json,
    status,
    created_at,
    updated_at,
    reviewed_by,
    reviewed_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000000901',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        NULL,
        '00000000-0000-4000-8000-000000000301',
        'TASK',
        '{"title":"검수 질문 남기기","description":"요구사항에서 모호한 질문을 대화에 남깁니다.","priority":"NORMAL"}'::jsonb,
        '{"source":"요구사항 정리.pdf","quote":"검수 전 확인 필요"}'::jsonb,
        'DRAFT',
        now() - interval '3 hours',
        now(),
        NULL,
        NULL
    ),
    (
        '00000000-0000-4000-8000-000000000902',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        NULL,
        '00000000-0000-4000-8000-000000000301',
        'WBS',
        '{"title":"최종 공유","description":"검수 후 제출 파일과 확인 메모를 묶어 공유합니다."}'::jsonb,
        '{"source":"회의록 06-28.md"}'::jsonb,
        'DRAFT',
        now() - interval '2 hours',
        now(),
        NULL,
        NULL
    ),
    (
        '00000000-0000-4000-8000-000000000903',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        NULL,
        '00000000-0000-4000-8000-000000000302',
        'DOCUMENT_DRAFT',
        '{"title":"시안 검수 메모","documentType":"REVIEW_NOTE","summary":"검수 질문과 제출 파일 기준을 한 문서로 정리합니다."}'::jsonb,
        '{"source":"회의록 06-28.md"}'::jsonb,
        'DRAFT',
        now() - interval '1 hour',
        now(),
        NULL,
        NULL
    );

INSERT INTO generated_documents (
    id,
    user_id,
    room_id,
    suggestion_id,
    resource_id,
    title,
    document_type,
    content_markdown,
    metadata_json,
    created_at,
    updated_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000001001',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        '00000000-0000-4000-8000-000000000903',
        '00000000-0000-4000-8000-000000000302',
        '시안 검수 메모',
        'REVIEW_NOTE',
        '# 시안 검수 메모\n\n- 업무 범위 확인\n- 모바일 간격 확인\n- 제출 파일 정리',
        '{"seed":"local-workboard"}'::jsonb,
        now() - interval '30 minutes',
        now()
    );

INSERT INTO notifications (id, user_id, source_type, source_id, title, body, status, read_at, created_at)
VALUES
    (
        '00000000-0000-4000-8000-000000001101',
        '00000000-0000-4000-8000-000000000132',
        'AGENT',
        '00000000-0000-4000-8000-000000000901',
        '확인할 후보가 생겼습니다',
        '자료 기준 정리에서 작업 후보 2개가 나왔습니다.',
        'UNREAD',
        NULL,
        now() - interval '1 hour'
    );

INSERT INTO widget_context_settings (id, user_id, selected_room_id, mode, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000001201',
        '00000000-0000-4000-8000-000000000132',
        '22222222-2222-4222-8222-222222222222',
        'ROOM',
        now(),
        now()
    );

INSERT INTO widget_bubble_settings (
    id,
    user_id,
    bubble_type,
    enabled,
    x,
    y,
    width,
    height,
    minimized,
    opacity,
    ghost_mode,
    alert_enabled,
    created_at,
    updated_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000001211',
        '00000000-0000-4000-8000-000000000132',
        'TODO',
        true,
        80,
        120,
        320,
        240,
        false,
        0.92,
        false,
        true,
        now(),
        now()
    ),
    (
        '00000000-0000-4000-8000-000000001212',
        '00000000-0000-4000-8000-000000000132',
        'SCHEDULE',
        true,
        430,
        120,
        320,
        220,
        false,
        0.86,
        false,
        true,
        now(),
        now()
    ),
    (
        '00000000-0000-4000-8000-000000001213',
        '00000000-0000-4000-8000-000000000132',
        'TIMER',
        true,
        80,
        390,
        260,
        180,
        true,
        0.9,
        false,
        false,
        now(),
        now()
    );

INSERT INTO widget_item_states (id, user_id, bubble_type, item_type, item_id, state, created_at, updated_at)
VALUES
    (
        '00000000-0000-4000-8000-000000001221',
        '00000000-0000-4000-8000-000000000132',
        'TODO',
        'TASK',
        '00000000-0000-4000-8000-000000000401',
        'PINNED',
        now(),
        now()
    );

INSERT INTO widget_daily_summaries (
    id,
    user_id,
    device_id,
    rollup_key,
    summary_date,
    bubble_setting_id,
    open_count,
    interaction_count,
    visible_seconds,
    synced_at,
    created_at,
    updated_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000001231',
        '00000000-0000-4000-8000-000000000132',
        'local-dev-mac',
        'local-dev-mac:2026-07-01:todo',
        current_date,
        '00000000-0000-4000-8000-000000001211',
        5,
        12,
        1840,
        now(),
        now(),
        now()
    );

COMMIT;
