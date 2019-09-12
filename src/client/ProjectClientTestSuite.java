package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class ProjectClientTestSuite {
	public static HashMap<String, String> testData;
	public static ArrayList<DataCollectionTest> dataCollectTests;
	public static ArrayList<SQLiteCompareTest> SQLiteCompareTests;
	public static ArrayList<DataPushTest> dataPushTests;
	public static ArrayList<DataCollectionTest> purgeTests;

	public static void adminCheck() throws IllegalAccessException {
		try {
			String line;
			String txt = "";
			Process p = Runtime.getRuntime().exec("net session");
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {
				txt += line;
			}
			input.close();
			if (txt.equals("")) {
				throw new IllegalAccessException("You must run as an administrator!");
			}
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	@BeforeAll
	public static void setUp() throws SQLException, IOException, IllegalAccessException {
		adminCheck();
		//KPITest.kpiFileEdit();
		testData = ExcelHandler.getTestData();
		System.out.println("SetUp() completed.");
	}

	@TestFactory
	Stream<DynamicTest> dataCollectionTestFactory() throws SQLException, IOException {
		dataCollectTests = ExcelHandler.getDataCollectionTests(testData);
		return dataCollectTests.stream().map(dataCollectTest -> DynamicTest.dynamicTest("DataCollection: Test Case " + dataCollectTest.getTestCaseNumber()
				+ ", " + dataCollectTest.getActionType() + ", " + dataCollectTest.getTableName() + ": ", () -> {
					dataCollectTest.runAction();
				}));
	}

//	@TestFactory
	Stream<DynamicTest> SQLiteCompareTestFactory() throws SQLException, IOException {
		SQLiteCompareTests = ExcelHandler.getSQLiteCompareTests(testData);
		return SQLiteCompareTests.stream().map(sqliteCompareTest -> DynamicTest.dynamicTest("SQLite Compare: Test Case "
				+ sqliteCompareTest.getTestCaseNumber() + ", " + sqliteCompareTest.getScenarioName(), () -> {
					sqliteCompareTest.runAction();
				}));
	}

	@TestFactory
	Stream<DynamicTest> dataPushTestFactory() throws SQLException, IOException {
		dataPushTests = ExcelHandler.getDataPushTests(testData);
		return dataPushTests.stream().map(dataPushTest -> DynamicTest.dynamicTest("DataPush: Test Case " + dataPushTest.getTestCaseNumber() + ", "
				+ dataPushTest.getActionType() + ", " + dataPushTest.getTableName() + ": ", () -> {
					dataPushTest.runAction();
				}));
	}

	@TestFactory
	Stream<DynamicTest> purgeTestFactory() throws SQLException, IOException {
		purgeTests = ExcelHandler.getLogTests(testData, "Purge Tests");
		return purgeTests.stream()
				.map(purgeTest -> DynamicTest.dynamicTest("Purge: Test Case " + purgeTest.getTestCaseNumber() + ", "
						+ purgeTest.getActionType() + ", " + purgeTest.getTableName() + ": ", () -> {
							purgeTest.runAction();
						}));
	}

	@AfterAll
	public static void tearDown() throws IOException {
		ExcelHandler.operationTestResultsToExcel(dataCollectTests, "Log Test");
		ExcelHandler.SQLiteCompareTestResultsToExcel(SQLiteCompareTests);
		ExcelHandler.operationTestResultsToExcel(dataPushTests, "DataPush Test");
  		ExcelHandler.operationTestResultsToExcel(purgeTests, "Purge Test");
		System.out.println("Test completed, connections closed.");
	}
}
