package de.fraunhofer.iais.spatial.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.SegmentedTimeline;
import org.jfree.chart.axis.Timeline;
import org.jfree.chart.labels.BubbleXYItemLabelGenerator;
import org.jfree.chart.labels.HighLowItemLabelGenerator;
import org.jfree.chart.labels.IntervalXYItemLabelGenerator;
import org.jfree.chart.labels.MultipleXYSeriesLabelGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.StandardXYSeriesLabelGenerator;
import org.jfree.chart.labels.SymbolicXYItemLabelGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
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

	public static void createLineChart(Map<String, Integer> cs, String filename) throws IOException {
		XYDataset xydataset = createXYDataset();
		JFreeChart jfreechart = createChart(xydataset);
		FileOutputStream file = new FileOutputStream(filename);
		ChartUtilities.writeChartAsJPEG(file, 0.8f, jfreechart, 500, 300, null);
	}

	private static XYDataset createXYDataset() {
		TimeSeriesCollection timeseriescollection = new TimeSeriesCollection();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		SimpleDateFormat dsdf = new SimpleDateFormat("dd", Locale.ENGLISH);
		SimpleDateFormat msdf = new SimpleDateFormat("MM", Locale.ENGLISH);

		List<Integer> years = new ArrayList<Integer>();
		years.add(2005);
		// years.add(2006);
		years.add(2007);
		double d = 100D;
		Calendar calendar = Calendar.getInstance();
		Calendar end = Calendar.getInstance();
		for (int y : years) {
			calendar.set(y, 00, 01);
			end.set(y + 1, 00, 01);

			TimeSeries timeseries = new TimeSeries(String.valueOf(y));
			while (calendar.getTime().before(end.getTime())) {
				// System.out.println(sdf.format(calendar.getTime()));
				d = (d + Math.random()) - 0.5D;
				timeseries.add(new Day(Integer.parseInt(dsdf.format(calendar.getTime())), Integer.parseInt(msdf.format(calendar.getTime())), 2000), d);
				// timeseries.add(new Day(calendar.getTime()), d);
				calendar.add(Calendar.DATE, 1);
			}

			timeseriescollection.addSeries(timeseries);

			TimeSeries avgtimeseries = MovingAverage.createMovingAverage(timeseries, "30 day moving average", 30, 0);
//			timeseriescollection.addSeries(avgtimeseries);
		}

		return timeseriescollection;
	}

	private static JFreeChart createChart(XYDataset xydataset) {

		JFreeChart jfreechart = ChartFactory.createTimeSeriesChart("TimeSeriesChart", // Title
				"Time", // X Label
				"#photos", // Y Label
				xydataset, // dataset
				true, // show Legend
				false, // generate Tooltips
				false // generate Urls
				);

		jfreechart.setBackgroundPaint(Color.WHITE);
		jfreechart.setBorderPaint(Color.BLACK);
//		jfreechart.setBackgroundPaint(Color.LIGHT_GRAY);

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
	public static void createBarChart(Map<String, Integer> cs, String filename) {
		System.out.println("chart");
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		Iterator it = cs.entrySet().iterator();
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
