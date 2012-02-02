	create table "GENNADY_FLICKR"."FLICKR_WORLD_TAGS_COUNT"(
       "ID" NUMBER not null,
       "RADIUS" NUMBER not null,
       "PERSON" NCLOB,
       "HOUR" NCLOB,
       "DAY" NCLOB,
       "MONTH" NCLOB,
       "YEAR" NCLOB,
       "TOTAL" NCLOB,
       "TAG_NUM" NUMBER,
       "TAG_SUM_NUM" NUMBER,
        constraint "FLICKR_WORLD_TAGS_COUNT_PK" primary key ("ID","RADIUS")
    );