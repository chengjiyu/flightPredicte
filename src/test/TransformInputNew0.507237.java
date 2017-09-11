package flightPrediction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformInputNew
{
	public static CsvReader fileInfo;
	public static Map<String, String> cityMap = new HashMap<>(); // airport+city+airportIndex
	public static Map<String, Map<String, List<MaInfo>>> cityWeather = new HashMap<>(); // date+city+weatherMap
	public static List<String> weatherList = new ArrayList<>();
	public static List<String> specialWeatherList = new ArrayList<>();
	public static Map<String, Map<String, List<Special>>> routeWeather = new HashMap<>();// date+airport+startTime+endTime
	public static Map<String, Map<String, List<FdInfo>>> flightInfos = new HashMap<>(); // startPort+landPort+date+fdinfo
	public static Map<String, Map<String, List<FdInfo>>> planeInfos = new HashMap<>(); // date+planeNo+fdinfo
	public static Map<String, Map<Integer, Map<Integer, ThroughputInfo>>> throughputInfos = new HashMap<>(); // airport + season + period + (throughput + delayTimes)
	public static Map<String, Map<Integer, Map<Integer, DelayInfo>>> startDelayRate = new HashMap<>(); // airport + season + period + delayRate (throughput > 100000)
	public static Map<String, Map<Integer, Map<Integer, DelayInfo>>> landDelayRate = new HashMap<>(); // airport + season + period + delayRate (throughput > 100000)
	public static Map<String, Map<Integer, Map<Integer, ThroughputInfo>>> flightComInfos = new HashMap<>(); //flightCompany + season + period + (flightTimes + delayTimes)
	public static Map<String, Map<Integer, Map<Integer, DelayInfo>>> flightDelayRate = new HashMap<>(); // filghtCompany + season + period + delayRate (throughput > 100000)
	public static Map<String, Map<Integer, Map<Integer, ThroughputInfo>>> routeInfos = new HashMap<>(); // route + season + period + (throughput + delayTimes)
	public static Map<String, Map<Integer, Map<Integer, DelayInfo>>> routeDelayRate = new HashMap<>(); // route + season + period + delayRate (throughput > 3000)
	public static Map<String, Map<String, List<Integer>>> specialInfo = new HashMap<>();// date+airport+specialWeatherInfo
	public static Map<String, Map<String, int []>> startNums = new HashMap<>(); //date + startPort + listNums
	public static Map<String, Map<String, int []>> landNums = new HashMap<>(); //date + landPort + listNums
	//0825
	public static Map<String, Map<String, String>> weatherMap = new HashMap<>(); // date+city+weatherInfo
	public static Map<String, WeatherRate> weatherCount = new HashMap<>(); // weatherInfo + WeatherRate
	public static Map<String, Float> weatherRate = new HashMap<>();
	//0825
	
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
		public Long delayDuration = (long) 0;
		public Long firstOut = (long) 2000000000;
		public Long lastOut = (long) 0;
		
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
		
		public void addDuration(Long duration)
		{
			delayDuration += duration;
		}
	}
	
	public static class DelayInfo
	{
		public float rate;
		public float delayDur;
	}
	//0825
	public static class WeatherRate {
		public int count = 0;
		public int delay = 0;
		public void statistics(FdInfo fdInfo) {
			count++;
			if (fdInfo.ifDelay == 1) {
				delay++;
			}
		}
	}
	//0825
	public static void initData()
	{
		weatherList.add("晴,星光璀灿,月光明亮,阳光,暖");
		weatherList.add("阴");
		weatherList.add("云");
		weatherList.add("浮沉");
		weatherList.add("小雨,中雨,阵雨");
		weatherList.add("小雪,中雪");
		weatherList.add("风");
		weatherList.add("冰雹");
		weatherList.add("雾");
		weatherList.add("沙");
		weatherList.add("霾");
		weatherList.add("雷雨,雷阵雨");
		weatherList.add("大雨,暴雨");
		weatherList.add("大雪,暴雪");
		
		specialWeatherList.add("预警");
		specialWeatherList.add("通行能力");
		specialWeatherList.add("延误");
		specialWeatherList.add("起降");
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

	public static class MaInfo {
		public int morning = 0;
		public int afternoon = 0;
	}
	public static void splitMaInfo(String splitString, String weather, List<MaInfo> weatherResult)
	{
		if (splitString.equals("早间")) {
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
					if (string2.equals("晴")) {
						maInfo.afternoon = 1;
					} else {
						maInfo.afternoon = 0;
					}
				}
				weatherResult.add(maInfo);
			}
		} else if (splitString.equals("晚间")) {
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
					if (string2.equals("晴")) {
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
		String num = "\\d+";
		specialWeatherResult.add(null);
		specialWeatherResult.add(null);
		specialWeatherResult.add(null);
		if (weatherInfo.contains("通行能力下降")) {
			String neng = "通行能力下降.*\\d+.%";
			// 创建 Pattern 对象
		    Pattern r1 = Pattern.compile((neng));
		    Matcher m1 = r1.matcher(weatherInfo);
		    if (m1.find()) {
			    Pattern r2 = Pattern.compile((num));
			    Matcher m2 = r2.matcher(m1.group(0));
			    if (m2.find()) {
					specialWeatherResult.set(13, Integer.parseInt(m2.group(0)));
				}
			}
		}
		if (weatherInfo.contains("能见度")) {
			String neng = "能见度.*\\d+.米";
			// 创建 Pattern 对象
		    Pattern r1 = Pattern.compile((neng));
		    Matcher m1 = r1.matcher(weatherInfo);
		    if (m1.find()) {
			    Pattern r2 = Pattern.compile((num));
			    Matcher m2 = r2.matcher(m1.group(0));
			    if (m2.find()) {
					specialWeatherResult.set(14, Integer.parseInt(m2.group(0)));
				}
			}
		}
		if (weatherInfo.contains("云")) {
			String neng = "云.*\\d+.米";
			// 创建 Pattern 对象
		    Pattern r1 = Pattern.compile((neng));
		    Matcher m1 = r1.matcher(weatherInfo);
		    if (m1.find()) {
			    Pattern r2 = Pattern.compile((num));
			    Matcher m2 = r2.matcher(m1.group(0));
			    if (m2.find()) {
					specialWeatherResult.set(15, Integer.parseInt(m2.group(0)));
				}
			}
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
				String date = null;
				if (fileInfo.get(4).contains("-"))
				{
					date = fileInfo.get(4);
				} else if (fileInfo.get(4).contains("/"))
				{
					Date dateForm = new Date(fileInfo.get(4));
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					date = sdf.format(dateForm);
				} else {
					continue;
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
				//0825
				if (!weatherMap.containsKey(date))
				{
					weatherMap.put(date, new HashMap<>());
				}
				weatherMap.get(date).put(city, weather);
				//0825
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

	public static class Special {
		public Date collectTime;
		public Date startTime;
		public Date endTime;
		public List<Integer> weatherInfo = new ArrayList<>();
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
				String airport = fileInfo.get(0).toUpperCase();
				String collectTime = fileInfo.get(1); 
				String startTime = fileInfo.get(2);
				String endTime = fileInfo.get(3);
				String weatherInfo = fileInfo.get(4);

				if (!startTime.isEmpty() && !endTime.isEmpty())
				{
					SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					SimpleDateFormat dfs1 = new SimpleDateFormat("yyyy-MM-dd");
					Date collectTime1 = dfs.parse(collectTime);
					Date startTime1 = dfs.parse(startTime);
					Date endTime1 = dfs.parse(endTime);
					
					Special special = new Special();
					special.collectTime = collectTime1;
					special.startTime = startTime1;
					special.endTime = endTime1;
					String date = dfs1.format(startTime1);
					List<Integer> specialWeatherResult = new ArrayList<>();
					specialWeatherMap(weatherInfo, specialWeatherResult);
					special.weatherInfo = specialWeatherResult;

					if (!routeWeather.containsKey(date))
					{
						routeWeather.put(date, new HashMap<>());
					}
					Map<String, List<Special>> routeWeatherPort = routeWeather.get(date);
					if (!routeWeatherPort.containsKey(airport))
					{
						routeWeatherPort.put(airport, new ArrayList<>());
					}
					routeWeatherPort.get(airport).add(special);
					//special
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
		flightInfos.clear();
		planeInfos.clear();
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
				String airports = fdInfo.startPort + fdInfo.landPort;
				String flightCompany = fdInfo.flightNo.substring(0, 2);
				
				SimpleDateFormat dfs1 = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dfs3 = new SimpleDateFormat("MM");
				SimpleDateFormat dfs2 = new SimpleDateFormat("dd");
				SimpleDateFormat dfs4 = new SimpleDateFormat("HH");
				
				if (!startTimePlan.isEmpty())
				{
					fdInfo.startTimePlan = new Long(startTimePlan);
					String date = dfs1.format(new Date(fdInfo.startTimePlan * 1000));
					Integer month = Integer.parseInt(dfs3.format(new Date(fdInfo.startTimePlan * 1000)));
					Integer hour = Integer.parseInt(dfs4.format(new Date(fdInfo.startTimePlan * 1000)));
					int season = (month - 1) / 3;
					int period = hour / 8;
					
					if (!throughputInfos.containsKey(fdInfo.startPort))
					{
						throughputInfos.put(fdInfo.startPort, new HashMap<>());
						for (int i = 0; i < 4; i++)
						{
							throughputInfos.get(fdInfo.startPort).put(i, new HashMap<>());
							for (int j = 0; j < 3; j++)
							{
								throughputInfos.get(fdInfo.startPort).get(i).put(j, new ThroughputInfo());
							}
						}
					}
					
					if (!throughputInfos.containsKey(fdInfo.landPort))
					{
						throughputInfos.put(fdInfo.landPort, new HashMap<>());
						for (int i = 0; i < 4; i++)
						{
							throughputInfos.get(fdInfo.landPort).put(i, new HashMap<>());
							for (int j = 0; j < 3; j++)
							{
								throughputInfos.get(fdInfo.landPort).get(i).put(j, new ThroughputInfo());
							}
						}
					}
					
					if (!flightComInfos.containsKey(flightCompany))
					{
						flightComInfos.put(flightCompany, new HashMap<>());
						for (int i = 0; i < 4; i++)
						{
							flightComInfos.get(flightCompany).put(i, new HashMap<>());
							for (int j = 0; j < 3; j++)
							{
								flightComInfos.get(flightCompany).get(i).put(j, new ThroughputInfo());
							}
						}
					}
					
					if (!routeInfos.containsKey(airports))
					{
						routeInfos.put(airports, new HashMap<>());
						for (int i = 0; i < 4; i++)
						{
							routeInfos.get(airports).put(i, new HashMap<>());
							for (int j = 0; j < 3; j++)
							{
								routeInfos.get(airports).get(i).put(j, new ThroughputInfo());
							}
						}
					}

					if (!startTimeReal.isEmpty())
					{
						fdInfo.startTimeReal = new Long(startTimeReal);
						long diff = fdInfo.startTimeReal - fdInfo.startTimePlan;
						long diffStd = 3 * 60 * 60;
						if (diff >= diffStd)
						{
							fdInfo.ifDelay = 1;
						}
						
						if (fdInfo.startTimeReal > fdInfo.startTimePlan)
						{
							long duration = fdInfo.startTimeReal - fdInfo.startTimePlan;
							throughputInfos.get(fdInfo.startPort).get(season).get(period).addDuration(duration);
							throughputInfos.get(fdInfo.landPort).get(season).get(period).addDuration(duration);
							flightComInfos.get(flightCompany).get(season).get(period).addDuration(duration);
							routeInfos.get(airports).get(season).get(period).addDuration(duration);
						}
						
						Date startReal = new Date(fdInfo.startTimeReal * 1000);
						int startHour = Integer.parseInt(dfs4.format(startReal));
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
					}

					if (!landTimePlan.isEmpty())
					{
						fdInfo.landTimePlan = new Long(landTimePlan);
						
						if (!landTimeReal.isEmpty()) {
							fdInfo.landTimeReal = new Long(landTimeReal);
							Date landReal = new Date(fdInfo.landTimeReal * 1000);
							int landHour = Integer.parseInt(dfs4.format(landReal));
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
					}
					
					throughputInfos.get(fdInfo.startPort).get(season).get(period).outCnt(fdInfo);
					throughputInfos.get(fdInfo.landPort).get(season).get(period).inCnt(fdInfo);
					flightComInfos.get(flightCompany).get(season).get(period).inCnt(fdInfo);
					routeInfos.get(airports).get(season).get(period).inCnt(fdInfo);
					
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
					//0825
					String weather = weatherMap.get(date).get(cityMap.get(fdInfo.startPort));
					if (!weatherCount.containsKey(weather)) {
						weatherCount.put(weather, new WeatherRate());
					}
					weatherCount.get(weather).statistics(fdInfo);
					//0825
				}
			}
			fileInfo.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void processData()
	{
		for (Entry<String, Map<Integer, Map<Integer, ThroughputInfo>>> throughputInfo : throughputInfos.entrySet())
		{
			for (Entry<Integer, Map<Integer, ThroughputInfo>> throughputIn : throughputInfo.getValue().entrySet())
			{
				for (Entry<Integer, ThroughputInfo> throughput : throughputIn.getValue().entrySet())
				{
					if (throughput.getValue().inTimes > 10000)
					{
						DelayInfo delayInfo = new DelayInfo();
						delayInfo.rate = (float)throughput.getValue().outDelay / (float)throughput.getValue().inTimes;
						delayInfo.delayDur = (float)throughput.getValue().delayDuration / (float)throughput.getValue().inTimes;
						if (!landDelayRate.containsKey(throughputInfo.getKey()))
						{
							landDelayRate.put(throughputInfo.getKey(), new HashMap<>());
						}
						if (!landDelayRate.get(throughputInfo.getKey()).containsKey(throughputIn.getKey()))
						{
							landDelayRate.get(throughputInfo.getKey()).put(throughputIn.getKey(), new HashMap<>());
						}
						landDelayRate.get(throughputInfo.getKey()).get(throughputIn.getKey()).put(throughputIn.getKey(), delayInfo);
					}
					if (throughput.getValue().outTimes > 10000)
					{
						DelayInfo delayInfo = new DelayInfo();
						delayInfo.rate = (float)throughput.getValue().outDelay / (float)throughput.getValue().outTimes;
						delayInfo.delayDur = (float)throughput.getValue().delayDuration / (float)throughput.getValue().outTimes;
						if (!startDelayRate.containsKey(throughputInfo.getKey()))
						{
							startDelayRate.put(throughputInfo.getKey(), new HashMap<>());
						}
						if (!startDelayRate.get(throughputInfo.getKey()).containsKey(throughputIn.getKey()))
						{
							startDelayRate.get(throughputInfo.getKey()).put(throughputIn.getKey(), new HashMap<>());
						}
						startDelayRate.get(throughputInfo.getKey()).get(throughputIn.getKey()).put(throughput.getKey(), delayInfo);
					}
				}
			}
		}
		
		for (Entry<String, Map<Integer, Map<Integer, ThroughputInfo>>> flightCompany : flightComInfos.entrySet())
		{
			for (Entry<Integer, Map<Integer, ThroughputInfo>> flightCom : flightCompany.getValue().entrySet())
			{
				for (Entry<Integer, ThroughputInfo> flight : flightCom.getValue().entrySet())
				{
					if (flight.getValue().inTimes > 10000)
					{
						DelayInfo delayInfo = new DelayInfo();
						delayInfo.delayDur = (float)flight.getValue().delayDuration / (float)flight.getValue().inTimes;
						delayInfo.rate = (float)flight.getValue().inDelay / (float)flight.getValue().inTimes;
						if (!flightDelayRate.containsKey(flightCompany.getKey()))
						{
							flightDelayRate.put(flightCompany.getKey(), new HashMap<>());
						}
						if (!flightDelayRate.get(flightCompany.getKey()).containsKey(flightCom.getKey()))
						{
							flightDelayRate.get(flightCompany.getKey()).put(flightCom.getKey(), new HashMap<>());
						}
						flightDelayRate.get(flightCompany.getKey()).get(flightCom.getKey()).put(flight.getKey(), delayInfo);
					}
				}
			}
		}
		
		for (Entry<String, Map<Integer, Map<Integer, ThroughputInfo>>> routeInfo : routeInfos.entrySet())
		{
			for (Entry<Integer, Map<Integer, ThroughputInfo>> routeIn : routeInfo.getValue().entrySet())
			{
				for (Entry<Integer, ThroughputInfo> route : routeIn.getValue().entrySet())
				{
					if (route.getValue().inTimes > 1000)
					{
						DelayInfo delayInfo = new DelayInfo();
						delayInfo.delayDur = (float)route.getValue().delayDuration / (float)route.getValue().inTimes;
						delayInfo.rate = (float)route.getValue().inDelay / (float)route.getValue().inTimes;
						if (!routeDelayRate.containsKey(routeInfo.getKey()))
						{
							routeDelayRate.put(routeInfo.getKey(), new HashMap<>());
						}
						if (!routeDelayRate.get(routeInfo.getKey()).containsKey(routeIn.getKey()))
						{
							routeDelayRate.get(routeInfo.getKey()).put(routeIn.getKey(), new HashMap<>());
						}
						routeDelayRate.get(routeInfo.getKey()).get(routeIn.getKey()).put(route.getKey(), delayInfo);
					}
				}
			}
		}
		//0825
		for (Entry<String, WeatherRate> weather : weatherCount.entrySet()) {
			if (weather.getValue().count > 10) {
				float rate = (float)weather.getValue().delay / (float)weather.getValue().count;
				weatherRate.put(weather.getKey(), rate);
			}
		}
		//0825
	}
	
	public static void printRouteDelay(int index, FdInfo fdInfoPre1, long nowTime, BufferedWriter outputBuffer) throws IOException
	{
		if (fdInfoPre1.startTimePlan <= nowTime)
		{
			int isCannel = fdInfoPre1.startTimeReal == null ? 1 : 0;
			if (fdInfoPre1.startTimePlan + 3 * 60 * 60 <= nowTime || isCannel == 1 || fdInfoPre1.startTimeReal <= nowTime)
			{
				outputBuffer.write(index + ":" + fdInfoPre1.ifDelay + " ");
			}
			index++;
			outputBuffer.write(index + ":" + isCannel + " ");
			index++;
			if (isCannel == 0)
			{
				if (fdInfoPre1.startTimeReal < nowTime)
				{
					outputBuffer.write(index + ":" + (fdInfoPre1.startTimeReal - fdInfoPre1.startTimePlan) + " ");
				} else {
					outputBuffer.write(index + ":" + (nowTime - fdInfoPre1.startTimePlan) + " ");
				}
			}
		}
	}

	public static void printData(String fileName) throws IOException
	{
		FileWriter outputFile = new FileWriter(fileName);
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
					if (startCity != null)
					{
						List<MaInfo> startWeather = cityWeather.get(date).get(startCity);
						if (startWeather != null)
						{
							for (MaInfo i : startWeather)
							{
								if (i.morning != -999)
								{
									outputBuffer.write(index + ":" + i.morning + " ");
									// System.out.print(index + ":" + i + " ");
								}
								index++;
								if (i.afternoon != -999)
								{
									outputBuffer.write(index + ":" + i.afternoon + " ");
								}
								index++;
							}
							//0825
							String weatherInfo = weatherMap.get(date).get(startCity);
							if (weatherRate.containsKey(weatherInfo)) {
								outputBuffer.write(index + ":" + weatherRate.get(weatherInfo) + " ");
								index++;
							}
							//0825
						}
					}
					
					index = 51;
					if (landCity != null)
					{
						List<MaInfo> landWeather = cityWeather.get(date).get(landCity);
						if (landWeather != null)
						{
							for (MaInfo i : landWeather)
							{
								if (i.morning != -999)
								{
									outputBuffer.write(index + ":" + i.morning + " ");
									// System.out.print(index + ":" + i + " ");
								}
								index++;
								if (i.afternoon != -999)
								{
									outputBuffer.write(index + ":" + i.afternoon + " ");
								}
								index++;
							}
							//0825
							String weatherInfo = weatherMap.get(date).get(landCity);
							if (weatherRate.containsKey(weatherInfo)) {
								outputBuffer.write(index + ":" + weatherRate.get(weatherInfo) + " ");
								index++;
							}
							//0825
						}
					}

					index = 101;
					long affectedStart = 0, affectedEnd = 0;
					if (routeWeather.containsKey(date))
					{
						Map<String, List<Special>> routeWeatherPort = routeWeather.get(date);
						if (routeWeatherPort.containsKey(fdInfo.startPort))
						{
							List<Special> specialTime = routeWeatherPort.get(fdInfo.startPort);
							boolean flag = false;
							for (Special special : specialTime) {
								long startTime = special.startTime.getTime() / 1000;
								long endTime = special.endTime.getTime() / 1000;
								long collectTime = special.collectTime.getTime() / 1000;
								if (fdInfo.startTimePlan > startTime
										&& fdInfo.startTimePlan < endTime && fdInfo.startTimePlan - collectTime > 2 * 60 * 60)
								{
									flag = true;
									outputBuffer.write(index + ":" + 1 + " ");
									index++;
									affectedStart = fdInfo.startTimePlan - startTime;
									affectedEnd = endTime - fdInfo.startTimePlan;
									outputBuffer.write(index + ":" + affectedStart + " ");
									index++;
									outputBuffer.write(index + ":" + affectedEnd + " ");
									index++;
									for (Integer i : special.weatherInfo) {
										if (i != null) {
											outputBuffer.write(index + ":" + i + " ");
										}
										index++;
									}
									break;
								} 
							}
							if (!flag) {
								outputBuffer.write(index + ":" + 0 + " ");
							}
						} else {
							outputBuffer.write(index + ":" + 0 + " ");
						}
						
						index = 126;						
						if (routeWeatherPort.containsKey(fdInfo.landPort) && fdInfo.landTimePlan != null)
						{
							boolean flag = false;
							List<Special> specialTime = routeWeatherPort.get(fdInfo.landPort);
							for (Special special : specialTime) {
								long startTime = special.startTime.getTime() / 1000;
								long endTime = special.endTime.getTime() / 1000;
								long collectTime = special.collectTime.getTime() / 1000;
								if (fdInfo.landTimePlan > startTime
										&& fdInfo.landTimePlan < endTime && fdInfo.startTimePlan - collectTime > 2 * 60 * 60)
								{
									flag = true;
									outputBuffer.write(index + ":" + 1 + " ");
									index++;
									affectedStart = fdInfo.landTimePlan - startTime;
									affectedEnd = endTime - fdInfo.landTimePlan;
									outputBuffer.write(index + ":" + affectedStart + " ");
									index++;
									outputBuffer.write(index + ":" + affectedEnd + " ");
									index++;
									for (Integer i : special.weatherInfo) {
										if (i != null) {
											outputBuffer.write(index + ":" + i + " ");
										}
										index++;
									}
									break;
									//</special>
								}
							}
							if (!flag) {
								outputBuffer.write(index + ":" + 0 + " ");
							}
						} else {
							outputBuffer.write(index + ":" + 0 + " ");
						}
					} else {
						outputBuffer.write(index + ":" + 0 + " ");
						index = 126;
						outputBuffer.write(index + ":" + 0 + " ");
					}

					Long nowTime = fdInfo.startTimePlan - 2 * 60 * 60;
					
					index = 151;
					int indexFlight = flightDateEntry.getValue().indexOf(fdInfo);
					outputBuffer.write(index + ":" + indexFlight + " ");
					index++;
					for (int i = 1; i < 5; i++)
					{
						if (indexFlight - i >= 0)
						{
							FdInfo fdInfoPre = flightDateEntry.getValue().get(indexFlight -i);
							printRouteDelay(index, fdInfoPre, nowTime, outputBuffer);
							index += 3;
						} else
						{
							break;
						}
					}
					
					index = 170;
					if (!fdInfo.planeNo.equals(""))
					{
						List<FdInfo> planeNoInfos = planeInfos.get(date).get(fdInfo.planeNo);
						int planeIndex = planeNoInfos.indexOf(fdInfo);
						outputBuffer.write(index + ":" + planeIndex + " ");
						index++;
//						outputBuffer.write("\n");
//						for (FdInfo fdInfo2 : planeNoInfos)
//						{
//							outputBuffer.write(fdInfo2.flightNo + "-" + fdInfo2.startTimePlan + "\t");
//						}
//						outputBuffer.write("\n");
						for (int i = 1; i < 5; i++)
						{
							if (planeIndex - i >= 0)
							{
								FdInfo fdInfoPre = planeNoInfos.get(planeIndex - i);
								printRouteDelay(index, fdInfoPre, nowTime, outputBuffer);
								index += 3;
							} else
							{
								break;
							}
						}
					}
					
					index = 190;
					SimpleDateFormat dfs1 = new SimpleDateFormat("HHmm");
					outputBuffer.write(index + ":" + dfs1.format(new Date(fdInfo.startTimePlan * 1000)) + " ");
					index++;
					outputBuffer.write(index + ":" + dfs1.format(new Date(fdInfo.landTimePlan * 1000)) + " ");
					index++;
					long flightTime = fdInfo.landTimePlan - fdInfo.startTimePlan;
					outputBuffer.write(index + ":" + flightTime + " ");
					
					index = 193;
					SimpleDateFormat dfs2 = new SimpleDateFormat("MM");
					SimpleDateFormat dfs4 = new SimpleDateFormat("HH");
					Integer month = Integer.parseInt(dfs2.format(new Date(fdInfo.startTimePlan * 1000)));
					Integer hour = Integer.parseInt(dfs4.format(new Date(fdInfo.startTimePlan * 1000)));
					int season = (month - 1) / 3;
					int period = hour / 8;
					if (startDelayRate.containsKey(fdInfo.startPort))
					{
						if (startDelayRate.get(fdInfo.startPort).containsKey(season))
						{
							if (startDelayRate.get(fdInfo.startPort).get(season).containsKey(period))
							{
								outputBuffer.write(index + ":" + startDelayRate.get(fdInfo.startPort).get(season).get(period).rate + " ");
								index++;
								outputBuffer.write(index + ":" + startDelayRate.get(fdInfo.startPort).get(season).get(period).delayDur + " ");
							}
						}
					}
					
					index = 195;
					if (landDelayRate.containsKey(fdInfo.landPort))
					{
						if (landDelayRate.get(fdInfo.landPort).containsKey(season))
						{
							if (landDelayRate.get(fdInfo.landPort).get(season).containsKey(period))
							{
								outputBuffer.write(index + ":" + landDelayRate.get(fdInfo.landPort).get(season).get(period).rate + " ");
								index++;
								outputBuffer.write(index + ":" + landDelayRate.get(fdInfo.landPort).get(season).get(period).delayDur + " ");
							}
							
						}
					}

					index = 197;
					String company = fdInfo.flightNo.substring(0, 2);
					if (flightDelayRate.containsKey(company))
					{
						if (flightDelayRate.get(company).containsKey(season))
						{
							if (flightDelayRate.get(company).get(season).containsKey(period))
							{
								outputBuffer.write(index + ":" + flightDelayRate.get(company).get(season).get(period).rate + " ");
								index++;
								outputBuffer.write(index + ":" + flightDelayRate.get(company).get(season).get(period).delayDur + " ");
							}
							
						}
					}
					
					index = 199;
					String route = fdInfo.startPort + fdInfo.landPort;
					if (routeDelayRate.containsKey(route))
					{
						if (routeDelayRate.get(route).containsKey(season))
						{
							if (routeDelayRate.get(route).get(season).containsKey(period))
							{
								outputBuffer.write(index + ":" + routeDelayRate.get(route).get(season).get(period).rate + " ");
								index++;
								outputBuffer.write(index + ":" + routeDelayRate.get(route).get(season).get(period).delayDur + " ");
							}
						}
					}
//					
					index = 210;
					SimpleDateFormat dfs3 = new SimpleDateFormat("dd");
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
//					
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
		String predCityWeather = args[4];
		String predRouteWeather = args[5];
		String predFile = args[6];

		initData();
		loadCityMap(cityFile);
		
		cityWeather(cityWeatherFile);
		System.out.println("load fd cityWeather Done");
		loadRouteWeather(routeWeatherFile);
		System.out.println("load fd routeWeather Done");
		loadFd(fdFile);
		System.out.println("load fdFile Done");
		processData();
		printData("test.txt.fd");
		System.out.println("fdInfo Processed");
		
		cityWeather(predCityWeather);
		System.out.println("load pred cityWeather Done");
		loadRouteWeather(predRouteWeather);
		System.out.println("load pred routeWeather Done");
		loadFd(predFile);
		printData("test.txt.pred");
		System.out.println("PredInfo Precessed");
	}
}
