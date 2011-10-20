package de.fraunhofer.iais.spatial.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.google.common.collect.Maps;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.dao.jdbc.FlickrAreaDaoOracleJdbc;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.SessionMutex;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.entity.Histograms;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.util.DateUtil;

public class FlickrAreaCancelableJob {

	private FlickrAreaDao flickrAreaDao;

	public FlickrAreaDao getAreaDao() {
		return flickrAreaDao;
	}

	public void setAreaDao(FlickrAreaDao areaDao) {
		this.flickrAreaDao = areaDao;
	}

	public List<FlickrArea> getAllAreas(Date sessionTimestamp, SessionMutex sessionMutex, Radius radius) throws InterruptedException {

		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		for (int areaid : flickrAreaDao.getAllAreaIds(radius)) {
			checkInterruption(sessionTimestamp, sessionMutex);
			areas.add(flickrAreaDao.getAreaById(areaid, radius));
		}

		return areas;
	}

	public List<FlickrArea> getAreasByRect(Date sessionTimestamp, SessionMutex sessionMutex, double x1, double y1, double x2, double y2, Radius radius,  boolean crossDateLine) throws InterruptedException {

		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		for (int areaid : flickrAreaDao.getAreaIdsByRect(x1, y1, x2, y2, radius, crossDateLine)) {
			checkInterruption(sessionTimestamp, sessionMutex);
			areas.add(flickrAreaDao.getAreaById(areaid, radius));
		}

		return areas;
	}

	/**
	 * calculate a Summary Histograms DataSet for all FlickrArea
	 */
	public Histograms calculateSumHistogram(Date sessionTimestamp, SessionMutex sessionMutex, List<FlickrArea> areas, FlickrAreaDto areaDto) throws InterruptedException {

		checkInterruption(sessionTimestamp, sessionMutex);
		Histograms sumHistrograms = new Histograms();

		Map<String, Integer> sumQueryStrData = Maps.newHashMap();
		for (String queryStr : areaDto.getQueryStrs()) {
			sumQueryStrData.put(queryStr, 0);
		}

		Thread.sleep(30);
		for (FlickrArea area : areas) {
			checkInterruption(sessionTimestamp, sessionMutex);

			for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
				if (areaDto.getQueryStrs().contains(e.getKey())) {
					sumQueryStrData.put(e.getKey(), e.getValue() + sumQueryStrData.get(e.getKey()));
				}
			}
		}

		Calendar calendar = DateUtil.createReferenceCalendar();
		calendar.setLenient(false);

		Map<Integer, Integer> sumYearData = sumHistrograms.getYears();
		Map<Integer, Integer> sumMonthData = sumHistrograms.getMonths();
		Map<Integer, Integer> sumDayData = sumHistrograms.getDays();
		Map<Integer, Integer> sumHourData = sumHistrograms.getHours();
		Map<Integer, Integer> sumWeekdayData = sumHistrograms.getWeekdays();

		//set values
		for (Map.Entry<String, Integer> e : sumQueryStrData.entrySet()) {

			int hour = Integer.parseInt(e.getKey().substring(11, 13));
			sumHourData.put(hour, e.getValue() + sumHourData.get(hour));

			int day = Integer.parseInt(e.getKey().substring(8, 10));
			sumDayData.put(day, e.getValue() + sumDayData.get(day));

			int month = Integer.parseInt(e.getKey().substring(5, 7));
			sumMonthData.put(month, e.getValue() + sumMonthData.get(month));

			int year = Integer.parseInt(e.getKey().substring(0, 4));
			sumYearData.put(year, e.getValue() + sumYearData.get(year));

			calendar.set(Calendar.YEAR, Integer.parseInt(e.getKey().substring(0, 4)));
			calendar.set(Calendar.MONTH, Integer.parseInt(e.getKey().substring(5, 7)) - 1);
			calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(e.getKey().substring(8, 10)));
			int weekday = DateUtil.getWeekdayInt(calendar.getTime());
			sumWeekdayData.put(weekday, e.getValue() + sumWeekdayData.get(weekday));
		}

		return sumHistrograms;
	}

	public void countSelected(Date sessionTimestamp, SessionMutex sessionMutex, List<FlickrAreaResult> areaResults, FlickrAreaDto areaDto) throws InterruptedException{

		for (FlickrAreaResult areaResult : areaResults) {
			FlickrArea area = areaResult.getArea();
			checkInterruption(sessionTimestamp, sessionMutex);

			int selectCount = 0;
			Map<String, Integer> counts = null;

			switch (areaDto.getQueryLevel()) {
			case HOUR:
				counts = area.getHoursCount();
				break;
			case DAY:
				counts = area.getDaysCount();
				break;
			case MONTH:
				counts = area.getMonthsCount();
				break;
			case YEAR:
				counts = area.getYearsCount();
				break;
			}

			for (Map.Entry<String, Integer> e : counts.entrySet()) {
				if (areaDto.getQueryStrs().contains(e.getKey())) {
					selectCount += e.getValue();
				}
			}

//			for (String queryStr : areaDto.getQueryStrs()) {
//				if (counts.containsKey(queryStr)) {
//					num += counts.get(queryStr);
//				}
//			}

			areaResult.setSelectedCount(selectCount);
		}

	}

	public FlickrAreaResult countTag(Date sessionTimestamp, SessionMutex sessionMutex, FlickrArea area, FlickrAreaDto areaDto) throws InterruptedException {

		checkInterruption(sessionTimestamp, sessionMutex);
		FlickrAreaResult areaResult = new FlickrAreaResult(area);

		Map<String, Map<String, Integer>> hoursTagsCount = null;
		Map<String, Integer> tagsCount = areaResult.getTagsCount();

		if (MapUtils.isEmpty(area.getHoursTagsCount())) {
			flickrAreaDao.loadHoursTagsCount(area);
		}
		hoursTagsCount = area.getHoursTagsCount();

		System.out.println(hoursTagsCount);

		int i = 0;
		for (String queryStr : areaDto.getQueryStrs()) {
			if (i++ % (24 * 31) == 0) {
				checkInterruption(sessionTimestamp, sessionMutex);
			}
			if (hoursTagsCount.containsKey(queryStr)) {
				Map<String, Integer> tags = hoursTagsCount.get(queryStr);
				for (Map.Entry<String, Integer> e : tags.entrySet()) {
					tagsCount.put(e.getKey(), MapUtils.getIntValue(tagsCount, e.getKey()) + e.getValue());
				}
			}
		}
		return areaResult;
	}

	private void checkInterruption(Date sessionTimestamp, SessionMutex sessionMutex) throws InterruptedException {
		try {
			Thread.sleep(1);
		} catch (InterruptedException e1) {
		}

		synchronized (this) {
			if (!sessionMutex.getTimestamp().equals(sessionTimestamp)) {
				throw new InterruptedException("Interrupted after");
			}
		}
	}

}
