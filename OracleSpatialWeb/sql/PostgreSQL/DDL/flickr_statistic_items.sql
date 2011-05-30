-- Table: flickr_statistic_items

-- DROP TABLE flickr_statistic_items;

CREATE TABLE flickr_statistic_items
(
  "name" character varying NOT NULL,
  "value" numeric NOT NULL DEFAULT 0,
  CONSTRAINT flickr_statistic_items_pkey PRIMARY KEY (name)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_statistic_items OWNER TO gennady_flickr;
