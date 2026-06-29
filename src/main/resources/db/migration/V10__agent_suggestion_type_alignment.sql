ALTER TABLE agent_suggestions
    DROP CONSTRAINT IF EXISTS ck_agent_suggestions_type;

ALTER TABLE agent_suggestions
    ADD CONSTRAINT ck_agent_suggestions_type
        CHECK (suggestion_type IN (
            'REQUIREMENT',
            'TODO',
            'WBS',
            'TASK',
            'SCHEDULE',
            'QUESTION',
            'CONTRACT_FIELD',
            'CONTRACT_REVIEW',
            'REVIEW_ITEM',
            'DOCUMENT_DRAFT',
            'DAILY_SUMMARY',
            'MEMO'
        ));
