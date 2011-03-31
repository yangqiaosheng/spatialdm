
    create table "GENNADY_FLICKR"."FLICKR_EUROPE"(
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
       "REGION_CHECKED" NUMBER default '0' not null,
       "REGION_5000_ID" NUMBER default '0' not null,
       "REGION_10000_ID" NUMBER default '0' not null,
       "REGION_20000_ID" NUMBER default '0' not null,
       "REGION_40000_ID" NUMBER default '0' not null,
       "REGION_80000_ID" NUMBER default '0' not null,
       "REGION_160000_ID" NUMBER default '0' not null,
       "REGION_320000_ID" NUMBER default '0' not null,
        constraint "FLICKR_EUROPE_PK" primary key ("PHOTO_ID")
    );

    alter table "GENNADY_FLICKR"."FLICKR_EUROPE"
        add constraint "FLICKR_EUROPE_FLICKR_EURO_FK3"
        foreign key ("REGION_10000_ID")
        references "GENNADY_FLICKR"."FLICKR_EUROPE_AREA_10000"("ID");
    alter table "GENNADY_FLICKR"."FLICKR_EUROPE"
        add constraint "FLICKR_EUROPE_FLICKR_EURO_FK6"
        foreign key ("REGION_160000_ID")
        references "GENNADY_FLICKR"."FLICKR_EUROPE_AREA_160000"("ID");
    alter table "GENNADY_FLICKR"."FLICKR_EUROPE"
        add constraint "FLICKR_EUROPE_FLICKR_EURO_FK2"
        foreign key ("REGION_20000_ID")
        references "GENNADY_FLICKR"."FLICKR_EUROPE_AREA_20000"("ID");
    alter table "GENNADY_FLICKR"."FLICKR_EUROPE"
        add constraint "FLICKR_EUROPE_FLICKR_EURO_FK7"
        foreign key ("REGION_320000_ID")
        references "GENNADY_FLICKR"."FLICKR_EUROPE_AREA_320000"("ID");
    alter table "GENNADY_FLICKR"."FLICKR_EUROPE"
        add constraint "FLICKR_EUROPE_FLICKR_EURO_FK1"
        foreign key ("REGION_40000_ID")
        references "GENNADY_FLICKR"."FLICKR_EUROPE_AREA_40000"("ID");
    alter table "GENNADY_FLICKR"."FLICKR_EUROPE"
        add constraint "FLICKR_EUROPE_FLICKR_EURO_FK4"
        foreign key ("REGION_5000_ID")
        references "GENNADY_FLICKR"."FLICKR_EUROPE_AREA_5000"("ID");
    alter table "GENNADY_FLICKR"."FLICKR_EUROPE"
        add constraint "FLICKR_EUROPE_FLICKR_EURO_FK5"
        foreign key ("REGION_80000_ID")
        references "GENNADY_FLICKR"."FLICKR_EUROPE_AREA_80000"("ID");
    alter table "GENNADY_FLICKR"."FLICKR_EUROPE"
        add constraint "FLICKR_EUROPE_FLICKR_PEOPLE_FK"
        foreign key ("USER_ID")
        references "GENNADY_FLICKR"."FLICKR_PEOPLE"("USER_ID");

    create index "GENNADY_FLICKR"."FLICKR_EUROPE_LONLAT_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("LONGITUDE","LATITUDE");
    create index "GENNADY_FLICKR"."FLICKR_EUROPE_TAKEN_DATE_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("TAKEN_DATE" desc);
    create index "GENNADY_FLICKR"."FLICKR_EUROPE_USER_ID_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("USER_ID");
    create index "GENNADY_FLICKR"."FLICKR_EUROPE_R10000_ID_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("REGION_10000_ID");
    create index "GENNADY_FLICKR"."FLICKR_EUROPE_R160000_ID_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("REGION_160000_ID");
    create index "GENNADY_FLICKR"."FLICKR_EUROPE_R20000_ID_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("REGION_20000_ID");
    create index "GENNADY_FLICKR"."FLICKR_EUROPE_R320000_ID_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("REGION_320000_ID");
    create index "GENNADY_FLICKR"."FLICKR_EUROPE_R40000_ID_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("REGION_40000_ID");
    create index "GENNADY_FLICKR"."FLICKR_EUROPE_R5000_ID_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("REGION_5000_ID");
    create index "GENNADY_FLICKR"."FLICKR_EUROPE_R80000_ID_IDX" on "GENNADY_FLICKR"."FLICKR_EUROPE"("REGION_80000_ID");