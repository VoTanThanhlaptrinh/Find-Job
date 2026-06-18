-- Resume: thêm cột is_analyzed và raw_text
ALTER TABLE resume ADD COLUMN is_analyzed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE resume ADD COLUMN raw_text TEXT;

-- Job: thêm cột is_analyzed
ALTER TABLE job ADD COLUMN is_analyzed BOOLEAN NOT NULL DEFAULT FALSE;
