-- Table: flickr_statistic

-- DROP TABLE flickr_statistic;

CREATE TABLE flickr_statistic
(
  checked_date timestamp without time zone NOT NULL,
  world_photo_num numeric NOT NULL,
  europe_photo_num numeric NOT NULL,
  people_num numeric NOT NULL,
  CONSTRAINT flickr_statistic_pkey PRIMARY KEY (checked_date)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_statistic OWNER TO gennady_flickr;
