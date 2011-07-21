-- Tabellen (Geometrien in WGS84)
-- Nav_D: alle Navteq-Straßen der Kat 4, 5, 7 mit Namen
-- Planet_osm_line_new: alle OSM-Straßen mit highway = (Primary, Primary_link, secondary, secondary_link, tertiary, residential, living_street, service, Forrtway, path, pedestrian, track, cycleway, steps, unclassified, road) aus PostGRES importiert 
-- OSM-Straßen mit und ohne Namen

-- 1. Puffer um Navteq-Straßen bilden
-- in 3 Größen: 5m, 10m, 30m
-- hier 5m: 
-- für alle 3 Puffergrößen ausführen

create table d_buff5 as
SELECT a.*, SDO_GEOM.SDO_BUFFER(GEOLOC, 5, 0.005,'UNIT=M')geo_object
FROM INA.nav_d a;

create table d_buff10 as
SELECT a.*, SDO_GEOM.SDO_BUFFER(GEOLOC, 10, 0.005,'UNIT=M')geo_object
FROM INA.nav_d a;

create table d_buff30 as
SELECT a.*, SDO_GEOM.SDO_BUFFER(GEOLOC, 30, 0.005,'UNIT=M')geo_object
FROM INA.nav_d a;

-- indexieren, da man räumliche Abfragen macht

INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO,
      SRID)
         VALUES ('d_buff5', 'geo_object',
           MDSYS.SDO_DIM_ARRAY
            (MDSYS.SDO_DIM_ELEMENT('X', -180, 180, 0.0111194874),
             MDSYS.SDO_DIM_ELEMENT('Y', -90, 90, 0.0111194874)
            ),8307
           );
          commit;
create index SDX_d_buff5 on d_buff5(geo_object)
      indextype is MDSYS.SPATIAL_INDEX;
commit;

INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO,
      SRID)
         VALUES ('d_buff10', 'geo_object',
           MDSYS.SDO_DIM_ARRAY
            (MDSYS.SDO_DIM_ELEMENT('X', -180, 180, 0.0111194874),
             MDSYS.SDO_DIM_ELEMENT('Y', -90, 90, 0.0111194874)
            ),8307
           );
          commit;
create index SDX_d_buff10 on d_buff10(geo_object)
      indextype is MDSYS.SPATIAL_INDEX;
commit;

INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO,
      SRID)
         VALUES ('d_buff30', 'geo_object',
           MDSYS.SDO_DIM_ARRAY
            (MDSYS.SDO_DIM_ELEMENT('X', -180, 180, 0.0111194874),
             MDSYS.SDO_DIM_ELEMENT('Y', -90, 90, 0.0111194874)
            ),8307
           );
          commit;
create index SDX_d_buff30 on d_buff30(geo_object)
      indextype is MDSYS.SPATIAL_INDEX;
commit;

-- neue Segmentierung der OpenStreetMapStraßen
-- Die Funktion bildet aus der Überschneidung von OSM-Straßen mit einem Navteq-Puffer neue Geometrien und führt gleichzeitig einen Namens- und Längenvergleich durch
-- sie wird für 3 Puffergrößen angewendet (5m, 10m. 30m)
-- Attribute der neuen Tabelle: OSM-Name, OSM-Ref, Navteq-Prim_name, Navteq-Sek_name, Länge Navteq-Element, Länge OSM-Segment, 4 Namensvergleiche mit der Levenshtein-Funktion
-- für alle 3 Puffergrößen durchführen

create table d_cut_osm5 -- hier Namen für die neue OSM-Segmenttabelle vergeben (je nach 5m, 10m, 30m)
       (osm_id integer, 
       nav_id integer, 
       osm_name varchar2 (255), 
       osm_name2 varchar2 (255), 
       nav_name varchar2 (255), 
       nav_name2 varchar2 (255),
       nav_laenge number, 
       osm_seglaenge number, 
       LS1 integer, 
       LS2 integer, 
       LS3 integer, 
       LS4 integer, 
       geoloc mdsys.sdo_geometry); -- Variablen definieren 
	   
create table d_cut_osm10 -- hier Namen für die neue OSM-Segmenttabelle vergeben (je nach 5m, 10m, 30m)
       (osm_id integer, 
       nav_id integer, 
       osm_name varchar2 (255), 
       osm_name2 varchar2 (255), 
       nav_name varchar2 (255), 
       nav_name2 varchar2 (255),
       nav_laenge number, 
       osm_seglaenge number, 
       LS1 integer, 
       LS2 integer, 
       LS3 integer, 
       LS4 integer, 
       geoloc mdsys.sdo_geometry); -- Variablen definieren 
	   
