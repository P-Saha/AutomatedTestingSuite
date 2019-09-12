package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;

public class KPITest extends OperationTest {

	protected KPITest(HashMap<String, String> test, HashMap<String, String> testData) {
		super(test, testData);
	}

	private static long[] getOffsetTime(LocalDateTime prev, LocalDateTime last) { // From
																					// https://stackoverflow.com/questions/25747499/java-8-calculate-difference-between-two-localdatetime
		LocalDateTime temp = LocalDateTime.of(last.getYear(), last.getMonthValue(), last.getDayOfMonth(),
				prev.getHour(), prev.getMinute(), prev.getSecond());
		Duration duration = Duration.between(temp, last);
		long seconds = duration.getSeconds();
		long hours = seconds / (60 * 60);
		long minutes = ((seconds % (60 * 60)) / 60);
		long secs = (seconds % 60);
		return new long[] { hours, minutes, secs };
	}

	public static void kpiFileEdit() throws IOException {
		BufferedReader in = null;
		BufferedWriter out = null;
		String tempLine = null;
		LocalDateTime lastDateTime = null;
		LocalDateTime prevDateTime = null;
		LocalDateTime curDateTime = LocalDateTime.now();
		LocalDateTime tempDateTime = null;
		try {
			in = new BufferedReader(new FileReader("input.log"));
			while (((tempLine = in.readLine()) != null)) {
				if (tempLine.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
					lastDateTime = LocalDateTime.parse(tempLine.split(" ")[0]);
				}
			}
			if (in != null) {
				in.close();
			}
			
			in = new BufferedReader(new FileReader("input.log"));
			out = new BufferedWriter(new FileWriter("output.log"));
			tempLine = null;
			while (((tempLine = in.readLine()) != null)) {
				tempLine = tempLine.replace("﻿","");
				if (tempLine.trim().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
					prevDateTime = LocalDateTime.parse(tempLine.split(" ")[0]);
					Period offsetDate = Period.between(prevDateTime.toLocalDate(), lastDateTime.toLocalDate());
					long offsetTime[] = getOffsetTime(prevDateTime, lastDateTime);
					tempDateTime = curDateTime.minus(offsetDate).minusHours(offsetTime[0]).minusMinutes(offsetTime[1])
							.minusSeconds(offsetTime[2]);
					tempDateTime = tempDateTime.minusNanos(tempDateTime.getNano());
					System.out.println(prevDateTime.toString());
					System.out.println(tempDateTime.toString());

					tempLine = tempLine.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}",
							((tempDateTime.getSecond() == 0) ? 
									(tempDateTime.toString() + ":00")
									: (tempDateTime.toString())));
					
					out.write(tempLine, 0, tempLine.length());
					out.newLine();
				} else {
					System.out.println(tempLine);
					out.write(tempLine, 0, tempLine.length());
					out.newLine();
				}
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
}
