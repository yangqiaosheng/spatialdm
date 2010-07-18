package de.fraunhofer.iais.spatial.util;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.VerticalAlignment;

public class ChartUtil {

	public static void createTimeSeriesChart(Map<Date, Integer> countsMap, OutputStream os) throws IOException {
		XYDataset xydataset = createXYDataset(countsMap);
		JFreeChart jfreechart = createXYChart(xydataset);
		ChartUtilities.writeChartAsJPEG(os, 0.8f, jfreechart, 600, 300, null);
	}

	private static XYDataset createXYDataset(Map<Date, Integer> countsMap) {
		TimeSeriesCollection timeseriescollection = new TimeSeriesCollection();
		SimpleDateFormat dsdf = new SimpleDateFormat("dd", Locale.ENGLISH);
		SimpleDateFormat msdf = new SimpleDateFormat("MM", Locale.ENGLISH);
		SimpleDateFormat ysdf = new SimpleDateFormat("yyyy", Locale.ENGLISH);

		// group the values by year
		Map<Integer, Map<Date, Integer>> countsGroupedMap = new TreeMap<Integer, Map<Date, Integer>>();
		for (Map.Entry<Date, Integer> e : countsMap.entrySet()) {
			int year = Integer.valueOf(ysdf.format(e.getKey().getTime()));
			if (!countsGroupedMap.containsKey(year)) {
				Map<Date, Integer> countsSubMap = new TreeMap<Date, Integer>();
				countsGroupedMap.put(year, countsSubMap);
			}
			countsGroupedMap.get(year).put(e.getKey(), e.getValue());
		}

		for (int year : countsGroupedMap.keySet()) {
			TimeSeries timeseries = new TimeSeries(String.valueOf(year));
			for (Map.Entry<Date, Integer> e : countsGroupedMap.get(year).entrySet()) {
				timeseries.add(new Day(Integer.parseInt(dsdf.format(e.getKey())), //day
						Integer.parseInt(msdf.format(e.getKey())), //month
						2000), //year
						e.getValue()); //value
			}
			//			timeseriescollection.addSeries(timeseries);

			TimeSeries avgtimeseries = MovingAverage.createMovingAverage(timeseries, String.valueOf(year), 5, 0);
			timeseriescollection.addSeries(avgtimeseries);
		}

		return timeseriescollection;
	}

	private static JFreeChart createXYChart(XYDataset xydataset) {

		JFreeChart jfreechart = ChartFactory.createTimeSeriesChart("#Photos Distribution", // Title
				"Time", // X Label
				"#photos", // Y Label
				xydataset, // dataset
				true, // show Legend
				false, // generate Tooltips
				false // generate Urls
				);

		jfreechart.setBackgroundPaint(Color.WHITE);
		jfreechart.setBorderPaint(Color.BLACK);
		// jfreechart.setBackgroundPaint(Color.LIGHT_GRAY);

		XYPlot xyplot = (XYPlot) jfreechart.getPlot();
		xyplot.setBackgroundPaint(Color.LIGHT_GRAY);
		xyplot.setDomainGridlinePaint(Color.WHITE);
		xyplot.setRangeGridlinePaint(Color.WHITE);
		xyplot.setAxisOffset(new RectangleInsets(5D, 5D, 5D, 5D));
		xyplot.setDomainCrosshairVisible(true);
		xyplot.setRangeCrosshairVisible(true);

		LegendTitle legend = jfreechart.getLegend();
		legend.setBackgroundPaint(Color.LIGHT_GRAY);
		legend.setPosition(RectangleEdge.RIGHT);
		legend.setVerticalAlignment(VerticalAlignment.CENTER);
		legend.setMargin(10, 0, 10, 10);

		XYItemRenderer xyitemrenderer = xyplot.getRenderer();
		XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyitemrenderer;
		xylineandshaperenderer.setBaseShapesVisible(false);

		DateAxis dateaxis = (DateAxis) xyplot.getDomainAxis();
		dateaxis.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 1));
		dateaxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
		dateaxis.setDateFormatOverride(new SimpleDateFormat("MMM", Locale.ENGLISH));
		return jfreechart;
	}

	@SuppressWarnings("unchecked")
	public static void createBarChart(Map<String, Integer> countsMap, String filename) {
		System.out.println("chart");
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		Iterator it = countsMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			dataset.addValue((Integer) pairs.getValue(), (String) pairs.getKey(), "Category 1");
		}

		JFreeChart chart = ChartFactory.createBarChart(
		// JFreeChart chart = ChartFactory.createLineChart3D(
				"Chart", // Title
				"Month", // X Label
				"Amount of Photo", // Y Label
				dataset, // dataset
				PlotOrientation.VERTICAL, // Plot Orientation
				false, // show Legend
				false, // generate Tooltips
				false // generate Urls
				);

		FileOutputStream fos_jpg = null;

		try {
			fos_jpg = new FileOutputStream(filename);
			ChartUtilities.writeChartAsJPEG(fos_jpg, 0.8f, chart, 800, 600, null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fos_jpg.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

	}
}
