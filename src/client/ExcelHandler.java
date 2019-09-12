package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.InvalidArgumentException;

//  This class handles excel workbook reading and writing
public class ExcelHandler {

//  getTestData() parses through an excel sheet (SQLiteData.xlsx) to get test data, which is important info for all tests.
//  Requires: excel sheet must exist in the specific location with specific sheets and layout.
	public static HashMap<String, String> getTestData() throws IOException {
		String path = new File("").getAbsolutePath();
		File file = new File(path + "/Tests/TestData.xlsx");
		FileInputStream fileInput;
		Workbook workbook;
		HashMap<String, String> TestDataHashMap = new HashMap<String, String>();
		fileInput = new FileInputStream(file);
		workbook = new XSSFWorkbook(fileInput);
		Sheet sheet = workbook.getSheet("Test Data");
		for (Row row : sheet) {
			String keyStringNoSpaces = row.getCell(0).toString().toLowerCase().replaceAll("\\s*", "");
			String value = row.getCell(1).toString();

			TestDataHashMap.put(keyStringNoSpaces, value);
		}
		workbook.close();
		return TestDataHashMap;
	}

//  These functions parse excel sheets and turn them into arraylists of the associated objects.
//  Requires: excel sheets must exist in a specific location with specific layout and sheet names.
	public static ArrayList<DataCollectionTest> getDataCollectionTests(HashMap<String, String> testDataHash)
			throws IOException {
		ArrayList<DataCollectionTest> allTestData = new ArrayList<DataCollectionTest>();
		ArrayList<HashMap<String, String>> operationTestCases = getOperationTestCases(testDataHash,
				"DataCollection Tests");
		for (HashMap<String, String> map : operationTestCases) {
			allTestData.add(new DataCollectionTest(map, testDataHash));
		}
		return allTestData;
	}

	public static ArrayList<DataPushTest> getDataPushTests(HashMap<String, String> testDataHash) throws IOException {
		ArrayList<DataPushTest> allTestData = new ArrayList<DataPushTest>();
		ArrayList<HashMap<String, String>> operationTestCases = getOperationTestCases(testDataHash, "DataPush Tests");
		for (HashMap<String, String> map : operationTestCases) {
			allTestData.add(new DataPushTest(map, testDataHash));
		}
		return allTestData;
	}

