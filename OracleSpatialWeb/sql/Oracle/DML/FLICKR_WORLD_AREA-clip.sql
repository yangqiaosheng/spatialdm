update flickr_world_area set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,1),SDO_ORDINATE_ARRAY(-180,-85, 180,-85, 180,85, -180,85, -180,-85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,1),SDO_ORDINATE_ARRAY(-180,-85, 180,-85, 180,85, -180,85, -180,-85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_2560000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_1280000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_640000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_320000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_160000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_80000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_40000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_20000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_10000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_5000 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_2500 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_1250 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;

update flickr_world_area_625 set geom = SDO_GEOM.SDO_INTERSECTION(geom,SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 5e-7)
where SDO_RELATE(geom, SDO_GEOMETRY(2003,8307,NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(-180,-85, 180,85)), 'mask=OVERLAPBDYINTERSECT') = 'TRUE';
commit;