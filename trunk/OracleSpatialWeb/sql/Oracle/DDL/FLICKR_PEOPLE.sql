
    create table "GENNADY_FLICKR"."FLICKR_PEOPLE"(
        "USER_ID" VARCHAR2(20) not null,
       "USERNAME" NVARCHAR2,
       "CONTACT_UPDATE_CHECKED" NUMBER default '0' not null,
       "LAST_UPLOAD_DATE" DATE,
       "LAST_TAKEN_DATE" DATE,
       "PHOTO_UPDATE_CHECKED" NUMBER default '0' not null,
        constraint "FLICKR_PEOPLE_PK" primary key ("USER_ID")
    );

    create unique index "GENNADY_FLICKR"."FLICKR_PEOPLE_PK" on "GENNADY_FLICKR"."FLICKR_PEOPLE"("USER_ID");