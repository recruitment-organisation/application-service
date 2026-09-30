ALTER TABLE application ADD COLUMN IF NOT EXISTS company_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_application_company ON application(company_id);
