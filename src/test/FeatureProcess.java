package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class FeatureProcess {
	public static BufferedReader readFile(String filename)
	{
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf-8"));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return br;
	}
	public static void writeFile() throws IOException {

	}
	// 航班动态历史数据表
	public static class FdInfo{
		public String departureAirport = "";
		public String arrivalAirport = "";
		public String flightNo = "";
		public String plannedDepartureTime = "";
		public String plannedArrivalTime = "";
		public String actualDepartureTime = "";
		public String actualArrivalTime = "";
		public String aircraftNo = "";
		public int ifDelay = 0;
		public int ifCancelled = 0;
		public String getPlannedDepartureTime(){
			return plannedDepartureTime;
		}
	}
	public static Map<String, Map<String, List<FdInfo>>> flightHistoryDataMap= new HashMap<>(); // departureToarrival + date + FdInfo
	public static void fd(String filename) {
		try {
			BufferedReader br = readFile(filename);
			String line = null;
			while ((line = br.readLine()) != null)
			{
				try {
					String[] item = line.split(",");
					FdInfo fdInfo = new FdInfo();
					fdInfo.departureAirport = item[0];
					fdInfo.arrivalAirport = item[1];
					fdInfo.flightNo = item[2];
					fdInfo.plannedDepartureTime = item[3];
					fdInfo.plannedArrivalTime = item[4];
					fdInfo.actualDepartureTime = item[5];
					fdInfo.actualArrivalTime = item[6];
					fdInfo.aircraftNo = item[7];
					fdInfo.ifDelay = item[8].equals("取消") ? 1 : 0;
					if (!fdInfo.actualDepartureTime.isEmpty()) {
						int diffDeparture = Integer.parseInt(fdInfo.actualDepartureTime)-Integer.parseInt(fdInfo.plannedDepartureTime);
						if (diffDeparture >= 3 * 60 * 60) {
							fdInfo.ifDelay = 1;
						}
					}
					String departureAndArrival = fdInfo.departureAirport + fdInfo.arrivalAirport;
					if (!flightHistoryDataMap.containsKey(departureAndArrival)) {
						flightHistoryDataMap.put(departureAndArrival, new HashMap<>());
					}
					Map<String, List<FdInfo>> dateMap = flightHistoryDataMap.get(departureAndArrival);
					Date dateTime=new Date(Long.parseLong(fdInfo.plannedDepartureTime)); 
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  
				    String date = sdf.format(dateTime);  
					if (!dateMap.containsKey(date)) {
						dateMap.put(date, new ArrayList<>());
					}
					dateMap.get(date).add(fdInfo);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	public static void sortFdList() throws IOException {
        File outFile = new File("test.txt");
    	BufferedWriter outPutFile = new BufferedWriter(new FileWriter(outFile));
		for (Entry<String, Map<String, List<FdInfo>>> dateMap : flightHistoryDataMap.entrySet()) {
			for (Entry<String, List<FdInfo>> fdList : dateMap.getValue().entrySet()) {
				Collections.sort(fdList.getValue(),new Comparator<FdInfo>(){
		            public int compare(FdInfo arg0, FdInfo arg1) {
		                return arg0.getPlannedDepartureTime().compareTo(arg1.getPlannedDepartureTime());
		            }
		        });
		        try {
		        	// label
		        	for (FdInfo fdInfoEntry : fdList.getValue()) {
		            	outPutFile.write(fdInfoEntry.ifDelay + " ");
		            	outPutFile.write("\n");
					}
		        	
		        } catch (FileNotFoundException e) {
		            e.printStackTrace();
		        }
			}
		}
		outPutFile.close();
	}
	// 机场城市对应表
	public static class CityCode{
		public String airportCode = "";
		public String cityName = "";
	}
	public static List<CityCode> cityCodeList= new ArrayList<>();
	public static void cityTo3Code(String filename) {
		try {
			BufferedReader br = readFile(filename);
			String line = null;
			while ((line = br.readLine()) != null)
			{
				try {
					String[] item = line.split(",");
					CityCode cityCode = new CityCode();
					if (item.length==2) {
						cityCode.airportCode = item[0];
						cityCode.cityName = item[1];
						cityCodeList.add(cityCode);
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	// 城市天气表
	public static class CityWeather{
		public String cityName = "";
		public String weather = "";
		public String low = "";
		public String high = "";
		public String date = "";
	}
	public static List<CityWeather> cityWeatherList= new ArrayList<>();
	public static void cityToWeather(String filename) {
		try {
			BufferedReader br = readFile(filename);
			String line = null;
			while ((line = br.readLine()) != null)
			{
				try {
					String[] item = line.split(",");
					CityWeather cityWeather = new CityWeather();
					cityWeather.cityName = item[0];
					cityWeather.weather = item[1];
					cityWeather.low = item[2];
					cityWeather.high = item[3];
					cityWeather.date = item[4];
					cityWeatherList.add(cityWeather);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	// 机场特情表
	public static class AirportEmergency{
		public String specialAirport = "";
		public String collectionTime = "";
		public String startTime = "";
		public String endTime = "";
		public String specialContent = "";
	}
	public static List<AirportEmergency> airportEmergencyList= new ArrayList<>();
	public static void routeweather(String filename) {
		try {
			BufferedReader br = readFile(filename);
			String line = null;
			while ((line = br.readLine()) != null)
			{
				try {
					String[] item = line.split(",");
					AirportEmergency airportEmergency = new AirportEmergency();
					airportEmergency.specialAirport = item[0];
					airportEmergency.collectionTime = item[1];
					airportEmergency.startTime = item[2];
					airportEmergency.endTime = item[3];
					airportEmergency.specialContent = item[4];
					airportEmergencyList.add(airportEmergency);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	public static void main(String[] args) throws IOException, ParseException {
		cityTo3Code("cityTo3Code.csv");
		cityToWeather("201505-201705city_weather.csv");
		routeweather("201505-201705routeweather.csv");
		fd("201505-201705fd.csv");
		sortFdList();
	}

}
