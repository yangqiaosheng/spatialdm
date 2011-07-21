CREATE OR REPLACE FUNCTION LEVENSHTEIN(p_source IN VARCHAR2, p_target IN VARCHAR2) RETURN NUMBER DETERMINISTIC AS
BEGIN
    DECLARE
--      Gross-/Kleinschreibung egal
        p_source_string    VARCHAR2(32767) := REGEXP_REPLACE(LOWER(COALESCE(p_source, ' ')), '[^a-z' || COMPOSE('o' || UNISTR('\0308')) || COMPOSE('u' || UNISTR('\0308')) || COMPOSE('a' || UNISTR('\0308')) || UNISTR('\00DF') || ']', '', 1, 0, 'i');
        p_target_string    VARCHAR2(32767) := REGEXP_REPLACE(LOWER(COALESCE(p_target, ' ')), '[^a-z' || COMPOSE('o' || UNISTR('\0308')) || COMPOSE('u' || UNISTR('\0308')) || COMPOSE('a' || UNISTR('\0308')) || UNISTR('\00DF') || ']', '', 1, 0, 'i');

--      Gross-/Kleinschreibung ueberhaupt nicht egal
	--  p_source_string    VARCHAR2(32767) := REGEXP_REPLACE(COALESCE(p_source, ' '), '[^a-z' || COMPOSE('o' || UNISTR('\0308')) || COMPOSE('u' || UNISTR('\0308')) || COMPOSE('a' || UNISTR('\0308')) || UNISTR('\00DF') || ']', '', 1, 0, 'i');
	--  p_target_string    VARCHAR2(32767) := REGEXP_REPLACE(COALESCE(p_target, ' '), '[^a-z' || COMPOSE('o' || UNISTR('\0308')) || COMPOSE('u' || UNISTR('\0308')) || COMPOSE('a' || UNISTR('\0308')) || UNISTR('\00DF') || ']', '', 1, 0, 'i');

        v_length_of_source NUMBER;
        v_length_of_target NUMBER;

        TYPE mytabtype IS TABLE OF NUMBER INDEX BY BINARY_INTEGER;
        column_to_left    mytabtype;
        current_column    mytabtype;
        v_cost            NUMBER := 0;
        err_1             NUMBER;
        err_2             NUMBER;
    BEGIN

--      p_source_string := REPLACE(p_source_string, COMPOSE('o' || UNISTR('\0308')), 'oe');
--      p_source_string := REPLACE(p_source_string, COMPOSE('u' || UNISTR('\0308')), 'ue');
--      p_source_string := REPLACE(p_source_string, COMPOSE('a' || UNISTR('\0308')), 'ue');
--      p_source_string := REPLACE(p_source_string, UNISTR('\00DF'), 'ss');
--
--      p_target_string := REPLACE(p_target_string, COMPOSE('o' || UNISTR('\0308')), 'oe');
--      p_target_string := REPLACE(p_target_string, COMPOSE('u' || UNISTR('\0308')), 'ue');
--      p_target_string := REPLACE(p_target_string, COMPOSE('a' || UNISTR('\0308')), 'ue');
--      p_target_string := REPLACE(p_target_string, UNISTR('\00DF'), 'ss');

        v_length_of_source := LENGTH(p_source_string);
        v_length_of_target := LENGTH(p_target_string);

        IF p_source_string IS NULL AND p_target_string IS NULL OR v_length_of_source = 0 AND v_length_of_target = 0 THEN
--          RETURN 100;
            RETURN 0;
        ELSIF p_source_string IS NULL OR v_length_of_source = 0 THEN
--          RETURN 0;
            RETURN v_length_of_target;
        ELSIF p_target_string IS NULL OR v_length_of_target = 0 THEN
--          RETURN 0;
            RETURN v_length_of_source;
        ELSE
            FOR j IN 0 .. v_length_of_target LOOP
                column_to_left(j) := j;
            END LOOP;
            FOR i IN 1.. v_length_of_source LOOP
                current_column(0) := i;
                FOR j IN 1 .. v_length_of_target LOOP
                    IF SUBSTR (p_source_string, i, 1) = SUBSTR (p_target_string, j, 1) THEN
                        v_cost := 0;
                    ELSE
                        v_cost := 1;
                    END IF;
                    current_column(j) := LEAST (current_column(j-1) + 1, column_to_left(j) + 1, column_to_left(j-1) + v_cost);
                END LOOP;
                FOR j IN 0 .. v_length_of_target  LOOP
                    column_to_left(j) := current_column(j);
                END LOOP;
            END LOOP;
        END IF;

        IF current_column(v_length_of_target) > v_length_of_source THEN
            err_1 := 100;
        ELSE
            err_1 := current_column(v_length_of_target) * 100 / v_length_of_source;
        END IF;
        IF current_column(v_length_of_target) > v_length_of_target THEN
            err_2 := 100;
        ELSE
            err_2 := current_column(v_length_of_target) * 100 / v_length_of_target;
        END IF;

-- 		RETURN  100 - (err_1 + err_2) / 2;
        RETURN current_column(v_length_of_target);
    END;
END;