	private static ArrayList<HashMap<String, String>> getOperationTestCases(HashMap<String, String> testDataHash,
			String type) throws IOException {
		FileInputStream fileInput;
		Workbook workbook;
		String path = new File("").getAbsolutePath();
		File file = null;
		if (type.equals("DataCollection Tests")) {
			file = new File(path + "/Tests/DataCollectionTests.xlsx");
		} else if (type.equals("DataPush Tests")) {
			file = new File(path + "/Tests/DataPushTests.xlsx");
		} else if (type.equals("Purge Tests")) {
			file = new File(path + "/Tests/PurgeTests.xlsx");
		} else {
			System.out.println("Error in getOperationTests()");
			throw new InvalidArgumentException("Error in getOperationTests()");
		}
		ArrayList<HashMap<String, String>> operationTestCases = new ArrayList<HashMap<String, String>>();
		ArrayList<String> columnTitles = new ArrayList<String>();
		// Excel setup
		fileInput = new FileInputStream(file);
		workbook = new XSSFWorkbook(fileInput);
		Sheet sheet = workbook.getSheet(type);
		// Parsing into array list of hash tables
		for (Cell titleCell : sheet.getRow(0)) {
			columnTitles.add(titleCell.toString().replaceAll("\\s*", "").toLowerCase());
		}
		Row operationTestRow = null;
		Cell operationTestCell = null;
		HashMap<String, String> testCase;
		for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {
			operationTestRow = sheet.getRow(i);
			testCase = new HashMap<String, String>();
			for (int j = 0; j < operationTestRow.getLastCellNum(); j++) {
				operationTestCell = operationTestRow.getCell(j);
				if (operationTestCell != null) {
					testCase.put(columnTitles.get(j), operationTestCell.toString());
				}
			}
			operationTestCases.add(testCase);
		}
		workbook.close();
		fileInput.close();
		return operationTestCases;
	}

//  getSQLiteCompareTests(HashMap<String, String> testDataHash) Parses each row of the SQLiteCompare Tests tab of the excel sheet, 
//  generating and returning an arraylist of SQLiteCompareTests.
//  Requires: excel sheet must exist in the specific location with specific sheets and layout.
	public static ArrayList<SQLiteCompareTest> getSQLiteCompareTests(HashMap<String, String> testData)
			throws IOException {
		ArrayList<SQLiteCompareTest> DBTests = new ArrayList<SQLiteCompareTest>();
		String path = new File("").getAbsolutePath();
		File file = new File(path + "/Tests/SQLiteCompareTests.xlsx");
		FileInputStream fileInput;
		Workbook workbook;
		Row DBTestRow;
		String[] testCaseData = new String[2];
		// Excel setup
		fileInput = new FileInputStream(file);
		workbook = new XSSFWorkbook(fileInput);
		Sheet sheet = workbook.getSheet("SQLite Compare Tests");
		for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {
			DBTestRow = sheet.getRow(i);
			for (int j = 0; j < DBTestRow.getLastCellNum(); j++) {
				testCaseData[j] = DBTestRow.getCell(j).toString();
			}
			DBTests.add(new SQLiteCompareTest(testCaseData[0], testCaseData[1], testData));
		}
		workbook.close();
		fileInput.close();
		return DBTests;
	}

//  SQLiteCompareTestResultsToExcel(ArrayList<SQLiteCompareTest> SQLiteCompareTests) takes an array of SQLiteCompareTests
//	(should be after they are run) and outputs the results onto a "SQLite Compare Test Results" sheet of SQLiteData.xlsx 
//	(the excel sheet where the tests are generated from).
//  Requires: excel sheet must exist in the specific location.
	public static void SQLiteCompareTestResultsToExcel(ArrayList<SQLiteCompareTest> SQLiteCompareTests)
			throws IOException {
		Workbook workbook = null;
		String path = new File("").getAbsolutePath();
		File file = new File(path + "/Tests/SQLiteCompareTests.xlsx");
		FileInputStream inputStream = new FileInputStream(file);
		workbook = WorkbookFactory.create(inputStream);
		Sheet sheet = workbook.getSheet("SQLite Compare Test Results");
		if (sheet == null) {
			sheet = workbook.createSheet("SQLite Compare Test Results");
		}
		String columns[] = { "Test Case #", "Scenario Name", "Execution Date", "Start Time", "End Time", "Result" };
		int rownum = 0;
		int cellnum = 0;
		Row row;
		Cell cell;
		CellStyle titleStyle = workbook.createCellStyle();
		CellStyle passStyle = workbook.createCellStyle();
		CellStyle failStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		titleStyle.setFont(font);
		passStyle.setFont(font);
		failStyle.setFont(font);
		passStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
		failStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
		passStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		row = sheet.createRow(rownum++);

		for (int i = 0; i < columns.length; i++) {
			cell = row.createCell(cellnum++);
			cell.setCellValue(columns[i]);
			cell.setCellStyle(titleStyle);
			sheet.autoSizeColumn(i);
		}

		for (SQLiteCompareTest test : SQLiteCompareTests) {
			row = sheet.createRow(rownum++);
			cellnum = 0;
			row.createCell(cellnum++).setCellValue(test.getTestCaseNumber());
			row.createCell(cellnum++).setCellValue(test.getScenarioName());
			row.createCell(cellnum++).setCellValue(test.getStartTime().toString());
			row.createCell(cellnum++).setCellValue(test.getEndTime().toString());
			cell = row.createCell(cellnum++);
			cell.setCellValue(test.getPass() ? "PASS" : "FAIL");
			cell.setCellStyle(test.getPass() ? passStyle : failStyle);
		}

		for (int i = 0; i < columns.length; i++) {
			sheet.autoSizeColumn(i);
		}

		FileOutputStream out = new FileOutputStream(file);
		workbook.write(out);
		workbook.close();
		out.close();
	}

//  operationTestResultsToExcel(ArrayList<OperationTest> clientTests) takes an array of OperationTests
//	(should be after they are run) and outputs the results onto a "Operation Test Results" sheet of SQLiteData.xlsx 
//	(the excel sheet where the tests are generated from).
//  Requires: excel sheet must exist in the specific location.
	public static void operationTestResultsToExcel(ArrayList<? extends OperationTest> operationTests, String type)
			throws IOException {
		Workbook workbook = null;
		String path = new File("").getAbsolutePath();
		File file = null;
		if (type.equals("Log Test")) {
			file = new File(path + "//Tests//LogTests.xlsx");
		} else if (type.equals("DataPush Test")) {
			file = new File(path + "//Tests//DataPushTests.xlsx");
		} else if (type.equals("Purge Test")) {
			file = new File(path + "//Tests//PurgeTests.xlsx");
		} else {
			System.out.println("Error in getOperationTests()");
		}
		FileInputStream inputStream = new FileInputStream(file);
		workbook = WorkbookFactory.create(inputStream);
		Sheet sheet = workbook.getSheet(type + " Results");
		if (sheet == null) {
			sheet = workbook.createSheet(type + " Results");
		}

		String columns[] = { "Test Case #", "Action Type", "Table", "Execution Date", "Start Time", "End Time",
				"Result" };
		int rownum = 0;
		int cellnum = 0;
		Row row;
		Cell cell;
		CellStyle titleStyle = workbook.createCellStyle();
		CellStyle passStyle = workbook.createCellStyle();
		CellStyle failStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		titleStyle.setFont(font);
		passStyle.setFont(font);
		failStyle.setFont(font);
		passStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
		failStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
		passStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		row = sheet.createRow(rownum++);

		for (int i = 0; i < columns.length; i++) {
			cell = row.createCell(cellnum++);
			cell.setCellValue(columns[i]);
			cell.setCellStyle(titleStyle);
		}

		for (OperationTest test : operationTests) {
			row = sheet.createRow(rownum++);
			cellnum = 0;
			row.createCell(cellnum++).setCellValue(test.getTestCaseNumber());
			row.createCell(cellnum++).setCellValue(test.getActionType());
			row.createCell(cellnum++).setCellValue(test.getTableName());
			row.createCell(cellnum++).setCellValue(test.getStartTime().toString());
			row.createCell(cellnum++).setCellValue(test.getEndTime().toString());
			cell = row.createCell(cellnum++);
			cell.setCellValue(test.getPass() ? "PASS" : "FAIL");
			cell.setCellStyle(test.getPass() ? passStyle : failStyle);
		}

		for (int i = 0; i < columns.length; i++) {
			sheet.autoSizeColumn(i);
		}

		FileOutputStream out = new FileOutputStream(file);
		workbook.write(out);
		workbook.close();
		out.close();
	}
}