# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table airline_code (
  id                        bigint auto_increment not null,
  icao_code                 varchar(255),
  iata_code                 varchar(255),
  airline                   varchar(255),
  call_sign                 varchar(255),
  country_name              varchar(255),
  comments                  varchar(255),
  constraint pk_airline_code primary key (id))
;

create table airport (
  id                        integer auto_increment not null,
  ident                     varchar(255),
  type                      varchar(255),
  airport_name              varchar(255),
  latitude                  varchar(255),
  longitude                 varchar(255),
  elevation_ft              varchar(255),
  continent                 varchar(255),
  iso_country               varchar(255),
  iso_region                varchar(255),
  scheduled_service         varchar(255),
  city_name                 varchar(255),
  iata_code                 varchar(255),
  gps_code                  varchar(255),
  local_code                varchar(255),
  time_zone                 varchar(255),
  dst                       varchar(255),
  country                   varchar(255),
  gmtOffset                 varchar(255),
  constraint pk_airport primary key (id))
;




# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table airline_code;

drop table airport;

SET FOREIGN_KEY_CHECKS=1;

