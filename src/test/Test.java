package test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;

public class Test {

	public static void main(String[] args) throws ParseException {
		// TODO Auto-generated method stub
//		SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		SimpleDateFormat dfs1 = new SimpleDateFormat("yyyy-MM-dd HH");
//		Date startTime1 = dfs.parse("1453773960");
//		String date = dfs1.format(startTime1);
//		System.out.println(date);
		
		SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String startTime = "2016-06-22 16:00:00Z";
		Date startTime1 = dfs.parse(startTime);
		System.out.println(startTime1.getTime() / 1000);
		
		Long startTimePlan = new Long("1453091700");
		Long landTimePlan = new Long("1453104900");
		long diff = landTimePlan - startTimePlan;
		System.out.println(diff);
		
		String special = "通行能力下降20 %左右,目前,08时-17时低能见度800米,10时-11时云低约30-60米,，期间进出港航班可能受到影响,";
		String neng = "能见度.{0,5}\\d+";
		String tong = "通行能力下降.{0,5}\\d+ %";
		String yun = "云.{0,5}\\d+.米";
		String num = "\\d+";
		// 创建 Pattern 对象
	    Pattern r1 = Pattern.compile((tong));
	    Matcher m1 = r1.matcher(special);
	    if (m1.find()) {
	    	System.out.println(m1.group(0));
		    Pattern r2 = Pattern.compile((num));
		    Matcher m2 = r2.matcher(m1.group(0));
		    if (m2.find()) {
				System.out.println(m2.group(0));
			}
		}
	    Long a = new Long(1453091700);
	    Long b = new Long(1453091700);
	    System.out.println(a <= b);
	}

}
