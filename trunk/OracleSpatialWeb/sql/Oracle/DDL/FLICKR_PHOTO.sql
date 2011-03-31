
    create table "GENNADY_FLICKR"."FLICKR_PHOTO"(
       "PHOTO_ID" NUMBER not null,
       "USER_ID" VARCHAR2(20),
       "LONGITUDE" FLOAT(126) not null,
       "LATITUDE" FLOAT(126) not null,
       "TAKEN_DATE" DATE not null,
       "UPLOAD_DATE" DATE not null,
       "VIEWED" NUMBER,
       "TITLE" NVARCHAR2(255),
       "SMALLURL" VARCHAR2(100),
       "PLACE_ID" VARCHAR2(20),
       "WOE_ID" VARCHAR2(20),
       "ACCURACY" NUMBER,
        constraint "FLICKR_PHOTO_PK" primary key ("PHOTO_ID")
    );