-- Table: flickr_people_rel_contact

-- DROP TABLE flickr_people_rel_contact;

CREATE TABLE flickr_people_rel_contact
(
  user_id character varying(20) NOT NULL,
  contact_user_id character varying(20) NOT NULL,
  CONSTRAINT flickr_people_rel_contact_pk PRIMARY KEY (user_id, contact_user_id)
)
WITH (
  OIDS=FALSE
);
