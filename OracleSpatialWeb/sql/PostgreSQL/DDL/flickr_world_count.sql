-- Table: flickr_world_count

-- DROP TABLE flickr_world_count;

CREATE TABLE flickr_world_count
(
  id integer NOT NULL,
  radius integer NOT NULL,
  person text,
  "hour" text,
  "day" text,
  "month" text,
  "year" text,
  total integer,
  CONSTRAINT flickr_world_count_pkey PRIMARY KEY (id, radius)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_world_count OWNER TO gennady_flickr;
