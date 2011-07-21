CREATE OR REPLACE
FUNCTION SQUIRREL_GET_ERROR_OFFSET(
        query IN VARCHAR2)
    RETURN NUMBER authid current_user
IS
    l_theCursor INTEGER DEFAULT dbms_sql.open_cursor;
    l_status    INTEGER;
BEGIN
    BEGIN
        dbms_sql.parse( l_theCursor, query, dbms_sql.native );
    EXCEPTION
    WHEN OTHERS THEN
        l_status := dbms_sql.last_error_position;
    END;
        dbms_sql.close_cursor( l_theCursor );
    RETURN l_status;
END; 