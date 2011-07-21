-- Table: measurement

-- DROP TABLE measurement;

CREATE TABLE measurement
(
  tr_id character varying NOT NULL,
  tr_n integer NOT NULL,
  p_idx integer NOT NULL,
  x double precision,
  y double precision,
  point geography,
  "time" timestamp without time zone,
  CONSTRAINT measurement_pkey PRIMARY KEY (tr_id, tr_n, p_idx) USING INDEX TABLESPACE gennady
)
WITH (
  OIDS=FALSE
);
ALTER TABLE measurement OWNER TO gennady_flickr;
