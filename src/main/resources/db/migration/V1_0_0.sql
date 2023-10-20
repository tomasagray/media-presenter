CREATE TABLE authorities
(
    username  VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL
);

CREATE TABLE group_authorities
(
    group_id  BIGINT      NOT NULL,
    authority VARCHAR(50) NOT NULL
);

CREATE TABLE group_members
(
    id       BIGINT AUTO_INCREMENT NOT NULL,
    username VARCHAR(50)           NOT NULL,
    group_id BIGINT                NOT NULL,
    CONSTRAINT PK_GROUP_MEMBERS PRIMARY KEY (id)
);

CREATE TABLE `groups`
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    group_name VARCHAR(50)           NOT NULL,
    CONSTRAINT PK_GROUPS PRIMARY KEY (id)
);

CREATE TABLE users
(
    username VARCHAR(50)  NOT NULL,
    password VARCHAR(500) NOT NULL,
    enabled  TINYINT(3)   NOT NULL,
    CONSTRAINT PK_USERS PRIMARY KEY (username)
);

CREATE TABLE image
(
    dtype    VARCHAR(31)  NOT NULL,
    id       VARCHAR(36)  NOT NULL,
    added    datetime     NULL,
    filesize BIGINT       NOT NULL,
    height   INT          NOT NULL,
    title    VARCHAR(255) NULL,
    uri      LONGTEXT     NULL,
    width    INT          NOT NULL,
    CONSTRAINT PK_IMAGE PRIMARY KEY (id)
);

CREATE TABLE image_set
(
    dtype    VARCHAR(31)  NOT NULL,
    id       VARCHAR(36)  NOT NULL,
    added    datetime     NULL,
    title    VARCHAR(255) NULL,
    location VARCHAR(255) NULL,
    CONSTRAINT PK_IMAGE_SET PRIMARY KEY (id)
);

CREATE TABLE image_set_images
(
    image_set_id VARCHAR(36) NOT NULL,
    images_id    VARCHAR(36) NOT NULL,
    CONSTRAINT PK_IMAGE_SET_IMAGES PRIMARY KEY (image_set_id, images_id),
    UNIQUE (images_id)
);

CREATE TABLE image_set_tags
(
    image_set_id VARCHAR(36)  NOT NULL,
    tags_hash_id VARCHAR(255) NOT NULL,
    CONSTRAINT PK_IMAGE_SET_TAGS PRIMARY KEY (image_set_id, tags_hash_id)
);

CREATE TABLE image_tags
(
    image_id     VARCHAR(36)  NOT NULL,
    tags_hash_id VARCHAR(255) NOT NULL,
    CONSTRAINT PK_IMAGE_TAGS PRIMARY KEY (image_id, tags_hash_id)
);

CREATE TABLE tag
(
    hash_id VARCHAR(255) NOT NULL,
    name    VARCHAR(255) NULL,
    CONSTRAINT PK_TAG PRIMARY KEY (hash_id)
);

CREATE TABLE user_preferences
(
    id       VARCHAR(36)  NOT NULL,
    username VARCHAR(255) NULL,
    CONSTRAINT PK_USER_PREFERENCES PRIMARY KEY (id)
);

CREATE TABLE user_preferences_favorite_comics
(
    user_preferences_id VARCHAR(36) NOT NULL,
    favorite_comics_id  VARCHAR(36) NOT NULL,
    CONSTRAINT PK_USER_PREFERENCES_FAVORITE_COMICS
        PRIMARY KEY (user_preferences_id, favorite_comics_id),
    UNIQUE (favorite_comics_id)
);

CREATE TABLE user_preferences_favorite_pictures
(
    user_preferences_id  VARCHAR(36) NOT NULL,
    favorite_pictures_id VARCHAR(36) NOT NULL,
    CONSTRAINT PK_USER_PREFERENCES_FAVORITE_PICTURES
        PRIMARY KEY (user_preferences_id, favorite_pictures_id),
    UNIQUE (favorite_pictures_id)
);

