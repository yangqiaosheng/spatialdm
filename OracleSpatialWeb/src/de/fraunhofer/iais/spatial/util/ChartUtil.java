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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.VerticalAlignment;

import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto.Level;

public class ChartUtil {

	public static void createTimeSeriesChartOld(Map<Date, Integer> countsMap, OutputStream os) throws IOException {
		XYDataset timeSeriesDataset = createTimeSeriesDatasetOld(countsMap);
		JFreeChart jfreechart = buildTimeSeriesChartOld(timeSeriesDataset);
		ChartUtilities.writeChartAsPNG(os, jfreechart, 600, 300);
	}

	public static void createXYLineChart(Map<String, Map<Integer, Integer>> countsMap, Level displayLevel, int width, int height, boolean displayLegend, OutputStream os) throws IOException {
		XYDataset xySeriesDataset = createXYSeriesDataset(countsMap);
		JFreeChart jfreechart = buildXYLineChart(xySeriesDataset, displayLevel, displayLegend);
		ChartUtilities.writeChartAsPNG(os, jfreechart, width, height);
	}

	public static void createTimeSeriesChart(Map<String, Map<Date, Integer>> countsMap, Level displayLevel, int width, int height, boolean displayLegend, OutputStream os) throws IOException {
		XYDataset xySeriesDataset = createTimeSeriesDataset(countsMap);
		JFreeChart jfreechart = buildTimeSeriesChart(xySeriesDataset, displayLevel, displayLegend);
		ChartUtilities.writeChartAsPNG(os, jfreechart, width, height);
	}

	private static XYDataset createTimeSeriesDatasetOld(Map<Date, Integer> countsMap) {
		TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();
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
			TimeSeries timeSeries = new TimeSeries(String.valueOf(year));
			for (Map.Entry<Date, Integer> e : countsGroupedMap.get(year).entrySet()) {
				timeSeries.add(new Day(Integer.parseInt(dsdf.format(e.getKey())), //day
						Integer.parseInt(msdf.format(e.getKey())), //month
						2000), //year
						e.getValue()); //value
			}
			timeSeriesCollection.addSeries(timeSeries);

//			TimeSeries avgtimeseries = MovingAverage.createMovingAverage(timeseries, String.valueOf(year), 5, 0);
//			timeseriescollection.addSeries(avgtimeseries);
		}

