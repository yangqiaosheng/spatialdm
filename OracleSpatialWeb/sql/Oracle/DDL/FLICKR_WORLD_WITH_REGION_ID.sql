
    create table "GENNADY_FLICKR"."FLICKR_WORLD"(
       "PHOTO_ID" NUMBER not null,
       "USER_ID" VARCHAR2(20),
       "LONGITUDE" FLOAT(126) not null,
       "LATITUDE" FLOAT(126) not null,
       "TAKEN_DATE" DATE not null,
       "UPLOAD_DATE" DATE not null,
       "VIEWED" NUMBER,
       "TITLE" NVARCHAR2(1024),
       "DESCRIPTION" CLOB,
       "TAGS" CLOB,
       "TAGSNUM" NUMBER,
       "SMALLURL" VARCHAR2(100),
       "PLACE_ID" VARCHAR2(20),
       "WOE_ID" VARCHAR2(20),
       "ACCURACY" NUMBER,
       "REGION_2560000_ID" NUMBER,
       "REGION_1280000_ID" NUMBER,
       "REGION_640000_ID" NUMBER,
       "REGION_320000_ID" NUMBER,
       "REGION_160000_ID" NUMBER,
       "REGION_80000_ID" NUMBER,
       "REGION_40000_ID" NUMBER,
       "REGION_20000_ID" NUMBER,
       "REGION_10000_ID" NUMBER,
       "REGION_5000_ID" NUMBER,
       "REGION_2500_ID" NUMBER,
       "REGION_1250_ID" NUMBER,
       "REGION_625_ID" NUMBER,
        constraint "FLICKR_WORLD_PK" primary key ("PHOTO_ID")
    );

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R2560000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_2560000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R1280000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_1280000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R640000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_640000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R320000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_320000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R160000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_160000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R80000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_80000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R40000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_40000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R20000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_20000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R10000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_10000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R5000_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_5000_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R2500_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_2500_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R1250_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_1250_ID", "TAKEN_DATE" desc);

     create index "GENNADY_FLICKR"."FLICKR_WORLD_R625_ID_IDX"
     	on "GENNADY_FLICKR"."FLICKR_WORLD"
     	("REGION_625_ID", "TAKEN_DATE" desc);