-- Supports split-event scoping without scanning all events for every displayed task.
SET @has_candidate_index = (SELECT count(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='blade_smart_event' AND column_name='source_candidate_id' AND seq_in_index=1);
SET @candidate_index_sql = IF(@has_candidate_index=0,'CREATE INDEX idx_smart_event_candidate ON blade_smart_event(source_candidate_id)','SELECT 1');
PREPARE candidate_index_stmt FROM @candidate_index_sql;
EXECUTE candidate_index_stmt;
DEALLOCATE PREPARE candidate_index_stmt;
