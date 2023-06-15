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

CREATE TABLE image
(
    id       BINARY(16)   NOT NULL,
    dtype    VARCHAR(31)  NULL,
    title    VARCHAR(255) NULL,
    height   INT          NOT NULL,
    width    INT          NOT NULL,
    filesize BIGINT       NOT NULL,
    uri      LONGTEXT     NULL,
    added    datetime     NULL,
    CONSTRAINT PK_IMAGE PRIMARY KEY (id)
);

CREATE TABLE image_set
(
    id       BINARY(16)   NOT NULL,
    dtype    VARCHAR(31)  NULL,
    title    VARCHAR(255) NULL,
    location VARCHAR(255) NULL,
    added    datetime     NULL,
    CONSTRAINT PK_IMAGE_SET PRIMARY KEY (id)
);

CREATE TABLE image_set_images
(
    image_set_id BINARY(16) NOT NULL,
    images_id    BINARY(16) NOT NULL,
    CONSTRAINT PK_IMAGE_SET_IMAGES PRIMARY KEY (image_set_id, images_id),
    UNIQUE (images_id)
);

CREATE TABLE image_set_tags
(
    image_set_id BINARY(16) NOT NULL,
    tags_tag_id  BINARY(16) NOT NULL,
    CONSTRAINT PK_IMAGE_SET_TAGS PRIMARY KEY (image_set_id, tags_tag_id)
);

CREATE TABLE image_tags
(
    image_id    BINARY(16) NOT NULL,
    tags_tag_id BINARY(16) NOT NULL,
    CONSTRAINT PK_IMAGE_TAGS PRIMARY KEY (image_id, tags_tag_id)
);

CREATE TABLE null_images
(
    comic_book_id BINARY(16) NOT NULL,
    images_id     BINARY(16) NOT NULL,
    CONSTRAINT PK_NULL_IMAGES PRIMARY KEY (comic_book_id, images_id),
    UNIQUE (images_id)
);

CREATE TABLE null_tags
(
    comic_book_id BINARY(16) NOT NULL,
    tags_tag_id   BINARY(16) NOT NULL,
    CONSTRAINT PK_NULL_TAGS PRIMARY KEY (comic_book_id, tags_tag_id)
);

CREATE TABLE tag
(
    tag_id BINARY(16)   NOT NULL,
    name   VARCHAR(255) NULL,
    CONSTRAINT PK_TAG PRIMARY KEY (tag_id)
);

CREATE TABLE user_preferences
(
    id       BINARY(16)   NOT NULL,
    username VARCHAR(255) NULL,
    CONSTRAINT PK_USER_PREFERENCES PRIMARY KEY (id)
);

CREATE TABLE user_preferences_favorite_comics
(
    user_preferences_id BINARY(16) NOT NULL,
    favorite_comics_id  BINARY(16) NOT NULL,
    CONSTRAINT PK_USER_PREFERENCES_FAVORITE_COMICS PRIMARY KEY (user_preferences_id, favorite_comics_id),
    UNIQUE (favorite_comics_id)
);

CREATE TABLE user_preferences_favorite_pictures
(
    user_preferences_id  BINARY(16) NOT NULL,
    favorite_pictures_id BINARY(16) NOT NULL,
    CONSTRAINT PK_USER_PREFERENCES_FAVORITE_PICTURES PRIMARY KEY (user_preferences_id, favorite_pictures_id),
    UNIQUE (favorite_pictures_id)
);

CREATE TABLE user_preferences_favorite_videos
(
    user_preferences_id BINARY(16) NOT NULL,
    favorite_videos_id  BINARY(16) NOT NULL,
    CONSTRAINT PK_USER_PREFERENCES_FAVORITE_VIDEOS PRIMARY KEY (user_preferences_id, favorite_videos_id),
    UNIQUE (favorite_videos_id)
);

CREATE TABLE users
(
    username VARCHAR(50)  NOT NULL,
    password VARCHAR(500) NOT NULL,
    enabled  TINYINT      NOT NULL,
    CONSTRAINT PK_USERS PRIMARY KEY (username)
);

CREATE TABLE video
(
    id            BINARY(16)   NOT NULL,
    metadata      LONGTEXT     NULL,
    added         datetime     NULL,
    file          VARCHAR(255) NULL,
    title         VARCHAR(255) NULL,
    thumbnails_id BINARY(16)   NULL,
    CONSTRAINT PK_VIDEO PRIMARY KEY (id)
);

