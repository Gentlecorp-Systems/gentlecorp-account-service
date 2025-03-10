-- Erstelle die Tabelle im TABLESPACE 'accountspace'
CREATE TABLE account (
                         id               uuid PRIMARY KEY USING INDEX TABLESPACE accountspace,
                         version          integer NOT NULL DEFAULT 1,
                         balance          DECIMAL(10, 2),
                         rate_of_interest DECIMAL(10, 2) NOT NULL,
                         category         VARCHAR(20) NOT NULL,
                         state            VARCHAR(20) NOT NULL,
                         overdraft_limit  INT,
                         transaction_limit INT,
                         created          timestamp NOT NULL,
                         updated          timestamp NOT NULL,
                         user_id          UUID NOT NULL,
    username VARCHAR(20) NOT NULL
) TABLESPACE accountspace;

-- Optional: Falls du den Index auf user_id im accountspace-Tablespace haben möchtest:
CREATE INDEX idx_account_user_id ON account (user_id) TABLESPACE accountspace;