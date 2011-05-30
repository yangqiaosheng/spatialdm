-- Table: flickr_statistic

-- DROP TABLE flickr_statistic;

CREATE TABLE flickr_statistic
(
  checked_date timestamp without time zone NOT NULL,
  world_photo_num numeric NOT NULL,
  europe_photo_num numeric NOT NULL,
  people_num numeric NOT NULL,
  people_photo_checked_num numeric NOT NULL DEFAULT 0,
  people_contact_checked_num numeric NOT NULL DEFAULT 0,
  CONSTRAINT flickr_statistic_pkey PRIMARY KEY (checked_date)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_statistic OWNER TO gennady_flickr;