CREATE TABLE video_tags
(
    video_id    BINARY(16) NOT NULL,
    tags_tag_id BINARY(16) NOT NULL,
    CONSTRAINT PK_VIDEO_TAGS PRIMARY KEY (video_id, tags_tag_id)
);

ALTER TABLE authorities
    ADD CONSTRAINT ix_auth_username UNIQUE (username, authority);

CREATE INDEX FK3jt0nel2jq07n0hexmvwugqb1 ON video (thumbnails_id);

CREATE INDEX fk_group_authorities_group ON group_authorities (group_id);

CREATE INDEX fk_group_members_group ON group_members (group_id);

CREATE INDEX fk_imasettag_on_tag ON image_set_tags (tags_tag_id);

CREATE INDEX fk_imatag_on_tag ON image_tags (tags_tag_id);

CREATE INDEX fk_null_tags_on_tag ON null_tags (tags_tag_id);

CREATE INDEX fk_vidtag_on_tag ON video_tags (tags_tag_id);

ALTER TABLE video
    ADD CONSTRAINT FK3jt0nel2jq07n0hexmvwugqb2 FOREIGN KEY (thumbnails_id) REFERENCES image_set (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE authorities
    ADD CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users (username) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE group_authorities
    ADD CONSTRAINT fk_group_authorities_group FOREIGN KEY (group_id) REFERENCES `groups` (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE group_members
    ADD CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES `groups` (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_set_images
    ADD CONSTRAINT fk_imasetima_on_image FOREIGN KEY (images_id) REFERENCES image (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_set_images
    ADD CONSTRAINT fk_imasetima_on_image_set FOREIGN KEY (image_set_id) REFERENCES image_set (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_set_tags
    ADD CONSTRAINT fk_imasettag_on_image_set FOREIGN KEY (image_set_id) REFERENCES image_set (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_set_tags
    ADD CONSTRAINT fk_imasettag_on_tag FOREIGN KEY (tags_tag_id) REFERENCES tag (tag_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_tags
    ADD CONSTRAINT fk_imatag_on_image FOREIGN KEY (image_id) REFERENCES image (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE image_tags
    ADD CONSTRAINT fk_imatag_on_tag FOREIGN KEY (tags_tag_id) REFERENCES tag (tag_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE null_images
    ADD CONSTRAINT fk_nulima_on_comic_book FOREIGN KEY (comic_book_id) REFERENCES image_set (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE null_images
    ADD CONSTRAINT fk_nulima_on_image FOREIGN KEY (images_id) REFERENCES image (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE null_tags
    ADD CONSTRAINT fk_null_tags_on_comic_book FOREIGN KEY (comic_book_id) REFERENCES image_set (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE null_tags
    ADD CONSTRAINT fk_null_tags_on_tag FOREIGN KEY (tags_tag_id) REFERENCES tag (tag_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_comics
    ADD CONSTRAINT fk_useprefavcom_on_comic_book FOREIGN KEY (favorite_comics_id) REFERENCES image_set (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_comics
    ADD CONSTRAINT fk_useprefavcom_on_user_preferences FOREIGN KEY (user_preferences_id) REFERENCES user_preferences (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_pictures
    ADD CONSTRAINT fk_useprefavpic_on_picture FOREIGN KEY (favorite_pictures_id) REFERENCES image (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_pictures
    ADD CONSTRAINT fk_useprefavpic_on_user_preferences FOREIGN KEY (user_preferences_id) REFERENCES user_preferences (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_videos
    ADD CONSTRAINT fk_useprefavvid_on_user_preferences FOREIGN KEY (user_preferences_id) REFERENCES user_preferences (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE user_preferences_favorite_videos
    ADD CONSTRAINT fk_useprefavvid_on_video FOREIGN KEY (favorite_videos_id) REFERENCES video (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_tags
    ADD CONSTRAINT fk_vidtag_on_tag FOREIGN KEY (tags_tag_id) REFERENCES tag (tag_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_tags
    ADD CONSTRAINT fk_vidtag_on_video FOREIGN KEY (video_id) REFERENCES video (id) ON UPDATE RESTRICT ON DELETE RESTRICT;