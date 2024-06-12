CREATE TABLE IF NOT EXISTS authorities
(
    username  VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS group_authorities
(
    group_id  BIGINT      NOT NULL,
    authority VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS group_members
(
    id       BIGINT AUTO_INCREMENT NOT NULL,
    username VARCHAR(50)           NOT NULL,
    group_id BIGINT                NOT NULL,
    CONSTRAINT PK_GROUP_MEMBERS PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS `groups`
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    group_name VARCHAR(50)           NOT NULL,
    CONSTRAINT PK_GROUPS PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS users
(
    username VARCHAR(50)  NOT NULL,
    password VARCHAR(500) NOT NULL,
    enabled  TINYINT      NOT NULL,
    CONSTRAINT PK_USERS PRIMARY KEY (username)
);