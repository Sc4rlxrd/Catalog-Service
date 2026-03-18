CREATE TABLE books
(
    id         UUID PRIMARY KEY,
    title      VARCHAR(255)   NOT NULL,
    author     VARCHAR(255)   NOT NULL,
    isbn       VARCHAR(50)    NOT NULL UNIQUE,
    price      NUMERIC(10, 2) NOT NULL,
    stock      INT            NOT NULL,
    created_at TIMESTAMP      NOT NULL
);