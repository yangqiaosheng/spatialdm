	create table "GENNADY_FLICKR"."FLICKR_WORLD_TAGS_COUNT"(
       "ID" NUMBER not null,
       "RADIUS" NUMBER not null,
       "PERSON" CLOB,
       "HOUR" CLOB,
       "DAY" CLOB,
       "MONTH" CLOB,
       "YEAR" CLOB,
       "TOTAL" CLOB,
       "TAG_NUM" NUMBER,
       "TAG_SUM_NUM" NUMBER,
        constraint "FLICKR_WORLD_TAGS_COUNT_PK" primary key ("ID","RADIUS")
    );