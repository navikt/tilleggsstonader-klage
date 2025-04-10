ALTER TABLE behandlingshistorikk
    ADD COLUMN utfall TEXT,
    ADD COLUMN metadata JSONB,
    ADD COLUMN opprettet_av_navn TEXT;

ALTER TABLE behandlingshistorikk
    ADD COLUMN git_versjon TEXT;

