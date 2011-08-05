--update region_id of the flickr_europe
update flickr_europe set region_320000_id = (
	select id as region_320000_id from flickr_europe_area_320000 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_160000_id = (
	select id as region_160000_id from flickr_europe_area_160000 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_80000_id = (
	select id as region_80000_id from flickr_europe_area_80000 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_40000_id = (
	select id as region_40000_id from flickr_europe_area_40000 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_20000_id = (
	select id as region_20000_id from flickr_europe_area_20000 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_10000_id = (
	select id as region_10000_id from flickr_europe_area_10000 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_5000_id = (
	select id as region_5000_id from flickr_europe_area_5000 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_2500_id = (
	select id as region_2500_id from flickr_europe_area_2500 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_1250_id = (
	select id as region_1250_id from flickr_europe_area_1250 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_750_id = (
	select id as region_750_id from flickr_europe_area_750 a
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);
update flickr_europe set region_375_id = (
	select id as region_375_id from flickr_europe_area_375 a
	where ST_Intersects(ST_GeomFromsEWKT('SRID=4326;POINT('||longitude||' '||latitude||')'), a.geom::geometry)
);