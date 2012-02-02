-- Table: flickr_world_tags_count

-- DROP TABLE flickr_world_tags_count;

CREATE TABLE flickr_world_tags_count
(
  id integer NOT NULL,
  radius integer NOT NULL,
  person text,
  "hour" text,
  "day" text,
  "month" text,
  "year" text,
  total text,
  tag_num integer,
  tag_sum_num integer,
  CONSTRAINT flickr_world_tags_count_15_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_world_tags_count OWNER TO gennady_flickr;
