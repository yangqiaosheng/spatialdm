-- Table: flickr_photo

-- DROP TABLE flickr_photo;

CREATE TABLE flickr_photo
(
  photo_id numeric NOT NULL,
  user_id character varying(20),
  longitude numeric(15,0) NOT NULL,
  latitude numeric(15,0) NOT NULL,
  taken_date date,
  upload_date date,
  viewed numeric,
  title character varying(255),
  smallurl character varying(100),
  place_id character varying(20),
  woe_id character varying(20),
  accuracy numeric,
  CONSTRAINT flickr_photo_pk PRIMARY KEY (photo_id),
  CONSTRAINT flickr_photo_fk FOREIGN KEY (user_id)
      REFERENCES flickr_people (user_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_photo OWNER TO gennady_flickr;
