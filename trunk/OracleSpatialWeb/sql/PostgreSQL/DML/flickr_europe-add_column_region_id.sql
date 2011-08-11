alter table flickr_europe
add column region_320000_id integer;
alter table flickr_europe
add column region_160000_id integer;
alter table flickr_europe
add column region_80000_id integer;
alter table flickr_europe
add column region_40000_id integer;
alter table flickr_europe
add column region_20000_id integer;
alter table flickr_europe
add column region_10000_id integer;
alter table flickr_europe
add column region_5000_id integer;
alter table flickr_europe
add column region_2500_id integer;
alter table flickr_europe
add column region_1250_id integer;
alter table flickr_europe
add column region_750_id integer;
alter table flickr_europe
add column region_375_id integer;

alter table flickr_europe
add column taken_date_hour char(13);
update flickr_europe set taken_date_hour = to_char(taken_date, 'YYYY-MM-DD@HH24');