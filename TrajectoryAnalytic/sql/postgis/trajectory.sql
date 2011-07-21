-- Table: trajectory

-- DROP TABLE trajectory;

CREATE TABLE trajectory
(
  "trId" character varying NOT NULL,
  "trN" integer NOT NULL,
  "fromX" double precision,
  "fromY" double precision,
  "toX" double precision,
  "toY" double precision,
  length double precision,
  CONSTRAINT trajectory_pkey PRIMARY KEY ("trId", "trN")
)
WITH (
  OIDS=FALSE
);
ALTER TABLE trajectory OWNER TO gennady_flickr;
