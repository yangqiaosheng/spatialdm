CREATE OR REPLACE
FUNCTION NORMALIZE_STRING (s1 IN VARCHAR2) RETURN VARCHAR2 DETERMINISTIC
AS
BEGIN
    DECLARE
        p_s1        VARCHAR2(32767) := LOWER(TRIM(s1));
        p_s2        VARCHAR2(32767);
        v_s1_length INTEGER:= NVL(LENGTH(s1), 0);
        c_s         CHAR(1);
        n_s         INTEGER;
    BEGIN

        v_s1_length :=  NVL(LENGTH(p_s1), 0);
        FOR j IN 1 .. v_s1_length LOOP
            c_s := SUBSTR(p_s1, j, 1);
            n_s := ASCII(c_s);
            IF (n_s >= 97 AND n_s <= 122 OR n_s = 32)
               THEN p_s2 := p_s2 || c_s;
            END IF;
        END LOOP;

        RETURN REGEXP_REPLACE(TRIM(p_s2), 'str$', 'strasse', 2, 1, 'i');
--        DBMS_OUTPUT.PUT_LINE(p_s2);
--        IF v_s1_length = 0 THEN
--            RETURN '';
--        END IF;
--
--
--
--        IF INSTR(p_s1, 'strasse', 1) > 0 THEN
--           p_s1 := SUBSTR(p_s1, 1,  instr(p_s1, 'strasse', 1) + 8);
--        ELSIF INSTR(p_s1, 'str', 3) > 0 THEN
--           p_s1 := SUBSTR(p_s1, 1,  instr(p_s1, 'str ', 1) + 3) || 'asse';
--        ELSIF INSTR(p_s1, 'weg', 2) > 0 THEN
--           p_s1 := SUBSTR(p_s1, 1,  instr(p_s1, 'weg', 1) + 3);
--        ELSIF INSTR(p_s1, 'allee', 2) > 0 THEN
--           p_s1 := SUBSTR(p_s1, 1,  instr(p_s1, 'allee', 1) + 5);
--        END IF;
--        p_s2 := REPLACE(p_s2, ' ');
--        RETURN p_s2;
    END;
END;