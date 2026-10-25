-- MySQL migration for allowing a new application after a previous decision,
-- while ensuring that only one pending application per normalized email exists.
-- Run the duplicate check first. Resolve any returned rows by reviewing them;
-- do not delete application history.
SELECT LOWER(email) AS normalized_email, COUNT(*) AS pending_count
FROM demandes_adhesion
WHERE statut = 'EN_ATTENTE'
GROUP BY LOWER(email)
HAVING COUNT(*) > 1;

-- Remove a legacy single-column UNIQUE(email) index, if present.
SET @email_unique_index = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'demandes_adhesion'
      AND s.COLUMN_NAME = 'email'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.INDEX_NAME IN (
          SELECT INDEX_NAME
          FROM INFORMATION_SCHEMA.STATISTICS
          WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = 'demandes_adhesion'
          GROUP BY INDEX_NAME
          HAVING COUNT(*) = 1
      )
    LIMIT 1
);

SET @drop_email_unique_sql = IF(
    @email_unique_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE demandes_adhesion DROP INDEX `', @email_unique_index, '`')
);
PREPARE drop_email_unique_statement FROM @drop_email_unique_sql;
EXECUTE drop_email_unique_statement;
DEALLOCATE PREPARE drop_email_unique_statement;

-- The generated NULL for non-pending rows allows historical emails to repeat.
ALTER TABLE demandes_adhesion
    ADD COLUMN email_en_attente_normalise VARCHAR(150)
        GENERATED ALWAYS AS (
            CASE WHEN statut = 'EN_ATTENTE' THEN LOWER(email) ELSE NULL END
        ) STORED,
    ADD UNIQUE INDEX uq_adhesion_email_pending (email_en_attente_normalise);
