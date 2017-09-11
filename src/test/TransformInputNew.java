package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.charset.Charset;
import java.rmi.activation.ActivationGroup_Stub;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sound.sampled.LineListener;

import org.w3c.dom.css.ElementCSSInlineStyle;

public class TransformInputNew
{
	public static CsvReader fileInfo;
	public static Map<String, String> cityMap = new HashMap<>(); // airport+city+airportIndex
	public static Map<String, Map<String, List<MaInfo>>> cityWeather = new HashMap<>(); // date+city+weatherMap
	public static List<String> weatherList = new ArrayList<>();
	public static Map<String, Map<String, List<Date>>> routeWeather = new HashMap<>();// date+airport+startTime+endTime
	public static Map<String, Map<String, List<FdInfo>>> flightInfos = new HashMap<>(); // startPort+landPort+date+fdinfo
	public static Map<String, Map<String, List<FdInfo>>> planeInfos = new HashMap<>(); // date+planeNo+fdinfo
	public static Map<String, ThroughputInfo> throughputInfos = new HashMap<>(); // airport + (throughput + delayTimes)
	public static Map<String, Float> startDelayRate = new HashMap<>(); // airport + delayRate (top 24 aka throughput > 100000)
	public static Map<String, Float> landDelayRate = new HashMap<>(); // airport + delayRate (top 24 aka throughput > 100000)
	public static Map<String, ThroughputInfo> flightComInfos = new HashMap<>(); //flightCompany + (flightTimes + delayTimes)
	public static Map<String, Float> flightDelayRate = new HashMap<>(); // filghtCompany + delayRate (top 16 aka throughput > 100000)
	public static List<String> specialWeatherList = new ArrayList<>();
	public static Map<String, Map<String, List<Integer>>> specialInfo = new HashMap<>();// date+airport+specialWeatherInfo
	//<hour>
	public static Map<String, Map<String, int []>> startNums = new HashMap<>(); //date + startPort + listNums
	public static Map<String, Map<String, int []>> landNums = new HashMap<>(); //date + landPort + listNums
	//</hour>
	public static class FdInfo
	{
		public String startPort;
		public String landPort;
		public String flightNo;
		public Long startTimePlan;
		public Long landTimePlan;
		public Long startTimeReal;
		public Long landTimeReal;
		public String planeNo;
		public int ifDelay = 0;
		public int lineNo;
	}

	public static class ThroughputInfo
	{
		public int inTimes = 0;
		public int outTimes = 0;
		public int inDelay = 0;
		public int outDelay = 0;
		
		public void inCnt(FdInfo fdInfo)
		{
			inTimes++;
			if (fdInfo.ifDelay == 1)
			{
				inDelay++;
			}
		}
		
		public void outCnt(FdInfo fdInfo)
		{
			outTimes++;
			if (fdInfo.ifDelay == 1)
			{
				outDelay++;
			}
		}
	}
	
	public static void initData()
	{
		weatherList.add("晴,星光璀灿,月光明亮,阳光");
		weatherList.add("阴");
		weatherList.add("云");
		weatherList.add("小雨,中雨,阵雨");
		weatherList.add("小雪,中雪");
		weatherList.add("风");
		weatherList.add("冰雹");
		weatherList.add("雾");
		weatherList.add("沙");
		weatherList.add("霾");
		weatherList.add("雷雨，雷阵雨");
		weatherList.add("大雨，暴雨");
		weatherList.add("大雪,暴雪");
		specialWeatherList.add("预警");
		specialWeatherList.add("通行能力");
		specialWeatherList.add("延误");
		specialWeatherList.add("能见度");
		specialWeatherList.add("雷雨");
		specialWeatherList.add("雾");
		specialWeatherList.add("云");
		specialWeatherList.add("风");
		specialWeatherList.add("沙尘");
		specialWeatherList.add("雪");
		specialWeatherList.add("冰");
		specialWeatherList.add("水");
	}

