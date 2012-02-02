	create table "GENNADY_FLICKR"."FLICKR_WORLD_COUNT"(
       "ID" NUMBER not null,
       "RADIUS" NUMBER not null,
       "PERSON" CLOB,
       "HOUR" CLOB,
       "DAY" CLOB,
       "MONTH" CLOB,
       "YEAR" CLOB,
       "TOTAL" NUMBER,
        constraint "FLICKR_WORLD_COUNT_PK" primary key ("ID","RADIUS")
    );