		return timeSeriesCollection;
	}

	private static XYDataset createTimeSeriesDataset(Map<String, Map<Date, Integer>> countsMap) {
		TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();
		SimpleDateFormat hsdf = new SimpleDateFormat("HH", Locale.ENGLISH);
		SimpleDateFormat dsdf = new SimpleDateFormat("dd", Locale.ENGLISH);
		SimpleDateFormat msdf = new SimpleDateFormat("MM", Locale.ENGLISH);
		SimpleDateFormat ysdf = new SimpleDateFormat("yyyy", Locale.ENGLISH);

		for (String key : countsMap.keySet()) {
			TimeSeries timeSeries = new TimeSeries(key);
			for (Map.Entry<Date, Integer> e : countsMap.get(key).entrySet()) {
				timeSeries.add(new Hour(
						Integer.parseInt(hsdf.format(e.getKey())), //hour
						Integer.parseInt(dsdf.format(e.getKey())), //day
						Integer.parseInt(msdf.format(e.getKey())), //month
						Integer.parseInt(ysdf.format(e.getKey()))), //year
						e.getValue()); //value
			}
			timeSeriesCollection.addSeries(timeSeries);

			//			TimeSeries avgtimeseries = MovingAverage.createMovingAverage(timeseries, String.valueOf(year), 5, 0);
			//			timeseriescollection.addSeries(avgtimeseries);
		}

		return timeSeriesCollection;
	}

	private static XYDataset createXYSeriesDataset(Map<String, Map<Integer, Integer>> countsMap) {

		XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
		for (String key : countsMap.keySet()) {
			XYSeries xySeries = new XYSeries(key, true);
			for (Map.Entry<Integer, Integer> e : countsMap.get(key).entrySet()) {
				xySeries.add(e.getKey(), e.getValue());
			}
			xySeriesCollection.addSeries(xySeries);
			//			TimeSeries avgtimeseries = MovingAverage.createMovingAverage(timeseries, String.valueOf(year), 5, 0);
			//			timeseriescollection.addSeries(avgtimeseries);
		}

		return xySeriesCollection;
	}

	private static JFreeChart buildTimeSeriesChartOld(XYDataset xydataset) {

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

		XYPlot xyPlot = (XYPlot) jfreechart.getPlot();
		xyPlot.setBackgroundPaint(Color.LIGHT_GRAY);
		xyPlot.setDomainGridlinePaint(Color.WHITE);
		xyPlot.setRangeGridlinePaint(Color.WHITE);
		xyPlot.setAxisOffset(new RectangleInsets(5D, 5D, 5D, 5D));
		xyPlot.setDomainCrosshairVisible(true);
		xyPlot.setRangeCrosshairVisible(true);

		DateAxis dateaAis = (DateAxis) xyPlot.getDomainAxis();
		dateaAis.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 1));
		dateaAis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
		dateaAis.setDateFormatOverride(new SimpleDateFormat("MMM", Locale.ENGLISH));

		XYItemRenderer xyItemRenderer = xyPlot.getRenderer();
		XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyItemRenderer;
		xylineandshaperenderer.setBaseShapesVisible(false);

		LegendTitle legend = jfreechart.getLegend();
		legend.setBackgroundPaint(Color.LIGHT_GRAY);
		legend.setPosition(RectangleEdge.RIGHT);
		legend.setVerticalAlignment(VerticalAlignment.CENTER);
		legend.setMargin(10, 0, 10, 10);

		return jfreechart;
	}

	private static JFreeChart buildXYLineChart(XYDataset xydataset, Level displayLevel, boolean displayLegend) {

		JFreeChart jfreechart = ChartFactory.createXYLineChart("#Photos Distribution", // Title
				displayLevel.toString(), // X Label
				"#photos", // Y Label
				xydataset, // dataset
				PlotOrientation.VERTICAL, // orientation
				displayLegend, // show Legend
				false, // generate Tooltips
				false // generate Urls
				);

		jfreechart.setBackgroundPaint(Color.WHITE);
		jfreechart.setBorderPaint(Color.BLACK);

		XYPlot xyPlot = (XYPlot) jfreechart.getPlot();
		xyPlot.setBackgroundPaint(Color.LIGHT_GRAY);
		xyPlot.setDomainGridlinePaint(Color.WHITE);
		xyPlot.setRangeGridlinePaint(Color.WHITE);
		xyPlot.setAxisOffset(new RectangleInsets(5D, 5D, 5D, 5D));
		xyPlot.setDomainCrosshairVisible(true);
		xyPlot.setRangeCrosshairVisible(true);
		xyPlot.getRangeAxis().setUpperMargin(0.10);

		xyPlot.setRenderer(new XYSplineRenderer());
		XYLineAndShapeRenderer xyLineAndShapeRenderer = (XYLineAndShapeRenderer) xyPlot.getRenderer();
		xyLineAndShapeRenderer.setBaseShapesVisible(true);
		xyLineAndShapeRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
		xyLineAndShapeRenderer.setBaseItemLabelsVisible(true);

		NumberAxis domainAxis = (NumberAxis) xyPlot.getDomainAxis();
		domainAxis.setAutoRangeIncludesZero(false);
		domainAxis.setTickUnit(new NumberTickUnit(1));
		switch (displayLevel) {
		case HOUR:
			domainAxis.setRange(DateUtil.allHourInts.first() - 0.5, DateUtil.allHourInts.last() + 0.5);
			break;

		case DAY:
			domainAxis.setRange(DateUtil.allDayInts.first() - 0.5, DateUtil.allDayInts.last() + 0.5);
			break;

		case MONTH:
			domainAxis.setRange(DateUtil.allMonthInts.first() - 0.5, DateUtil.allMonthInts.last() + 0.5);
			break;

		case YEAR:
			domainAxis.setRange(DateUtil.allYearInts.first() - 0.5, DateUtil.allYearInts.last() + 0.5);
			break;
		}

		if (displayLegend) {
			LegendTitle legend = jfreechart.getLegend();
			legend.setBackgroundPaint(Color.LIGHT_GRAY);
			legend.setPosition(RectangleEdge.RIGHT);
			legend.setFrame(new BlockBorder(Color.white));
			legend.setVerticalAlignment(VerticalAlignment.CENTER);
			legend.setMargin(10, 0, 10, 10);

//			XYTitleAnnotation localXYTitleAnnotation = new XYTitleAnnotation(0.98D, 0.02D, legend, RectangleAnchor.BOTTOM_RIGHT);
//		    localXYTitleAnnotation.setMaxWidth(0.48D);
//		    xyPlot.addAnnotation(localXYTitleAnnotation);
		}
		return jfreechart;
	}

	private static JFreeChart buildTimeSeriesChart(XYDataset xydataset, Level displayLevel, boolean displayLegend) {

		JFreeChart jfreechart = ChartFactory.createTimeSeriesChart("#Photos Distribution", // Title
				displayLevel.toString(), // X Label
				"#photos", // Y Label
				xydataset, // dataset
				displayLegend, // show Legend
				false, // generate Tooltips
				false // generate Urls
				);

		jfreechart.setBackgroundPaint(Color.WHITE);
		jfreechart.setBorderPaint(Color.BLACK);

		XYPlot xyPlot = (XYPlot) jfreechart.getPlot();
		xyPlot.setBackgroundPaint(Color.LIGHT_GRAY);
		xyPlot.setDomainGridlinePaint(Color.WHITE);
		xyPlot.setRangeGridlinePaint(Color.WHITE);
		xyPlot.setAxisOffset(new RectangleInsets(5D, 5D, 5D, 5D));
		xyPlot.setDomainCrosshairVisible(true);
		xyPlot.setRangeCrosshairVisible(true);
		xyPlot.getRangeAxis().setUpperMargin(0.10);

		xyPlot.setRenderer(new XYSplineRenderer());
		XYLineAndShapeRenderer xyLineAndShapeRenderer = (XYLineAndShapeRenderer) xyPlot.getRenderer();
		xyLineAndShapeRenderer.setBaseShapesVisible(true);
		xyLineAndShapeRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
		xyLineAndShapeRenderer.setBaseItemLabelsVisible(true);

		DateAxis domainAxis = (DateAxis) xyPlot.getDomainAxis();
		domainAxis.setTickMarkPosition(DateTickMarkPosition.START);
		switch (displayLevel) {
		case HOUR:
			domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 1));
			domainAxis.setDateFormatOverride(new SimpleDateFormat("HH", Locale.ENGLISH));
			domainAxis.setUpperMargin(0.03);
			domainAxis.setLowerMargin(0.03);
			break;

		case DAY:
			domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, 1));
			domainAxis.setDateFormatOverride(new SimpleDateFormat("dd", Locale.ENGLISH));
			domainAxis.setUpperMargin(0.025);
			domainAxis.setLowerMargin(0.025);
			break;

		case MONTH:
			domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 1));
			domainAxis.setDateFormatOverride(new SimpleDateFormat("MMM", Locale.ENGLISH));
			domainAxis.setUpperMargin(0.05);
			domainAxis.setLowerMargin(0.05);
			break;

		case YEAR:
			domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.YEAR, 1));
			domainAxis.setDateFormatOverride(new SimpleDateFormat("yyyy", Locale.ENGLISH));
			domainAxis.setUpperMargin(0.11);
			domainAxis.setLowerMargin(0.11);
			break;

		case WEEKDAY:
			domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, 1));
			domainAxis.setDateFormatOverride(new SimpleDateFormat("E", Locale.ENGLISH));
			domainAxis.setUpperMargin(0.09);
			domainAxis.setLowerMargin(0.09);
			break;
		}

		if (displayLegend) {
			LegendTitle legend = jfreechart.getLegend();
			legend.setBackgroundPaint(Color.LIGHT_GRAY);
			legend.setPosition(RectangleEdge.RIGHT);
			legend.setFrame(new BlockBorder(Color.white));
			legend.setVerticalAlignment(VerticalAlignment.CENTER);
			legend.setMargin(10, 0, 10, 10);

//			XYTitleAnnotation localXYTitleAnnotation = new XYTitleAnnotation(0.98D, 0.02D, legend, RectangleAnchor.BOTTOM_RIGHT);
//		    localXYTitleAnnotation.setMaxWidth(0.48D);
//		    xyPlot.addAnnotation(localXYTitleAnnotation);
		}

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
