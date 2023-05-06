create table users
(
    username VARCHAR(50)  not null primary key,
    password VARCHAR(500) not null,
    enabled  boolean      not null
);

create table authorities
(
    username  VARCHAR(50) not null,
    authority VARCHAR(50) not null,
    constraint fk_authorities_users foreign key (username) references users (username)
);
create unique index ix_auth_username on authorities (username, authority);

create table `groups`
(
    id         bigint      NOT NULL AUTO_INCREMENT primary key,
    group_name VARCHAR(50) not null
);

create table group_authorities
(
    group_id  bigint      not null,
    authority varchar(50) not null,
    constraint fk_group_authorities_group foreign key (group_id) references `groups` (id)
);

create table group_members
(
    id       bigint      NOT NULL AUTO_INCREMENT primary key,
    username varchar(50) not null,
    group_id bigint      not null,
    constraint fk_group_members_group foreign key (group_id) references `groups` (id)
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
    CONSTRAINT pk_image PRIMARY KEY (id)
);

CREATE TABLE image_set
(
    id       BINARY(16)   NOT NULL,
    dtype    VARCHAR(31)  NULL,
    title    VARCHAR(255) NULL,
    location VARCHAR(255) NULL,
    CONSTRAINT pk_imageset PRIMARY KEY (id)
);

CREATE TABLE image_set_images
(
    image_set_id BINARY(16) NOT NULL,
    images_id    BINARY(16) NOT NULL,
    CONSTRAINT pk_imageset_images PRIMARY KEY (image_set_id, images_id)
);

CREATE TABLE image_set_tags
(
    image_set_id BINARY(16) NOT NULL,
    tags_tag_id  BINARY(16) NOT NULL,
    CONSTRAINT pk_imageset_tags PRIMARY KEY (image_set_id, tags_tag_id)
);

CREATE TABLE image_tags
(
    image_id    BINARY(16) NOT NULL,
    tags_tag_id BINARY(16) NOT NULL,
    CONSTRAINT pk_image_tags PRIMARY KEY (image_id, tags_tag_id)
);

CREATE TABLE null_images
(
    comic_book_id BINARY(16) NOT NULL,
    images_id     BINARY(16) NOT NULL,
    CONSTRAINT pk_imageset_images PRIMARY KEY (comic_book_id, images_id)
);

CREATE TABLE null_tags
(
    comic_book_id BINARY(16) NOT NULL,
    tags_tag_id   BINARY(16) NOT NULL,
    CONSTRAINT pk_imageset_tags PRIMARY KEY (comic_book_id, tags_tag_id)
);

CREATE TABLE tag
(
    tag_id BINARY(16) NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (tag_id)
);

CREATE TABLE user_preferences
(
    id       BINARY(16)   NOT NULL,
    username VARCHAR(255) NULL,
    CONSTRAINT pk_userpreferences PRIMARY KEY (id)
);

CREATE TABLE user_preferences_favorite_comics
(
    user_preferences_id BINARY(16) NOT NULL,
    favorite_comics_id  BINARY(16) NOT NULL,
    CONSTRAINT pk_userpreferences_favoritecomics PRIMARY KEY (user_preferences_id, favorite_comics_id)
);

CREATE TABLE user_preferences_favorite_pictures
(
    user_preferences_id  BINARY(16) NOT NULL,
    favorite_pictures_id BINARY(16) NOT NULL,
    CONSTRAINT pk_userpreferences_favoritepictures PRIMARY KEY (user_preferences_id, favorite_pictures_id)
);

CREATE TABLE user_preferences_favorite_videos
(
    user_preferences_id BINARY(16) NOT NULL,
    favorite_videos_id  BINARY(16) NOT NULL,
    CONSTRAINT pk_userpreferences_favoritevideos PRIMARY KEY (user_preferences_id, favorite_videos_id)
);

CREATE TABLE video
(
    id       BINARY(16) NOT NULL,
    metadata LONGTEXT   NULL,
    CONSTRAINT pk_video PRIMARY KEY (id)
);

