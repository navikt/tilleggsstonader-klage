ALTER TABLE vurdering
    ADD COLUMN hjemler JSONB;
UPDATE vurdering

SET hjemler = jsonb_build_array(hjemmel)
WHERE hjemmel IS NOT NULL;

ALTER TABLE vurdering
    DROP COLUMN hjemmel;