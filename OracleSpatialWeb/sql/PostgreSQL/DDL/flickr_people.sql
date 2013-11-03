-- Table: flickr_people

-- DROP TABLE flickr_people;

CREATE TABLE flickr_people
(
  user_id character varying(20) NOT NULL,
  username character varying(200),
  contact_num int NOT NULL DEFAULT 0,
  contact_referenced_num int NOT NULL DEFAULT 1,
  contact_update_checked integer NOT NULL DEFAULT 0,
  photo_update_checked_date timestamp without time zone,
  photo_update_checked integer NOT NULL DEFAULT 0,
  world_num integer NOT NULL DEFAULT 0,
  europe_num integer NOT NULL DEFAULT 0,
  CONSTRAINT flickr_people_pk PRIMARY KEY (user_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_people OWNER TO gennady_flickr;

INSERT INTO flickr_people (user_id, username) VALUES ('49596882@N02', 'haolin_zhi');
