DROP TABLE results IF EXISTS;
DROP TABLE targets IF EXISTS;
DROP TABLE names IF EXISTS;
DROP TABLE identifiers IF EXISTS;
DROP TABLE actors IF EXISTS;
DROP TABLE pgrfas IF EXISTS;

CREATE TABLE pgrfas (
  id bigint IDENTITY PRIMARY KEY,
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
  coll_georef varchar(16) DEFAULT NULL,
  coll_elevation int DEFAULT NULL,
  coll_date varchar(10) DEFAULT NULL,
  ancestry varchar(65536),
  coll_source char(2) DEFAULT NULL,
  historical char(1) DEFAULT NULL,
  progdois varchar(65536) DEFAULT NULL
);

CREATE TABLE actors (
  id bigint IDENTITY PRIMARY KEY,
  sample_id varchar(128)  NOT NULL REFERENCES pgrfas (sample_id),
  role char(2) NOT NULL,
  wiews varchar(16) DEFAULT NULL,
  pid varchar(16) DEFAULT NULL,
  name varchar(128) DEFAULT NULL,
  address varchar(128) DEFAULT NULL,
  country char(3) DEFAULT NULL
);

CREATE TABLE identifiers (
  id bigint IDENTITY PRIMARY KEY,
  sample_id varchar(128)  NOT NULL REFERENCES pgrfas (sample_id),
  type varchar(16) NOT NULL,
  value varchar(128) NOT NULL
);

CREATE TABLE names (
  id bigint IDENTITY PRIMARY KEY,
  sample_id varchar(128)  NOT NULL REFERENCES pgrfas (sample_id),
  name_type varchar(2) NOT NULL,
  name varchar(128) NOT NULL
);

CREATE TABLE targets (
  id bigint IDENTITY PRIMARY KEY,
  sample_id varchar(128)  NOT NULL REFERENCES pgrfas (sample_id),
  value varchar(256) NOT NULL,
  tkws varchar(256) NOT NULL
);

CREATE TABLE results (
  id bigint IDENTITY PRIMARY KEY,
  operation varchar(16) NOT NULL,
  genus varchar(64) DEFAULT NULL,
  sample_id varchar(128) NOT NULL,
  doi varchar(128) DEFAULT NULL,
  result varchar(2) NOT NULL,
  error varchar(32768)
);
