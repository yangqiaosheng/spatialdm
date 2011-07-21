CREATE OR REPLACE
PROCEDURE INSERT_D_CUT_OSM5
AS
BEGIN
  DECLARE
    CURSOR c
    IS
      SELECT t.osm_id, t.name, t.ref_, t.way FROM INA.planet_osm_line_new t; -- hier Tabelle mit OSM-Straﬂenobjekten (ganz)
    v_osm_id    INTEGER;
    v_osm_name  VARCHAR2 (255);
    v_osm_name2 VARCHAR2 (255);
    v_geoloc MDSYS.sdo_geometry;
  BEGIN
    OPEN c;
    LOOP
      FETCH c INTO v_osm_id, v_osm_name, v_osm_name2, v_geoloc;
      EXIT
    WHEN c%notfound;
      INSERT INTO d_cut_osm5 -- hier die erstellte neue Tabelle aufrufen (die Segmente erh‰lt)
      SELECT v_osm_id,
        id,
        v_osm_name,
        v_osm_name2,
        prim_name,
        sek_name,
        laenge,
        SDO_GEOM.SDO_LENGTH(sdo_geom.sdo_intersection(a.geo_object, v_geoloc, 0.005), 0.005, 'UNIT=M'),
        CASE
          WHEN v_osm_name IS NOT NULL
          AND prim_name   IS NOT NULL
          THEN LEVENSHTEIN(v_osm_name, prim_name)
          ELSE NULL
        END,
        CASE
          WHEN v_osm_name2 IS NOT NULL
          AND sek_name     IS NOT NULL
          THEN LEVENSHTEIN(v_osm_name2, sek_name)
          ELSE NULL
        END,
        CASE
          WHEN v_osm_name IS NOT NULL
          AND sek_name    IS NOT NULL
          THEN LEVENSHTEIN(v_osm_name, sek_name)
          ELSE NULL
        END,
        CASE
          WHEN v_osm_name2 IS NOT NULL
          AND prim_name    IS NOT NULL
          THEN LEVENSHTEIN(v_osm_name2, prim_name)
          ELSE NULL
        END,
        sdo_geom.sdo_intersection(a.geo_object, v_geoloc, 0.005)
      FROM d_buff5 a -- Puffertabelle aus den Navteq-Geometrien (5m, 10m, 30m)
      WHERE sdo_relate(a.geo_object, v_geoloc, 'mask=anyinteract') = 'TRUE';
    END LOOP;
  END;
END INSERT_D_CUT_OSM5;