create table d_cut_osm30 -- hier Namen für die neue OSM-Segmenttabelle vergeben (je nach 5m, 10m, 30m)
       (osm_id integer, 
       nav_id integer, 
       osm_name varchar2 (255), 
       osm_name2 varchar2 (255), 
       nav_name varchar2 (255), 
       nav_name2 varchar2 (255),
       nav_laenge number, 
       osm_seglaenge number, 
       LS1 integer, 
       LS2 integer, 
       LS3 integer, 
       LS4 integer, 
       geoloc mdsys.sdo_geometry); -- Variablen definieren 


Declare
  cursor c is select t.osm_id, t.name, t.ref_, t.way from INA.planet_osm_line_new t; -- hier Tabelle mit OSM-Straßenobjekten (ganz)
  v_osm_id integer;
  v_osm_name varchar2 (255);
  v_osm_name2 varchar2 (255);
  v_geoloc MDSYS.sdo_geometry;
  
 Begin   
 open c;
    Loop
     Fetch c into v_osm_id, v_osm_name, v_osm_name2, v_geoloc;
      exit when c%notfound;
      insert into d_cut_osm5 -- hier die erstellte neue Tabelle aufrufen (die Segmente erhält)
      select v_osm_id,
             id,
             v_osm_name,
             v_osm_name2,
             prim_name,
             sek_name,
             laenge,
             SDO_GEOM.SDO_LENGTH(sdo_geom.sdo_intersection(a.geo_object,
                                                           v_geoloc,
                                                           0.005),
                                 0.005,
                                 'UNIT=M'),
             case
               when v_osm_name is not null and prim_name is not null then
                LEVENSHTEIN(v_osm_name, prim_name)
               else
                null
             end,
             case
               when v_osm_name2 is not null and sek_name is not null then
                LEVENSHTEIN(v_osm_name2, sek_name)
               else
                null
             end,
             case
               when v_osm_name is not null and sek_name is not null then
                LEVENSHTEIN(v_osm_name, sek_name)
               else
                null
             end,
             case
               when v_osm_name2 is not null and prim_name is not null then
                LEVENSHTEIN(v_osm_name2, prim_name)
               else
                null
             end,
             sdo_geom.sdo_intersection(a.geo_object, v_geoloc, 0.005)
        from d_buff5 a -- Puffertabelle aus den Navteq-Geometrien (5m, 10m, 30m)
       where sdo_relate(a.geo_object, v_geoloc, 'mask=anyinteract') = 'TRUE';
  end loop;
 end;


--Qualitätsattribute an Segmente verschiedener Puffergrößen vergeben

-- für 5m Puffer
create table d_quali5 as 
select a.*,
       case
         when osm_seglaenge * 100 / (nav_laenge + 10) >= 75 and
              (ls1 < 5 or ls2 = 0 or ls3 < 5 or ls4 = 0) and nav_laenge > 5 then
          '5LN'
         when ((ls1 < 5 or ls2 = 0 or ls3 < 5 or ls4 = 0) and
              osm_seglaenge * 100 / (nav_laenge + 10) < 75 and
              nav_laenge > 5) or ((ls1 < 5 or ls2 = 0 or ls3 < 5 or ls4 = 0) and
              nav_laenge <= 5) then
          '5N'
         when osm_seglaenge * 100 / (nav_laenge + 10) >= 75 and
              (ls1 > 4 or ls2 > 0 or ls3 > 4 or ls4 > 0) and nav_laenge > 5 then
          '5L'
         when osm_seglaenge * 100 / (nav_laenge + 10) >= 75 and
              osm_name is null and osm_name2 is null and nav_laenge > 5 then
          '5Lnull'
       end m5
  from d_cut_osm5 a;

-- für 10m Puffer
create table d_quali10 as 
select a.*,
       case
         when osm_seglaenge * 100 / (nav_laenge + 20) >= 75 and
              (ls1 < 5 or ls2 = 0 or ls3 < 5 or ls4 = 0) and nav_laenge > 10 then
          '10LN'
         when ((ls1 < 5 or ls2 = 0 or ls3 < 5 or ls4 = 0) and
              osm_seglaenge * 100 / (nav_laenge + 20) < 75 and
              nav_laenge > 10) or ((ls1 < 5 or ls2 = 0 or ls3 < 5 or ls4 = 0) and
              nav_laenge <= 10) then
          '10N'
         when osm_seglaenge * 100 / (nav_laenge + 20) >= 75 and
              (ls1 > 4 or ls2 > 0 or ls3 > 4 or ls4 > 0) and nav_laenge > 10 then
          '10L'
         when osm_seglaenge * 100 / (nav_laenge + 20) >= 75 and
              osm_name is null and osm_name2 is null and nav_laenge > 10 then
          '10Lnull'
       end m10
  from d_cut_osm10 a;

-- für 30m Puffer
create table d_quali30 as 
select a.*,
       case
         when (ls1 < 5 or ls2 = 0 or ls3 < 5 or ls4 = 0) then
          '30N'
       
       end m30
  from d_cut_osm30 a;

