-- Apply before deploying resumable chat. Existing conversations are untouched.
CREATE TABLE IF NOT EXISTS blade_app_chat_job (
  user_id BIGINT NOT NULL,
  request_id VARCHAR(80) NOT NULL,
  payload_hash CHAR(64) NOT NULL,
  job_status VARCHAR(16) NOT NULL,
  result_json LONGTEXT NULL,
  error_message VARCHAR(240) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY(user_id,request_id),
  KEY idx_chat_job_active(user_id,job_status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Idempotent background chat outcomes';
