CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE movies (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    release_year INTEGER NOT NULL,
    external_id VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE personal_ratings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT personal_ratings_user_movie_uk UNIQUE (user_id, movie_id)
);

CREATE TABLE collection_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE genres (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE movie_genres (
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    genre_id UUID NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (movie_id, genre_id)
);

CREATE TABLE people (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE movie_credits (
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES people(id) ON DELETE CASCADE,
    role VARCHAR(100) NOT NULL,
    PRIMARY KEY (movie_id, person_id, role)
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(255) NOT NULL
);

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key VARCHAR(255) NOT NULL UNIQUE,
    result_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL
);
