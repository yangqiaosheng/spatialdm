package de.fraunhofer.iais.spatial.test;

import java.util.List;

import org.junit.Test;

import de.fraunhofer.iais.spatial.dao.MilanpolyDao;
import de.fraunhofer.iais.spatial.dao.ibatis.MilanpolyDaoIbatis;
import de.fraunhofer.iais.spatial.dao.jdbc.MilanpolyDaoJdbc;
import de.fraunhofer.iais.spatial.entity.Milanpoly;
import de.fraunhofer.iais.spatial.service.MilanpolyMgr;

public class TestMilanpoly {

	@Test
	public void testQuery() {
		MilanpolyDao dao = new MilanpolyDaoIbatis();
		List<Milanpoly> ms = dao.getAllMilanpolys();
		//List<Milanpoly> ms = dao.getMilanpolysByPoint(9.17, 45.46);
		// List<Milanpoly> ms = dao.getMilanpolysByRect(1, 1, 96.5, 95.4);

		for (Milanpoly m : ms) {
			String coordinates = "\t";
			if (m.getGeom().getOrdinatesArray() != null) {
				for (int i = 0; i < m.getGeom().getOrdinatesArray().length; i++) {
					coordinates += m.getGeom().getOrdinatesArray()[i] + ", ";
					if (i % 2 == 1) {
						coordinates += "0\t";
					}
				}
			}

			System.out.println(m.getName() + " area:" + m.getArea() + "\t" + coordinates + "\n");
		}
	}

	@Test
	public void testKml() {

		MilanpolyDao dao = new MilanpolyDaoJdbc();
		List<Milanpoly> ms = dao.getMilanpolysByPoint(9.17, 45.46);
		MilanpolyMgr kmlMgr = new MilanpolyMgr();
		System.out.println(kmlMgr.createKml(ms));
	}
}
