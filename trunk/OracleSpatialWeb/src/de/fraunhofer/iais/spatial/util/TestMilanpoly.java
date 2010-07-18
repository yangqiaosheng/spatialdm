package de.fraunhofer.iais.spatial.util;

import java.util.List;

import de.fraunhofer.iais.spatial.dao.MilanpolyDao;
import de.fraunhofer.iais.spatial.dao.ibatis.MilanpolyDaoIbatis;
import de.fraunhofer.iais.spatial.entity.Milanpoly;

public class TestMilanpoly {
	public static void main(String[] args) throws Exception {
		//		MilanpolyDao dao = new MilanpolyDaoJdbc();
		MilanpolyDao dao = new MilanpolyDaoIbatis();
		MilanpolyDao dao2 = new MilanpolyDaoIbatis();

		List<Milanpoly> ms = dao.getAllMilanpolys();
		List<Milanpoly> ms2 = dao2.getMilanpolysByRect(1, 1, 96.5, 95.4);

		System.out.println((dao.getMilanpolyById(31)).getArea());

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

			System.out.println(m.getName() + " area:" + m.getArea() + "\t" + " cluster:" + m.getClustering() + "\t" + coordinates + "\n");
		}

		for (Milanpoly m : ms2) {
			String coordinates = "\t";
			if (m.getGeom().getOrdinatesArray() != null) {
				for (int i = 0; i < m.getGeom().getOrdinatesArray().length; i++) {
					coordinates += m.getGeom().getOrdinatesArray()[i] + ", ";
					if (i % 2 == 1) {
						coordinates += "0\t";
					}
				}
			}

			System.out.println(m.getName() + " area:" + m.getArea() + "\t" + " cluster:" + m.getClustering() + "\t" + coordinates + "\n");
		}

	}

}
