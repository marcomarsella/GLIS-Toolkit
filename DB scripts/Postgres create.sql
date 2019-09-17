DROP TABLE IF EXISTS results;
DROP TABLE IF EXISTS targets;
DROP TABLE IF EXISTS names;
DROP TABLE IF EXISTS identifiers;
DROP TABLE IF EXISTS actors;
DROP TABLE IF EXISTS results;
DROP TABLE IF EXISTS targets;
DROP TABLE IF EXISTS pgrfas;

CREATE TABLE pgrfas (
  id SERIAL NOT NULL,
  operation varchar(8) NOT NULL,
  sample_id varchar(128) UNIQUE NOT NULL,
  processed char(1) NOT NULL,
  sample_doi varchar(128) DEFAULT NULL,
  date varchar(10) NOT NULL,
  hold_wiews varchar(16) DEFAULT NULL,
  hold_pid varchar(16) DEFAULT NULL,
  hold_name varchar(128) DEFAULT NULL,
  hold_address varchar(128) DEFAULT NULL,
  hold_country char(3) DEFAULT NULL,
  method char(4) NOT NULL,
  genus varchar(64) DEFAULT NULL,
  species varchar(128) DEFAULT NULL,
  sp_auth varchar(64) DEFAULT NULL,
  subtaxa varchar(128) DEFAULT NULL,
  st_auth varchar(64) DEFAULT NULL,
  bio_status char(3) DEFAULT NULL,
  mls_status varchar(2) DEFAULT NULL,
  prov_sid varchar(128) DEFAULT NULL,
  provenance char(3) DEFAULT NULL,
  coll_sid varchar(128) DEFAULT NULL,
  coll_miss_id varchar(128) DEFAULT NULL,
  coll_site varchar(128) DEFAULT NULL,
  coll_lat varchar(100) DEFAULT NULL,
  coll_lon varchar(100) DEFAULT NULL,
  coll_uncert varchar(128) DEFAULT NULL,
  coll_datum varchar(16) DEFAULT NULL,
  coll_georef int DEFAULT NULL,
  coll_elevation varchar(16)  DEFAULT NULL,
  coll_date varchar(10) DEFAULT NULL,
  ancestry TEXT,
  coll_source char(2) DEFAULT NULL,
  historical char(1) DEFAULT NULL,
  progdois varchar(128) DEFAULT NULL
);

CREATE TABLE actors (
  id SERIAL NOT NULL,
  sample_id varchar(128) NOT NULL REFERENCES pgrfas (sample_id),
  role char(2) NOT NULL,
  wiews varchar(16) DEFAULT NULL,
  pid varchar(16) DEFAULT NULL,
  name varchar(128) DEFAULT NULL,
  address varchar(128) DEFAULT NULL,
  country char(3) DEFAULT NULL
);

CREATE TABLE identifiers (
  id SERIAL NOT NULL,
  sample_id varchar(128)  NOT NULL REFERENCES pgrfas (sample_id),
  type varchar(16) NOT NULL,
  value varchar(128) NOT NULL
);

CREATE TABLE names (
  id SERIAL NOT NULL,
  sample_id varchar(128)  NOT NULL REFERENCES pgrfas (sample_id),
  name_type varchar(2) NOT NULL,
  name varchar(128) NOT NULL
);

CREATE TABLE targets (
  id SERIAL NOT NULL,
  sample_id varchar(128)  NOT NULL REFERENCES pgrfas (sample_id),
  value varchar(256) NOT NULL,
  tkws varchar(256) NOT NULL
);

CREATE TABLE results (
  id SERIAL NOT NULL,
  operation varchar(16) NOT NULL,
  genus varchar(64) DEFAULT NULL,
  sample_id varchar(128) NOT NULL,
  doi varchar(128) DEFAULT NULL,
  result varchar(2) NOT NULL,
  error TEXT
);
