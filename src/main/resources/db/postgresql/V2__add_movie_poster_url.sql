ALTER TABLE movies
    ADD COLUMN poster_url VARCHAR(2048),
    ADD CONSTRAINT movies_poster_url_check
        CHECK (poster_url IS NULL OR poster_url ~ '^https?://\S+$');
