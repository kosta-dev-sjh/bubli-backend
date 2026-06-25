ALTER TABLE agent_requests
    DROP CONSTRAINT IF EXISTS ck_agent_requests_type;

ALTER TABLE agent_requests
    ADD CONSTRAINT ck_agent_requests_type
        CHECK (request_type IN (
            'DOCUMENT_INGESTION',
            'PROJECT_INIT',
            'CONTRACT_CHECK',
            'REQUIREMENT_ANALYSIS',
            'WORK_CANDIDATE',
            'DOCUMENT_DRAFT'
        ));
