create table planet_osm_line_new as (
	select * from planet_osm_line where ( 	
						highway = 'primary' or
						highway = 'primary_link' or
						highway = 'secondary' or
						highway = 'secondary_link' or
						highway = 'tertiary' or
						highway = 'unclassified' or
						highway = 'residential' or
						highway = 'living_street' or
						highway = 'service' or
						highway = 'footway' or
						highway = 'path' or
						highway = 'pedestrian' or
						highway = 'cycleway' or
						highway = 'track' or
						highway = 'steps' or
						highway = 'road') and 
						osm_id > 0 );