	public static void loadCityMap(String fileName)
	{
		File file = new File(fileName);
		FileInputStream inputFile;
		try
		{
			inputFile = new FileInputStream(file);
			fileInfo = new CsvReader(inputFile, Charset.forName("UTF-8"));
			fileInfo.readHeaders();

			while (fileInfo.readRecord())
			{
				String airport = fileInfo.get(0);
				String cityName = fileInfo.get(1);
				cityMap.put(airport, cityName);
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void doWeatherMap(String weather, List<Integer> weatherResult)
	{
		for (String string : weatherList)
		{
			String[] weathers = string.split(",");
			int result = 0;
			for (String string2 : weathers)
			{
				if (weather.contains(string2))
				{
					result = 1;
				}
			}
			weatherResult.add(result);
		}
	}

	public static class MaInfo {
		public int morning = 0;
		public int afternoon = 0;
	}
	public static void splitMaInfo(String splitString, String weather, List<MaInfo> weatherResult)
	{
		if (splitString == "早间") {
			for (String weatherString : weatherList)
			{
				MaInfo maInfo = new MaInfo();
				String[] weathers = weatherString.split(",");
				for (String string2 : weathers)
				{
					if (weather.contains(string2))
					{
						maInfo.morning = 1;
					}
					if (string2 == "晴") {
						maInfo.afternoon = 1;
					} else {
						maInfo.afternoon = 0;
					}
				}
				weatherResult.add(maInfo);
			}
		} else if (splitString == "晚间") {
			for (String weatherString : weatherList)
			{
				MaInfo maInfo = new MaInfo();
				String[] weathers = weatherString.split(",");
				for (String string2 : weathers)
				{
					if (weather.contains(string2))
					{
						maInfo.afternoon = 1;
					}
					if (string2 == "晴") {
						maInfo.morning = 1;
					} else {
						maInfo.morning = 0;
					}
				}
				weatherResult.add(maInfo);
			}
		} else {
			String[] s = weather.split(splitString);
			if (s.length == 2) {
				for (String weatherString : weatherList)
				{
					MaInfo maInfo = new MaInfo();
					String[] weathers = weatherString.split(",");
					for (String string2 : weathers)
					{
						if (s[0].contains(string2))
						{
							maInfo.morning = 1;
						}
						if (s[1].contains(string2)) {
							maInfo.afternoon = 1;
						}
					}
					weatherResult.add(maInfo);
				}
			}
		}
	}
	public static void splitWeatherMap(String weather, List<MaInfo> weatherResult)
	{	
		String[] split = {"逐渐","转","后","早间","晚间"};
		// splitString 天气变化的分割词， s 天气分成2部分，
		boolean flag = true;
		for (String splitString : split)
		{
			if (weather.contains(splitString))
			{
				splitMaInfo(splitString, weather, weatherResult);
				flag = false;
			}
		}
		if (flag)
		{
			for (String weatherString : weatherList)
			{
				String[] weathers = weatherString.split(",");
				MaInfo maInfo = new MaInfo();
				for (String string2 : weathers)
				{
					if (weather.contains(string2))
					{
						maInfo.morning = 1;
						maInfo.afternoon = 1;
					}
				}
				weatherResult.add(maInfo);
			}
		}
	}
	
	public static void specialWeatherMap(String weatherInfo, List<Integer> specialWeatherResult)
	{
		for (String string : specialWeatherList)
		{
			int result = 0;
			if (weatherInfo.contains(string))
			{
				result = 1;
			}
			specialWeatherResult.add(result);
		}
	}
	
	public static void cityWeather(String fileName)
	{
		File file = new File(fileName);
		FileInputStream inputFile;
		try
		{
			inputFile = new FileInputStream(file);
			fileInfo = new CsvReader(inputFile, Charset.forName("UTF-8"));
			fileInfo.readHeaders();

			while (fileInfo.readRecord())
			{
				String city = fileInfo.get(0);
				String weather = fileInfo.get(1);
				String lowestTemp = fileInfo.get(2).trim().contains(" ") ? "" : fileInfo.get(2).trim();
				String highestTemp = fileInfo.get(3).trim().contains(" ") ? "" : fileInfo.get(3).trim();
				String date;
				if (fileInfo.get(4).contains("-"))
				{
					date = fileInfo.get(4);
				} else
				{
					Date dateForm = new Date(fileInfo.get(4));
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					date = sdf.format(dateForm);
				}

				List<MaInfo> weatherResult = new ArrayList<>();
				splitWeatherMap(weather, weatherResult);
				MaInfo maInfo = new MaInfo();
				if (!lowestTemp.isEmpty())
				{
					maInfo.morning = Integer.parseInt(lowestTemp);
				} else
				{
					maInfo.morning = -999;
				}
				if (!highestTemp.isEmpty())
				{
					maInfo.afternoon = Integer.parseInt(highestTemp);
				} else
				{
					maInfo.afternoon = -999;
				}
				weatherResult.add(maInfo);
				if (!cityWeather.containsKey(date))
				{
					cityWeather.put(date, new HashMap<>());
				}
				cityWeather.get(date).put(city, weatherResult);
			}
			// for (Entry<String, Map<String, List<Integer>>> integer :
			// cityWeather.entrySet())
			// {
			// System.out.println(integer.getKey());
			// }
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void loadRouteWeather(String fileName)
	{
		File file = new File(fileName);
		FileInputStream inputFile;
		try
		{
			inputFile = new FileInputStream(file);
			fileInfo = new CsvReader(inputFile, Charset.forName("UTF-8"));
			fileInfo.readHeaders();

			while (fileInfo.readRecord())
			{
				String airport = fileInfo.get(0);
				String startTime = fileInfo.get(2);
				String endTime = fileInfo.get(3);
				String weatherInfo = fileInfo.get(4);
				// System.out.println(startTime + "\t" + endTime + "\t" +
				// weatherInfo);

				if (!startTime.isEmpty() && !endTime.isEmpty())
				{
					SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					SimpleDateFormat dfs1 = new SimpleDateFormat("yyyy-MM-dd");
					Date startTime1 = dfs.parse(startTime);
					String date = dfs1.format(startTime1);
					Date endTime1 = dfs.parse(endTime);

					if (!routeWeather.containsKey(date))
					{
						routeWeather.put(date, new HashMap<>());
					}
					Map<String, List<Date>> routeWeatherPort = routeWeather.get(date);
					if (!routeWeatherPort.containsKey(airport))
					{
						routeWeatherPort.put(airport, new ArrayList<>());
					}
					routeWeatherPort.get(airport).add(startTime1);
					routeWeatherPort.get(airport).add(endTime1);
					if (!specialInfo.containsKey(date)) {
						specialInfo.put(date,  new HashMap<>());
					}
					Map<String, List<Integer>> specialInfoPort = specialInfo.get(date);
					List<Integer> specialWeatherResult = new ArrayList<>();
					specialWeatherMap(weatherInfo, specialWeatherResult);
					specialInfoPort.put(airport, specialWeatherResult);
					// System.out.println(startTime1 + "\t" + endTime1 + "\t" +
					// date);
				}

			}
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (ParseException e)
		{
			e.printStackTrace();
		}
	}

	public static void loadFd(String fileName)
	{
		File file = new File(fileName);
		FileInputStream inputFile;
		try
		{
			inputFile = new FileInputStream(file);
			fileInfo = new CsvReader(inputFile, Charset.forName("UTF-8"));
			fileInfo.readHeaders();
			int line = 0;

			while (fileInfo.readRecord())
			{
				FdInfo fdInfo = new FdInfo();
				fdInfo.startPort = fileInfo.get(0);
				fdInfo.landPort = fileInfo.get(1);
				fdInfo.flightNo = fileInfo.get(2);
				String startTimePlan = fileInfo.get(3);
				String landTimePlan = fileInfo.get(4);
				String startTimeReal = fileInfo.get(5);
				String landTimeReal = fileInfo.get(6);
				fdInfo.planeNo = fileInfo.get(7);
				fdInfo.ifDelay = fileInfo.get(8).equals("取消") ? 1 : 0;
				fdInfo.lineNo = line;
				line++;
				// System.out.println(fdInfo.startPort + "\t" + fdInfo.landPort
				// + "\t" + fdInfo.flightNo + "\t" + startTimePlan);
				SimpleDateFormat dfs1 = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dfs2 = new SimpleDateFormat("dd");
				SimpleDateFormat dfs3 = new SimpleDateFormat("HH");
				if (!startTimePlan.isEmpty())
				{
					fdInfo.startTimePlan = new Long(startTimePlan);
					String date = dfs1.format(new Date(fdInfo.startTimePlan * 1000));
					if (!startTimeReal.isEmpty())
					{
						fdInfo.startTimeReal = new Long(startTimeReal);
						long diff = fdInfo.startTimeReal - fdInfo.startTimePlan;
						long diffStd = 3 * 60 * 60;
						if (diff >= diffStd)
						{
							fdInfo.ifDelay = 1;
						}
						//<hour>
						Date startReal = new Date(fdInfo.startTimeReal * 1000);
						int startHour = Integer.parseInt(dfs3.format(startReal));
						String startDay = dfs2.format(startReal);
						if (!startNums.containsKey(startDay)) {
							startNums.put(startDay, new HashMap<>());
						}
						Map<String, int[]> startPort = startNums.get(startDay);
						
						if (!startPort.containsKey(fdInfo.startPort)) {
							startPort.put(fdInfo.startPort, new int[24]);
						}
						int[] startResult = startPort.get(fdInfo.startPort);
						for (int i = 0; i <= 23; i++) {
							if (startHour == i) {
								startResult[i] += 1;
							}
						}
						startPort.put(fdInfo.startPort, startResult);
						//</hour>
					}

					if (!landTimePlan.isEmpty())
					{
						fdInfo.landTimePlan = new Long(landTimePlan);
						//<hour>
						if (!landTimeReal.isEmpty()) {
							fdInfo.landTimeReal = new Long(landTimeReal);
							Date landReal = new Date(fdInfo.landTimeReal * 1000);
							int landHour = Integer.parseInt(dfs3.format(landReal));
							String landDay = dfs2.format(landReal);
							if (!landNums.containsKey(landDay)) {
								landNums.put(landDay, new HashMap<>());
							}
							Map<String, int[]> landPort = landNums.get(landDay);
							
							if (!landPort.containsKey(fdInfo.landPort)) {
								landPort.put(fdInfo.landPort, new int[24]);
							}
							int[] landResult = landPort.get(fdInfo.landPort);
							for (int i = 0; i <= 23; i++) {
								if (landHour == i) {
									landResult[i] += 1;
								}
							}
							landPort.put(fdInfo.landPort, landResult);
						}
						//</hour>
					}

					String airports = fdInfo.startPort + fdInfo.landPort;
					if (!flightInfos.containsKey(airports))
					{
						flightInfos.put(airports, new HashMap<>());
					}
					Map<String, List<FdInfo>> flightInfosDate = flightInfos.get(airports);
					if (!flightInfosDate.containsKey(date))
					{
						flightInfosDate.put(date, new ArrayList<>());
					}
					flightInfosDate.get(date).add(fdInfo);

					if (!planeInfos.containsKey(date))
					{
						planeInfos.put(date, new HashMap<>());
					}
					Map<String, List<FdInfo>> planeInfosNo = planeInfos.get(date);
					if (!planeInfosNo.containsKey(fdInfo.planeNo))
					{
						planeInfosNo.put(fdInfo.planeNo, new ArrayList<>());
					}
					planeInfosNo.get(fdInfo.planeNo).add(fdInfo);
				}
				
				if (!throughputInfos.containsKey(fdInfo.startPort))
				{
					throughputInfos.put(fdInfo.startPort, new ThroughputInfo());
				}
				throughputInfos.get(fdInfo.startPort).inCnt(fdInfo);
				if (!throughputInfos.containsKey(fdInfo.landPort))
				{
					throughputInfos.put(fdInfo.landPort, new ThroughputInfo());
				}
				throughputInfos.get(fdInfo.landPort).outCnt(fdInfo);
				
				String flightCompany = fdInfo.flightNo.substring(0, 2);
				if (!flightComInfos.containsKey(flightCompany))
				{
					flightComInfos.put(flightCompany, new ThroughputInfo());
				}
				flightComInfos.get(flightCompany).inCnt(fdInfo);
			}
			fileInfo.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void processData() throws IOException
	{
		FileWriter outputFile = new FileWriter("test.txt");
		BufferedWriter outputBuffer = new BufferedWriter(outputFile);

		for (Entry<String, Map<String, List<FdInfo>>> planeInfosEntry : planeInfos.entrySet())
		{
			for (Entry<String, List<FdInfo>> planeDateEntry : planeInfosEntry.getValue().entrySet())
			{
				Collections.sort(planeDateEntry.getValue(), new Comparator<FdInfo>() {
					@Override
					public int compare(FdInfo o1, FdInfo o2)
					{
						return o1.startTimePlan.compareTo(o2.startTimePlan);
					}
				});
			}
		}
		
		float rate = 0;
		for (Entry<String, ThroughputInfo> throughputInfo : throughputInfos.entrySet())
		{
			if (throughputInfo.getValue().inTimes > 100000)
			{
				rate = (float)throughputInfo.getValue().inDelay / (float)throughputInfo.getValue().inTimes;
				startDelayRate.put(throughputInfo.getKey(), rate);
			}
			if (throughputInfo.getValue().outTimes > 10000)
			{
				rate = (float)throughputInfo.getValue().outDelay / (float)throughputInfo.getValue().outTimes;
				landDelayRate.put(throughputInfo.getKey(), rate);
			}
		}
		
		for (Entry<String, ThroughputInfo> flightCom : flightComInfos.entrySet())
		{
			if (flightCom.getValue().inTimes > 100000)
			{
				rate = (float)flightCom.getValue().inDelay / (float)flightCom.getValue().inTimes;
				flightDelayRate.put(flightCom.getKey(), rate);
			}
		}

		for (Entry<String, Map<String, List<FdInfo>>> flightInfoEntry : flightInfos.entrySet())
		{
			for (Entry<String, List<FdInfo>> flightDateEntry : flightInfoEntry.getValue().entrySet())
			{
				Collections.sort(flightDateEntry.getValue(), new Comparator<FdInfo>() {
					@Override
					public int compare(FdInfo o1, FdInfo o2)
					{
						return o1.startTimePlan.compareTo(o2.startTimePlan);
					}
				});

				for (FdInfo fdInfo : flightDateEntry.getValue())
				{
					// System.out.print(flightNo + "\t" + date + "\t" + ifDelay
					// + "\t");
					
					outputBuffer.write(fdInfo.lineNo + "\t");
					
					outputBuffer.write(fdInfo.ifDelay + " ");

					String date = flightDateEntry.getKey();
					String startCity = cityMap.get(fdInfo.startPort);
					String landCity = cityMap.get(fdInfo.landPort);
					
					int index = 1;
					SimpleDateFormat dfs0 = new SimpleDateFormat("HH");
					int sWeather = Integer.parseInt(dfs0.format(new Date(fdInfo.startTimePlan * 1000)));
					if (startCity != null)
					{
						List<MaInfo> startWeather = cityWeather.get(date).get(startCity);
						if (startWeather != null)
						{
							for (MaInfo i : startWeather)
							{
								if (i.morning != -999)
								{
									//加计划起飞时的天气，上午
									if (sWeather <= 12) {
										outputBuffer.write(index + ":" + i.morning + " ");
										index++;
									}
								}
								if (i.afternoon != -999)
								{
									//下午
									if (sWeather > 12) {
										outputBuffer.write(index + ":" + i.afternoon + " ");
										index++;
									}
								}
							}
						}
					}
					
					index = 51;
					int lWeather = Integer.parseInt(dfs0.format(new Date(fdInfo.landTimePlan * 1000)));
					if (landCity != null)
					{
						List<MaInfo> landWeather = cityWeather.get(date).get(landCity);
						if (landWeather != null)
						{
							for (MaInfo i : landWeather)
							{
								if (i.morning != -999)
								{
									if (lWeather <= 12) {
										outputBuffer.write(index + ":" + i.morning + " ");
										// System.out.print(index + ":" + i + " ");
										index++;
									}
								}
								
								if (i.afternoon != -999)
								{
									if (lWeather > 12) {
										outputBuffer.write(index + ":" + i.afternoon + " ");
										index++;
									}
								}
							}
						}
					}

					index = 101;
					long affectedStart = 0, affectedEnd = 0;
					if (routeWeather.containsKey(date))
					{
						Map<String, List<Date>> routeWeatherPort = routeWeather.get(date);
						if (routeWeatherPort.containsKey(fdInfo.startPort))
						{
							Date startTime = routeWeatherPort.get(fdInfo.startPort).get(0);
							Date endTime = routeWeatherPort.get(fdInfo.startPort).get(1);
							if (fdInfo.startTimePlan > startTime.getTime() / 1000
									&& fdInfo.startTimePlan < endTime.getTime() / 1000)
							{
								outputBuffer.write(index + ":" + 1 + " ");
								index++;
								affectedStart = fdInfo.startTimePlan - startTime.getTime() / 1000;
								affectedEnd = endTime.getTime() / 1000 - fdInfo.startTimePlan;
								outputBuffer.write(index + ":" + affectedStart + " ");
								index++;
								outputBuffer.write(index + ":" + affectedEnd + " ");
								index++;
								List<Integer> startSpecialWeather = specialInfo.get(date).get(fdInfo.startPort);
								for (Integer i : startSpecialWeather) {
									outputBuffer.write(index + ":" + i + " ");
									index++;
								}
							} else
							{
								outputBuffer.write(index + ":" + 0 + " ");
							}
						} else
						{
							outputBuffer.write(index + ":" + 0 + " ");
						}
						
						index = 126;						
						if (routeWeatherPort.containsKey(fdInfo.landPort) && fdInfo.landTimePlan != null)
						{
							Date startTime = routeWeatherPort.get(fdInfo.landPort).get(0);
							Date endTime = routeWeatherPort.get(fdInfo.landPort).get(1);
							if (fdInfo.landTimePlan > startTime.getTime() / 1000
									&& fdInfo.landTimePlan < endTime.getTime() / 1000)
							{
								outputBuffer.write(index + ":" + 1 + " ");
								index++;
								affectedStart = fdInfo.startTimePlan - startTime.getTime() / 1000;
								affectedEnd = endTime.getTime() / 1000 - fdInfo.startTimePlan;
								outputBuffer.write(index + ":" + affectedStart + " ");
								index++;
								outputBuffer.write(index + ":" + affectedEnd + " ");
								index++;
								List<Integer> landSpecialWeather = specialInfo.get(date).get(fdInfo.landPort);
								for (Integer i : landSpecialWeather) {
									outputBuffer.write(index + ":" + i + " ");
									index++;
								}
							} else
							{
								outputBuffer.write(index + ":" + 0 + " ");
							}
						} else
						{
							outputBuffer.write(index + ":" + 0 + " ");
						}
					}

					index = 151;
					int indexFlight = flightDateEntry.getValue().indexOf(fdInfo);
					outputBuffer.write(index + ":" + indexFlight + " ");
					index++;
					if (indexFlight > 0)
					{
						FdInfo fdInfoPre1 = flightDateEntry.getValue().get(indexFlight - 1);
						outputBuffer.write(index + ":" + fdInfoPre1.ifDelay + " ");
						index++;
						if (fdInfoPre1.ifDelay == 1)
						{
							if (fdInfoPre1.startTimeReal != null && fdInfoPre1.startTimeReal - fdInfo.startTimePlan > 2 * 60 *60)
							{
								outputBuffer.write(index + ":" + (fdInfoPre1.startTimeReal - fdInfoPre1.startTimePlan) + " ");
							} else
							{
								outputBuffer.write(index + ":" + 99999 + " ");
							}
						}
					} else
					{
						outputBuffer.write(index + ":" + 0 + " ");
					}
					
					index = 154;
					if (indexFlight > 1)
					{
						FdInfo fdInfoPre2 = flightDateEntry.getValue().get(indexFlight - 2);
						outputBuffer.write(index + ":" + fdInfoPre2.ifDelay + " ");
						index++;
						if (fdInfoPre2.ifDelay == 1)
						{
							if (fdInfoPre2.startTimeReal != null && fdInfoPre2.startTimeReal - fdInfo.startTimePlan > 2 * 60 *60)
							{
								outputBuffer.write(index + ":" + (fdInfoPre2.startTimeReal - fdInfoPre2.startTimePlan) + " ");
							} else
							{
								outputBuffer.write(index + ":" + 99999 + " ");
							}
						}
					} else
					{
						outputBuffer.write(index + ":" + 0 + " ");
					}
					
					index = 156;
					List<FdInfo> planeNoInfos = planeInfos.get(date).get(fdInfo.planeNo);
					int planeIndex = planeNoInfos.indexOf(fdInfo.planeNo);
					outputBuffer.write(index + ":" + planeIndex + " ");
					index++;
					if (planeIndex > 0)
					{
						outputBuffer.write(index + ":" + planeNoInfos.get(planeIndex - 1).ifDelay + " ");
						index++;
						if (planeNoInfos.get(planeIndex - 1).ifDelay ==1)
						{
							Long nowTime = fdInfo.startTimePlan - 2 * 60 * 60;
							if (planeNoInfos.get(planeIndex - 1).landTimeReal != null && nowTime > planeNoInfos.get(planeIndex - 1).landTimeReal)
							{
								outputBuffer.write(index + ":" + (nowTime - planeNoInfos.get(planeIndex - 1).landTimeReal) + " ");
							} else
							{
								outputBuffer.write(index + ":" + 99999 + " ");
							}
						}
					} else
					{
						outputBuffer.write(index + ":" + 0 + " ");
					}
					
					index = 159;
					SimpleDateFormat dfs1 = new SimpleDateFormat("HHmm");
					outputBuffer.write(index + ":" + dfs1.format(new Date(fdInfo.startTimePlan * 1000)) + " ");
					index++;
					outputBuffer.write(index + ":" + dfs1.format(new Date(fdInfo.landTimePlan * 1000)) + " ");
					
//					index = 161;
//					if (startDelayRate.containsKey(fdInfo.startPort))
//					{
//						outputBuffer.write(index + ":" + startDelayRate.get(fdInfo.startPort) + " ");
//					}
//					index++;
//					if (landDelayRate.containsKey(fdInfo.landPort))
//					{
//						outputBuffer.write(index + ":" + landDelayRate.get(fdInfo.landPort) + " ");
//					}
//					index++;
//					
//					if (flightDelayRate.containsKey(fdInfo.flightNo.substring(0 ,2)))
//					{
//						outputBuffer.write(index + ":" + flightDelayRate.get(fdInfo.flightNo.substring(0, 2)) + " ");
//					}
//					outputBuffer.write(index + ":" + fdInfo.flightNo.substring(0, 1));
					//<hour>
					index = 200;
					SimpleDateFormat dfs3 = new SimpleDateFormat("dd");
					SimpleDateFormat dfs4 = new SimpleDateFormat("HH");
					String day = dfs3.format(new Date(fdInfo.startTimePlan * 1000));
					if (startNums.containsKey(day)) {
						Map<String, int[]> startPort = startNums.get(day);
						if (startPort.containsKey(fdInfo.startPort)) {
							int[] nums = startPort.get(fdInfo.startPort);
							if (fdInfo.startTimePlan != null) {
								int s = Integer.parseInt(dfs4.format(new Date(fdInfo.startTimePlan * 1000))) - 3;
								if (s >= 0) {
									outputBuffer.write(index + ":" + nums[s] + " ");
								}
								index++;
								if ((s-1) >= 0) {
									outputBuffer.write(index + ":" + nums[s-1] + " ");
								}
								index++;
							}
						}
					}
					if (landNums.containsKey(day)) {
						Map<String, int[]> landPort = landNums.get(day);
						if (landPort.containsKey(fdInfo.landPort)) {
							int[] nums = landPort.get(fdInfo.landPort);
							if (fdInfo.landTimePlan != null) {
								int l = Integer.parseInt(dfs4.format(new Date(fdInfo.startTimePlan * 1000))) - 3;
								if (l >= 0) {
									outputBuffer.write(index + ":" + nums[l] + " ");
								}
								index++;
								if ((l-1) >= 0) {
									outputBuffer.write(index + ":" + nums[l-1] + " ");
								}
							}
						}
					}
					//</hour>
					outputBuffer.write("\n");
				}
			}
		}
		outputBuffer.close();
		outputFile.close();
	}

	public static void main(String[] args) throws IOException
	{
		String cityFile = args[0];
		String cityWeatherFile = args[1];
		String routeWeatherFile = args[2];
		String fdFile = args[3];

		initData();
		loadCityMap(cityFile);
		cityWeather(cityWeatherFile);
		System.out.println("cityweather Done");
		loadRouteWeather(routeWeatherFile);
		System.out.println("routeWeather Done");
		loadFd(fdFile);
		System.out.println("fdInfo Done");
		processData();
		System.out.println("sort per airport per day Done");
	}

}