CREATE TABLE user_preferences_favorite_videos
(
    user_preferences_id VARCHAR(36) NOT NULL,
    favorite_videos_id  VARCHAR(36) NOT NULL,
    CONSTRAINT PK_USER_PREFERENCES_FAVORITE_VIDEOS
        PRIMARY KEY (user_preferences_id, favorite_videos_id),
    UNIQUE (favorite_videos_id)
);

CREATE TABLE video
(
    id            VARCHAR(36)  NOT NULL,
    added         datetime     NULL,
    file          VARCHAR(255) NULL,
    metadata      LONGTEXT     NULL,
    title         VARCHAR(255) NULL,
    thumbnails_id VARCHAR(36)  NULL,
    CONSTRAINT PK_VIDEO PRIMARY KEY (id)
);

CREATE TABLE video_tags
(
    video_id     VARCHAR(36)  NOT NULL,
    tags_hash_id VARCHAR(255) NOT NULL,
    CONSTRAINT PK_VIDEO_TAGS PRIMARY KEY (video_id, tags_hash_id)
);

CREATE INDEX FK3jt0nel2jq07n0hexmvwugqb1 ON video (thumbnails_id);

CREATE INDEX FK447h0m60g8mn7o6kabl9pgpfh ON video_tags (tags_hash_id);

CREATE INDEX FKdi5ka2vft62qv8el6hceobh34 ON image_tags (tags_hash_id);

CREATE INDEX FKpbtdvry9pex9a05m2wr8h131f ON image_set_tags (tags_hash_id);

ALTER TABLE user_preferences_favorite_comics
    ADD CONSTRAINT FK1pe8n27gcydtll63ip1xwvrwy
        FOREIGN KEY (favorite_comics_id) REFERENCES image_set (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_set_images
    ADD CONSTRAINT FK21agfnifm2dnkfm2p1764xgu6
        FOREIGN KEY (image_set_id) REFERENCES image_set (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video
    ADD CONSTRAINT FK3jt0nel2jq07n0hexmvwugqb1
        FOREIGN KEY (thumbnails_id) REFERENCES image_set (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_videos
    ADD CONSTRAINT FK3lxe1ohul3mlrhp49pt59ieuh
        FOREIGN KEY (favorite_videos_id) REFERENCES video (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_tags
    ADD CONSTRAINT FK447h0m60g8mn7o6kabl9pgpfh
        FOREIGN KEY (tags_hash_id) REFERENCES tag (hash_id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_set_images
    ADD CONSTRAINT FK54x50jkwapp63fgko3ibab1u1
        FOREIGN KEY (images_id) REFERENCES image (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_pictures
    ADD CONSTRAINT FK6678egysdww95cdu38n91me31
        FOREIGN KEY (favorite_pictures_id) REFERENCES image (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_set_tags
    ADD CONSTRAINT FK9j3y8xw9c6dawso9m0cnmdx2h
        FOREIGN KEY (image_set_id) REFERENCES image_set (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_tags
    ADD CONSTRAINT FKdi5ka2vft62qv8el6hceobh34
        FOREIGN KEY (tags_hash_id) REFERENCES tag (hash_id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_tags
    ADD CONSTRAINT FKga9qdchq195tgievhkrtfbu4i
        FOREIGN KEY (image_id) REFERENCES image (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_comics
    ADD CONSTRAINT FKm85bc945s23u9fhev9bidgjcw
        FOREIGN KEY (user_preferences_id) REFERENCES user_preferences (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_videos
    ADD CONSTRAINT FKmkb9untxtpq6j56npfubm19qv
        FOREIGN KEY (user_preferences_id) REFERENCES user_preferences (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_set_tags
    ADD CONSTRAINT FKpbtdvry9pex9a05m2wr8h131f
        FOREIGN KEY (tags_hash_id) REFERENCES tag (hash_id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_tags
    ADD CONSTRAINT FKry3serark801w5vq5mmp2nny5
        FOREIGN KEY (video_id) REFERENCES video (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_pictures
    ADD CONSTRAINT FKtqrdrha6nknv0dya6p91nbhq9
        FOREIGN KEY (user_preferences_id) REFERENCES user_preferences (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;