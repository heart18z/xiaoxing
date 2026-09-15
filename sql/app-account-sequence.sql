-- Install before deploying the account suggestion endpoint. No existing user data is changed.
CREATE TABLE IF NOT EXISTS blade_app_account_sequence (
  account_day CHAR(8) NOT NULL,
  next_value BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (account_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='App registration daily account suggestion counter';
