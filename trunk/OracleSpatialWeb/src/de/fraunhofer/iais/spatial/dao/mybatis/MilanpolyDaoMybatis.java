package de.fraunhofer.iais.spatial.dao.mybatis;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import oracle.spatial.geometry.JGeometry;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import de.fraunhofer.iais.spatial.dao.MilanpolyDao;
import de.fraunhofer.iais.spatial.entity.Milanpoly;

public class MilanpolyDaoMybatis implements MilanpolyDao {
	public final static String resource = "ibatis-config.xml";
	public static SqlSessionFactory sqlSessionFactory = null;

	public MilanpolyDaoMybatis() {
		if (sqlSessionFactory == null) {
			try {
				Reader reader = Resources.getResourceAsReader(resource);
				sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
				System.out.println("initializing session factory");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Milanpoly> getAllMilanpolys() {
		SqlSession session = sqlSessionFactory.openSession();
		List<Milanpoly> ms = null;
		try {
			ms = session.selectList(Milanpoly.class.getName() + ".selectAll");
		} finally {
			session.close();
		}
		return ms;
	}

	@Override
	public Milanpoly getMilanpolyById(int id) {
		SqlSession session = sqlSessionFactory.openSession();
		Milanpoly m = null;
		try {
			m = (Milanpoly) session.selectOne(Milanpoly.class.getName() + ".selectById", id);
		} finally {
			session.close();
		}
		return m;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Milanpoly> getMilanpolysByPoint(double x, double y) {
		SqlSession session = sqlSessionFactory.openSession();
		JGeometry j_geom = new JGeometry(2001, 8307, x, y, 0, null, null);
		List<Milanpoly> ms = null;
		try {
			ms = session.selectList(Milanpoly.class.getName() + ".select", j_geom);
		} finally {
			session.close();
		}
		return ms;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Milanpoly> getMilanpolysByRect(double x1, double y1, double x2, double y2) {
		SqlSession session = sqlSessionFactory.openSession();
		int[] elemInfo = { 1, 1003, 3 };
		double[] ordinates = { x1, y1, x2, y2 };
		JGeometry j_geom = new JGeometry(2003, 8307, elemInfo, ordinates);
		List<Milanpoly> ms = null;
		try {
			ms = session.selectList(Milanpoly.class.getName() + ".select", j_geom);
		} finally {
			session.close();
		}
		return ms;
	}

}
