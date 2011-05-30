-- Table: flickr_photo

-- DROP TABLE flickr_photo;

CREATE TABLE flickr_world
(
  photo_id numeric NOT NULL,
  user_id character varying(20),
  longitude double precision NOT NULL,
  latitude double precision NOT NULL,
  taken_date timestamp without time zone,
  upload_date timestamp without time zone,
  viewed integer,
  title character varying(1024),
  tags character varying(1024),
  smallurl character varying(100),
  place_id character varying(20),
  woe_id character varying(20),
  accuracy integer,
  CONSTRAINT flickr_photo_pk PRIMARY KEY (photo_id),
  CONSTRAINT flickr_photo_fk FOREIGN KEY (user_id)
      REFERENCES flickr_people (user_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_photo OWNER TO gennady_flickr;
