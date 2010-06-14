package de.fraunhofer.iais.spatial.dao;

import java.util.List;

import de.fraunhofer.iais.spatial.entity.Milanpoly;

public interface MilanpolyDao {

	public abstract List<Milanpoly> getAllMilanpolys();

	public abstract Milanpoly getMilanpolyById(int id);

	public abstract List<Milanpoly> getMilanpolysByPoint(double x, double y);

	public abstract List<Milanpoly> getMilanpolysByRect(double x1, double y1, double x2, double y2);

}