	create table "GENNADY_FLICKR"."FLICKR_EUROPE_TAGS_COUNT"(
       "ID" NUMBER not null,
       "RADIUS" NUMBER not null,
       "PERSON" CLOB,
       "HOUR" CLOB,
       "DAY" CLOB,
       "MONTH" CLOB,
       "YEAR" CLOB,
       "TOTAL" CLOB,
        constraint "FLICKR_EUROPE_TAGS_COUNT_PK" primary key ("ID","RADIUS")
    );