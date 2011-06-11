-- Table: flickr_europe_tags_count

-- DROP TABLE flickr_europe_tags_count;

CREATE TABLE flickr_europe_tags_count
(
  id integer NOT NULL,
  radius integer NOT NULL,
  person text,
  "hour" text,
  "day" text,
  "month" text,
  "year" text,
  total integer,
  CONSTRAINT flickr_europe_tags_count_pkey PRIMARY KEY (id, radius)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_europe_tags_count OWNER TO gennady_flickr;