CREATE TABLE video_tags
(
    video_id    BINARY(16) NOT NULL,
    tags_tag_id BINARY(16) NOT NULL,
    CONSTRAINT pk_video_tags PRIMARY KEY (video_id, tags_tag_id)
);

ALTER TABLE image_set_images
    ADD CONSTRAINT uc_image_set_images_images UNIQUE (images_id);

ALTER TABLE null_images
    ADD CONSTRAINT uc_null_images_images UNIQUE (images_id);

ALTER TABLE user_preferences_favorite_comics
    ADD CONSTRAINT uc_user_preferences_favorite_comics_favoritecomics UNIQUE (favorite_comics_id);

ALTER TABLE user_preferences_favorite_pictures
    ADD CONSTRAINT uc_user_preferences_favorite_pictures_favoritepictures UNIQUE (favorite_pictures_id);

ALTER TABLE user_preferences_favorite_videos
    ADD CONSTRAINT uc_user_preferences_favorite_videos_favoritevideos UNIQUE (favorite_videos_id);

ALTER TABLE image_set_images
    ADD CONSTRAINT fk_imasetima_on_image FOREIGN KEY (images_id) REFERENCES image (id);

ALTER TABLE image_set_images
    ADD CONSTRAINT fk_imasetima_on_image_set FOREIGN KEY (image_set_id) REFERENCES image_set (id);

ALTER TABLE image_set_tags
    ADD CONSTRAINT fk_imasettag_on_image_set FOREIGN KEY (image_set_id) REFERENCES image_set (id);

ALTER TABLE image_set_tags
    ADD CONSTRAINT fk_imasettag_on_tag FOREIGN KEY (tags_tag_id) REFERENCES tag (tag_id);

ALTER TABLE image_tags
    ADD CONSTRAINT fk_imatag_on_image FOREIGN KEY (image_id) REFERENCES image (id);

ALTER TABLE image_tags
    ADD CONSTRAINT fk_imatag_on_tag FOREIGN KEY (tags_tag_id) REFERENCES tag (tag_id);

ALTER TABLE null_images
    ADD CONSTRAINT fk_nulima_on_comic_book FOREIGN KEY (comic_book_id) REFERENCES image_set (id);

ALTER TABLE null_images
    ADD CONSTRAINT fk_nulima_on_image FOREIGN KEY (images_id) REFERENCES image (id);

ALTER TABLE null_tags
    ADD CONSTRAINT fk_null_tags_on_comic_book FOREIGN KEY (comic_book_id) REFERENCES image_set (id);

ALTER TABLE null_tags
    ADD CONSTRAINT fk_null_tags_on_tag FOREIGN KEY (tags_tag_id) REFERENCES tag (tag_id);

ALTER TABLE user_preferences_favorite_comics
    ADD CONSTRAINT fk_useprefavcom_on_comic_book FOREIGN KEY (favorite_comics_id) REFERENCES image_set (id);

ALTER TABLE user_preferences_favorite_comics
    ADD CONSTRAINT fk_useprefavcom_on_user_preferences FOREIGN KEY (user_preferences_id) REFERENCES user_preferences (id);

ALTER TABLE user_preferences_favorite_pictures
    ADD CONSTRAINT fk_useprefavpic_on_picture FOREIGN KEY (favorite_pictures_id) REFERENCES image (id);

ALTER TABLE user_preferences_favorite_pictures
    ADD CONSTRAINT fk_useprefavpic_on_user_preferences FOREIGN KEY (user_preferences_id) REFERENCES user_preferences (id);

ALTER TABLE user_preferences_favorite_videos
    ADD CONSTRAINT fk_useprefavvid_on_user_preferences FOREIGN KEY (user_preferences_id) REFERENCES user_preferences (id);

ALTER TABLE user_preferences_favorite_videos
    ADD CONSTRAINT fk_useprefavvid_on_video FOREIGN KEY (favorite_videos_id) REFERENCES video (id);

ALTER TABLE video_tags
    ADD CONSTRAINT fk_vidtag_on_tag FOREIGN KEY (tags_tag_id) REFERENCES tag (tag_id);

ALTER TABLE video_tags
    ADD CONSTRAINT fk_vidtag_on_video FOREIGN KEY (video_id) REFERENCES video (id);