-- Zusammenfügen von allen 
create table d_qualiall as
select *
  from (select c.nav_id, c.osm_id, c.m30, d.m10, d.m5
          from d_quali30 c
          left join (select a.osm_id, a.nav_id, a.m10, b.m5
                      from d_quali10 a
                      left join d_quali5 b on a.osm_id = b.osm_id
                                          and a.nav_id = b.nav_id) d on c.osm_id =
                                                                        d.osm_id
                                                                    and c.nav_id =
                                                                        d.nav_id)
 where m5 is not null
    or m10 is not null
    or m30 is not null;

-- index bilden für osm_id und nav_id

create index nav_id on d_qualiall (nav_id);
create index osm_id on d_qualiall (osm_id);


-- Rangbildung für Zuordnung (mit Kategorieeinschränkung)

create table d_Rang as 
select a.osm_id,
       a.nav_id,
       a.highway,
       a.kat,
       case
         when m5 = '5LN' then
          1
         when m5 = '5N' and m10 = '10LN' then
          2
         when m5 is null and m10 = '10LN' then
          3
         when m5 = '5Lnull' then
          4
         when m5 is null and m10 = '10Lnull' then
          5
         when m5 = '5N' and m10 = '10N' or m5 = '5N' then
          6
         when m5 is null and m10 = '10N' then
          7
         when m5 is null and m10 is null and m30 = '30N' then
          8
         when m5 = '5L' then
          9
         when m5 is null and m10 = '10L' then
          10
       end Rang
  from (select *
          from (select a.*, b.highway, c.kat
                  from d_qualiall a, INA.planet_osm_line_new_2011 b, INA.nav_d c
                 where a.osm_id = b.osm_id
                   and a.nav_id = c.id)
         where (kat = '4' and
               (highway = 'primary' or highway = 'primary_link' or
               highway = 'secondary' or highway = 'secondary_link' or
               highway = 'tertiary'))
            or (kat = '5' and
               (highway = 'secondary' or highway = 'secondary_link' or
               highway = 'tertiary' or highway = 'unclassified' or
               highway = 'residential'))
            or (kat = '7' and
               (highway = 'tertiary' or highway = 'unclassified' or
               highway = 'residential' or highway = 'living_street' or
               highway = 'service' or highway = 'footway' or
               highway = 'path' or highway = 'pedestrian' or
               highway = 'track' or highway = 'cycleway' or
               highway = 'steps'))) a

-- Zuordnung
create table d_zuordnung as 
select a.*
  from (select * from d_rang) a,
       (select nav_id, min(rang) minrang from d_rang group by nav_id) b
 where a.nav_id = b.nav_id
   and a.rang = b.minrang;


-- Rangbildung ohne Kategorieeinschränkung? 
/*create table d_Rang_2 as 
select a.osm_id,
       a.nav_id,
       case
         when m5 = '5LN' then
          1
         when m5 = '5N' and m10 = '10LN' then
          2
         when m5 is null and m10 = '10LN' then
          3
         when m5 = '5Lnull' then
          4
         when m5 is null and m10 = '10Lnull' then
          5
         when m5 = '5N' and m10 = '10N' or m5 = '5N' then
          6
         when m5 is null and m10 = '10N' then
          7
         when m5 is null and m10 is null and m30 = '30N' then
          8
         when m5 = '5L' then
          9
         when m5 is null and m10 = '10L' then
          10
       end Rang
  from d_qualiall a

-- Zuordnen
create table d_zuordnung as 
select a.*
  from (select * from a_rang) a,
       (select nav_id, min(rang) minrang from d_rang group by nav_id) b
 where a.nav_id = b.nav_id
   and a.rang = b.minrang;
   
--ohne Kategorieeinschränkung
/*create table d_zuordnung2 as 
select a.*
  from (select * from d_rang_2) a,
       (select nav_id, min(rang) minrang from d_rang_2 group by nav_id) b
 where a.nav_id = b.nav_id
   and a.rang = b.minrang;
*/
-- Rang grenze
create table d_zuordnung8 as select * from d_zuordnung where rang < 9;

--backup, da man später Stichstraßen entfernt                
create table d_zuordnungbp as select * from d_zuordnung;

--join the result from d_zuordnung with osm and nav_d tables
create table d_zuordnung_all as
select  
  n.gkz,
  z.rang,
  z.nav_id,
  z.osm_id,
  o.highway,
  o.name,
  o.ref_,
  o.oneway,
  o.foot,
  o.maxspeed,
  o.access_,
  o.laenge_full_osm,
  n.prim_name,
  n.sek_name,
  n.kat,
  n.laenge,
  n.richtung,
  n.fuss_zone,
  n.fussweg,
  n.km_h  
from d_zuordnung z,
     ina.nav_d n,
     ina.planet_osm_line_new_2011 o
where z.osm_id = o.osm_id and
      z.nav_id = n.id;



