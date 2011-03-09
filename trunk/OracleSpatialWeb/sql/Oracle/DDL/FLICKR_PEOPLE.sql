
    create table "GENNADY_FLICKR"."FLICKR_PEOPLE"(
        "USER_ID" VARCHAR2(20) not null,
       "USERNAME" NVARCHAR2(200),
       "CONTACT_UPDATE_CHECKED" NUMBER default '0' not null,
       "PHOTO_UPDATE_CHECKED_DATE" DATE,
       "PHOTO_UPDATE_CHECKED" NUMBER default '0' not null,
        constraint "FLICKR_PEOPLE_PK" primary key ("USER_ID")
    );

    INSERT INTO flickr_people (user_id, username) VALUES ('49596882@N02', 'haolin_zhi');