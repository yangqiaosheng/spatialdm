package db_work.database;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 31-Jan-2006
 * Time: 16:57:09
 * To change this template use File | Settings | File Templates.
 */
public class SQLStatements {
	public static String sql[] = { "SELECT * FROM !", //0
			"SELECT COUNT(*) FROM !", //1
			"SELECT ! FROM !", //2
			"SELECT ! FROM ! GROUP BY !", //3
			"SELECT !, COUNT(*) FROM ! GROUP BY !", //4
			"SELECT DISTINCT ! FROM ! ", //5 -- ORDER BY 1
			"SELECT min(!), avg(!), max(!) FROM !", //6
			"SELECT count(*) FROM ! WHERE (!<=!)", //7
			"SELECT count(*) FROM ! WHERE ( ! > ? and ! <= ? )", //8
			"SELECT count(*) FROM ! WHERE ! IS NULL", //9
			"SELECT count(DISTINCT !) FROM !", //10 9+10 works slower than 5
			"DROP TABLE !", //11

/*
SELECT V, COUNT(*)
FROM
(SELECT
(CASE
  WHEN LIFE_EXPECTANCY <= 69.354004 THEN 1
  WHEN LIFE_EXPECTANCY <= 70.488 THEN 2
  WHEN LIFE_EXPECTANCY <= 71.622 THEN 3
  WHEN LIFE_EXPECTANCY <= 72.756 THEN 4
  WHEN LIFE_EXPECTANCY <= 73.89 THEN 5
  WHEN LIFE_EXPECTANCY <= 75.024 THEN 6
  WHEN LIFE_EXPECTANCY <= 76.158 THEN 7
  WHEN LIFE_EXPECTANCY <= 77.292 THEN 8
  WHEN LIFE_EXPECTANCY <= 78.425995 THEN 9
  ELSE 10
END) V
FROM eu
WHERE LIFE_EXPECTANCY IS NOT NULL)
GROUP BY V
*/
			"SELECT V! \n" + "FROM\n (SELECT\n!(CASE ", //12
			//"WHEN ! IS NULL THEN NULL ",       //13/12
			"WHEN ! <= ! THEN ! \n", //13/12
			"ELSE ! END) AS V\n FROM ! \n", //14/12
			"WHERE ! IS NOT NULL) TMPTABLE\nGROUP BY V", //15/12

/*
SELECT DISTRICT, COUNT(*)
FROM nwe
WHERE DISTRICT IS NOT NULL
GROUP BY DISTRICT
*/
			"SELECT ! ! \n" + "FROM ! \n" + "WHERE ! IS NOT NULL\n" + "GROUP BY !\n" + "ORDER BY !", //16

			"SELECT min(!), max(!) FROM !", //17

			"SELECT ! \n", //18
			"FROM\n(SELECT !\n", //19/18
			"(CASE ", //20/18
			"WHEN ! <= ! THEN ! \n", //21/18
			"ELSE ! END) AS !\n", //22/18
			"FROM ! \n" + "WHERE ! IS NOT NULL AND ! IS NOT NULL) TMPTABLE\n", //23/18
			"FROM !\n", //24/18
			"GROUP BY !", //25/18

/*
(select rownum tid, a.* from
(select bid, starttime, min(endtime) as endtime from
(select bid, starttime, endtime from
(select id as bid, starttime from
(select id, min(dt) as starttime from adr061228_0 group by id union all select id, nexttime as starttime from adr061228_0
where difftime > 0.2 order by id asc) a) a,
(select id as eid, dt as endtime from adr061228_0 a
where (difftime>0.2 or difftime is null) order by id asc, dt asc) b
where a.bid = b.eid and a.starttime < b.endtime)
group by bid, starttime) a)
*/
			/*
			"(select rownum TID_, a.* from \n"+
			"(select EID_, starttime_, min(endtime_) as endtime_ from \n"+
			"(select EID_, starttime_, endtime_ from \n"+
			"(select ! as EID_, starttime_ from \n"+                          // 0:id
			"(select !, min(!) as starttime_ from ! group by ! union all select !, ! as starttime_ from ! \n"+ // 1:id; 2:dt; 3:tbl; 4:id; 5:id; 6:next; 7:tbl;
			"where (! is null or !) order by ! asc) a) a,\n"+  // 8:condattr; 9:cond; 10: id;
			"(select ! as EID_1, ! as endtime_ from ! a \n"+ // 11:id; 12:dt; 13:tbl;
			"where (! is null or !) order by ! asc, ! asc) b\n"+ // 14:condattr; 15:cond; 16; id; 17:dt;
			"where a.EID_ = b.EID_1 and a.starttime_ <= b.endtime_) \n"+
			"group by EID_, starttime_) a)",                //26 trajectory separation, step 0

			"select \n"+
			    "! as ID,\n"+ // 0: oid
			    "! as DT,\n"+ // 1: time
			    "lag(!) OVER (order by ! desc) as NEXTTIME,\n"+ // 2,3:time;
			    "lag(!) OVER (order by ! desc) - ! as DIFFTIME,\n"+ // 4,5,6:time;
			    "! as X,\n"+ // 7:x
			    "LAG(!) OVER (order by ! desc) as NEXTX,\n"+ // 8:x; 9:time;
			    "! as Y,\n"+ // 10:y
			    "LAG(!) OVER (order by ! desc) as NEXTY,\n"+ // 11:y; 12:time
			    "GREAT_CIRCLE(!,!,LAG(!) OVER (order by ! desc),LAG(!) OVER (order by ! desc)) as DISTANCE "+
			    // 13:y; 14:x; 15:y; 16:time; 17:x; 18:time
			    "!\n"+ // 19:",attr1,attr2" or "" (e.g. ",height,course,speed,PDOP")
			    "from !\n"+ // 20:table name
			    "order by !", // 21:time
			  // 27 movement (without existing ID)

			  "select \n"+
			      "! as ID,\n"+ // 0: oid
			      "! as DT,\n"+ // 1: time
			      "lag(!) OVER (partition by ! order by ! desc) as NEXTTIME,\n"+ // 2,4:time; 3:oid
			      "lag(!) OVER (partition by ! order by ! desc) - ! as DIFFTIME,\n"+ // 5,7,8:time; 6:oid
			      "! as X,\n"+ // 9:x
			      "LAG(!) OVER (partition by ! order by ! desc) as NEXTX,\n"+ // 10:x; 11:oid; 12:time;
			      "! as Y,\n"+ // 13:y
			      "LAG(!) OVER (partition by ! order by ! desc) as NEXTY,\n"+ // 14:y; 15:oid; 16:time
			      "GREAT_CIRCLE(!,!,LAG(!) OVER (partition by ! order by ! desc),LAG(!) OVER (partition by ! order by ! desc)) as DISTANCE "+
			      // 17:y; 18:x; 19:y; 20:oid; 21:time; 22:x; 23:oid; 24:time
			      "!\n"+ // 25:",attr1,attr2" or "" (e.g. ",height,course,speed,PDOP")
			      "from !\n"+ // 26:table name
			      "order by !,!", // 27:oid; 28:time
			    // 28 movement (with existing ID)

			  "create table ! as \n"+"" +  // 0: table prefix
			      "select \n"+
			      "a.TID_, a.TNUM_ - b.TNUM_ + 1 as TNUM_, a.ID_, a.DT_, a.NEXTTIME_, a.DIFFTIME_, a.X_, a.Y_, a.DX_, a.DY_, a.DISTANCE_, a.SPEED_C, a.ACCELERATION_C, a.COURSE_C, a.TURN_C !\n"+
			      // 1: extra attrs
			      "from \n"+
			      "(select rownum as TNUM_, a.* from \n"+
			      "(select a.TID_, a.starttime_, a.endtime_, b.* from \n"+
			      "! a, \n"+ // 2: table prefix
			      "! b \n"+  // 3: table prefix
			      "where a.EID_ = b.ID_ and b.DT_ between a.starttime_ and a.endtime_ order by ID_, DT_) a) a, \n"+
			      "(select a.ID_, a.starttime_, min(TNUM_) as TNUM_ from \n"+
			      "(select rownum as TNUM_, a.ID_, a.starttime_ from \n"+
			      "(select a.TID_, a.starttime_, a.endtime_, b.* from \n"+
			      "! a, \n"+ // 4: table prefix
			      "! b \n"+ // 5: table prefix
			      "where a.EID_ = b.ID_ and b.DT_ between a.starttime_ and a.endtime_ order by ID_, DT_) a) a group by a.ID_, a.starttime_) b\n"+
			      "where a.ID_ = b.ID_ and a.starttime_ = b.starttime_\n"+
			      "order by a.TID_, a.TNUM_",
			  // 29 movement: _0 & _1 => _2

			  "create table ! as\n"+ // 0: prefix
			      "select a.ID_, a.TNUM_, a.TID_, a.DT_, a.X_, a.Y_,\n"+
			      "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.DX_ end) as DX_,\n"+
			      "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.DY_ end) as DY_,\n"+
			      "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.SPEED_C end) as SPEED_C,\n"+
			      "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.COURSE_C end) as COURSE_C,\n"+
			      "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.NEXTTIME_ end) as NEXTTIME_,\n"+
			      "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.DIFFTIME_ end) as DIFFTIME_,\n"+
			      "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.DISTANCE_ end) as DISTANCE_, \n"+
			      "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then a.DIFFTIME_ else null end) as DIFFTIME_NEXT_,\n"+
			      "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then a.DISTANCE_ else null end) as DISTANCE_NEXT_, \n"+
			      "ACCELERATION_C, TURN_C !\n"+ // 1: extra attrs
			      "from ! a, (select max(DT_) as DT_, TID_ from ! group by TID_) b where a.TID_ = b.TID_", // 2,3: prefix
			  // 30 movement: _2 => _3
			 */
			"" //N
	};

	public static String getSQL(int index) {
		return (index < sql.length) ? sql[index] : null;
	}

	public static String getSQL(String sqlstr, String param) {
		int ind = sqlstr.indexOf("!");
		if (ind > 0) {
			String str = sqlstr.substring(0, ind) + param;
			if (ind < str.length() - 1) {
				str += sqlstr.substring(ind + 1, sqlstr.length());
			}
			return str;
		} else
			return sqlstr;
	}

	public static String getSQL(String sqlstr, String params[]) {
		int fromIdx = 0;
		for (String param : params) {
			int ind = sqlstr.indexOf("!", fromIdx);
			if (ind > 0) {
				String prevStr = sqlstr;
				sqlstr = prevStr.substring(0, ind) + param;
				if (ind < prevStr.length() - 1) {
					sqlstr += prevStr.substring(ind + 1, prevStr.length());
				}
				fromIdx = ind + 1;
			}
		}
		return sqlstr;
	}

	public static String getSQL(int index, String params[]) {
		if (index < sql.length) {
			String str = sql[index];
			return getSQL(str, params);
		} else
			return "";
	}

	public static String getSQL(int index, String param) {
		if (index < sql.length) {
			String str = sql[index];
			return getSQL(str, param);
		} else
			return "";
	}

}
