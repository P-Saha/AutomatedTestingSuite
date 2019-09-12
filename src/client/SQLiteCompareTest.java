package client;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import org.apache.poi.ss.usermodel.*;

public class SQLiteCompareTest extends TestBase {
	private final String scenarioName;
	private Connection expectedConnection;
	private Connection actualConnection;
	private Statement expectedStatement;
	private Statement actualStatement;
	// Numbers mean: tableNames[#] Connection#
	private static final String[][][] tableColumns =
			// Observation Table
			{ { { "ObservationId", "int" }, { "DataPointId", "string" }, { "CollectionTimestamp", "string" },
					{ "ObservationHash", "blob" } },
					// Observation Attribute Table
					{ { "ObservationId", "int" }, { "DataPointId", "string" }, { "CollectionTimestamp", "string" },
							{ "AttributeId", "int" }, { "AttributeValue", "string" } },
					// Observation Archive Table
					{ { "ObservationId", "int" }, { "DataPointId", "string" }, { "CollectionTimestamp", "string" },
							// { "StartArchiveCollectionTimestamp", "string"},
							{ "StartLine", "int" },
							// { "EndArchiveCollectionTimestamp", "string"},
							{ "EndLine", "int" } } };
	private boolean[] tableCountPass = { true, true, true };
	private static final String[] tableNames = { "Observation", "ObservationAttribute", "ObservationArchive" };
	private static final String[] countTestHeaders = { "Table Name", "Expected Count", "Actual Count" };
	private static final String[] dataTestHeaders = { "Table Name", "Row #", "Column", "Expected Result",
			"Expected CollectionTimestamp", "Actual Result", "Actual CollectionTimestamp" };

	private final String[] pathsToClean = { agentLogsDestinationPath, logDestinationPath, mcmLogFolderPath,
			agentProgramCachePath };
	private final String[] copyFrom = { sourcePath + "Agent Logs", sourcePath + "Logs", sourcePath + "DG",
			sourcePath + "RCP", sourcePath + "RCT" };
	private final String[] copyTo = { agentLogsDestinationPath, logDestinationPath, mcmLogFolderPath, mcmLogFolderPath,
			mcmLogFolderPath };
	private final String archiveDest;
	private final String[] pathsToArchive = { sourcePath + "Logs", agentProgramCachePath, projectClientLogPath };

	/**
	 * Constructor
	 * 
	 * @param testCaseNum
	 * @param scenarioname
	 * @param testData
	 */
	public SQLiteCompareTest(String testCaseNum, String scenarioname, HashMap<String, String> testData) {
		super(testData);
		scenarioName = scenarioname;
		testCaseNumber = testCaseNum;
		archiveDest = workingDirectory + "\\TestArchive\\SqLiteDbCompare - Test Case " + testCaseNumber + " "
				+ scenarioName + " " + startLocalDateTime;
		sourcePath = testData.get("logsourcefilepart1") + "\\" + scenarioName + "\\";
	}

	/**
	 * Running a test, running project, and then checking the produced data in the
	 * Observation, ObservationAttribute, and ObservationArchive tables, while also
	 * writing to Excel with results.
	 * 
	 * @throws SQLException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void runAction() throws SQLException, IOException, InterruptedException {
		startLocalDateTime = LocalDateTime.now();
		expectedConnection = connectSQLite(sourcePath + "\\SQLite Files\\project.sqlite");
		expectedStatement = expectedConnection.createStatement();
		performProjectAction(waitMinutesProjectClientDataCollection, pathsToClean, copyFrom, copyTo,
				archiveDest + " " + startLocalDateTime.toString().replaceAll(":","-"), pathsToArchive, false);

		actualConnection = connectSQLite(agentProgramCachePath + "\\project.sqlite");
		actualStatement = actualConnection.createStatement();

		ResultSet expectedResults;
		ResultSet actualResults;
		int expectedCount;
		int actualCount;
		boolean countPass = true;
		Workbook workbook = null;
		Row row;
		Cell cell;
		int rownum;
		int cellnum;
		String path = new File("").getAbsolutePath();
		File file = new File(path + "\\Tests\\SQLiteCompareTests.xlsx");
		FileInputStream inputStream = new FileInputStream(file);
		workbook = WorkbookFactory.create(inputStream);
		String sheetname = "SQLite Compare Failure Results";
		Sheet sheet = workbook.getSheet(sheetname);

		if (sheet != null) {
			rownum = sheet.getLastRowNum();
		} else {
			sheet = workbook.createSheet(sheetname);
			rownum = 0;
		}
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
		cellnum = 0;
		cell = row.createCell(cellnum++);
		cell.setCellValue("TEST NUMBER " + testCaseNumber + ": " + scenarioName);
		cell.setCellStyle(titleStyle);
		row = sheet.createRow(rownum++);
		cellnum = 0;
		try {
			// Count Test starts here:
			for (int i = 0; i < 3; i++) {
				cell = row.createCell(cellnum++);
				cell.setCellValue(countTestHeaders[i]);
				cell.setCellStyle(titleStyle);
			}

			for (int i = 0; i < 3; i++) {
				expectedCount = 0;
				actualCount = 0;

				expectedResults = expectedStatement.executeQuery("SELECT * FROM " + tableNames[i]);
				actualResults = actualStatement.executeQuery("SELECT * FROM " + tableNames[i]);
				while (expectedResults.next()) {
					expectedCount++;
				}
				while (actualResults.next()) {
					actualCount++;
				}
				row = sheet.createRow(rownum++);
				cellnum = 0;
				row.createCell(cellnum++).setCellValue(tableNames[i]);
				row.createCell(cellnum++).setCellValue(expectedCount);
				row.createCell(cellnum++).setCellValue(actualCount);
				if (expectedCount != actualCount) {
					tableCountPass[i] = false;
					countPass = false;
					pass = false;
				}
			}

			int counter;
			row = sheet.createRow(rownum++);
			cellnum = 0;
			cell = row.createCell(cellnum++);
			cell.setCellValue(countPass ? "COUNT TEST: PASS" : "COUNT TEST: FAIL");
			cell.setCellStyle(countPass ? passStyle : failStyle);
			// Count Test ends here and Data Test begins.
			sheet.createRow(rownum++);
			row = sheet.createRow(rownum++);
			cellnum = 0;
			for (int i = 0; i < 7; i++) {
				cell = row.createCell(cellnum++);
				cell.setCellValue(dataTestHeaders[i]);
				cell.setCellStyle(titleStyle);
			}
			for (int i = 0; i < 3; i++) {
				counter = 1;
				expectedResults = expectedStatement.executeQuery("SELECT * FROM " + tableNames[i]);
				actualResults = actualStatement.executeQuery("SELECT * FROM " + tableNames[i]);
				if (tableCountPass[i]) {
					while (expectedResults.next() && actualResults.next()) {
						for (String column[] : tableColumns[i]) {
							if (column[1].equals("int")) {
								if (expectedResults.getInt(column[0]) != actualResults.getInt(column[0])) {
									row = sheet.createRow(rownum++);
									cellnum = 0;
									row.createCell(cellnum++).setCellValue(tableNames[i]);
									row.createCell(cellnum++).setCellValue(counter);
									row.createCell(cellnum++).setCellValue(column[0]);
									row.createCell(cellnum++).setCellValue(expectedResults.getString(column[0]));
									row.createCell(cellnum++)
											.setCellValue(expectedResults.getString("CollectionTimestamp"));
									row.createCell(cellnum++).setCellValue(actualResults.getString(column[0]));
									row.createCell(cellnum++)
											.setCellValue(actualResults.getString("CollectionTimestamp"));
									pass = false;
								}
							}
							if (column[1].equals("string")) {
								if (!expectedResults.getString(column[0]).equals(actualResults.getString(column[0]))) {
									row = sheet.createRow(rownum++);
									cellnum = 0;
									row.createCell(cellnum++).setCellValue(tableNames[i]);
									row.createCell(cellnum++).setCellValue(counter);
									row.createCell(cellnum++).setCellValue(column[0]);
									row.createCell(cellnum++).setCellValue(expectedResults.getString(column[0]));
									row.createCell(cellnum++)
											.setCellValue(expectedResults.getString("CollectionTimestamp"));
									row.createCell(cellnum++).setCellValue(actualResults.getString(column[0]));
									row.createCell(cellnum++)
											.setCellValue(actualResults.getString("CollectionTimestamp"));
									pass = false;
								}
							}
							if (column[1].equals("blob")) {
//							 System.out.println(results[i][0].getBlob(column[0])); 
//							 if (!(results0.getBlob(column[0]).length() ==
//							 (results1.getBlob(column[0]).length()))){ 
//								 System.out.println("FAIL at: "
//							 + tableNames[i] + " " + column[0] + ", " + results0.getBlob(column[0]) +
//							 " DOES NOT EQUAL " + results1.getBlob(column[0])); dataPass= false; }
								row = sheet.createRow(rownum++);
								cellnum = 0;
								row.createCell(cellnum++).setCellValue(tableNames[i]);
								row.createCell(cellnum++).setCellValue(counter);
								row.createCell(cellnum++).setCellValue(column[0]);
								row.createCell(cellnum++).setCellValue("BLOB");
								row.createCell(cellnum++)
										.setCellValue(expectedResults.getString("CollectionTimestamp"));
								row.createCell(cellnum++).setCellValue("BLOB");
								row.createCell(cellnum++).setCellValue(actualResults.getString("CollectionTimestamp"));
								row.createCell(cellnum++)
										.setCellValue("Blobs are not supported, please do a manual check here");
							}
						}
						counter++;
					}
				} else {
					row = sheet.createRow(rownum++);
					cellnum = 0;
					row.createCell(cellnum++).setCellValue(tableNames[i]);
					row.createCell(cellnum++);
					row.createCell(cellnum++);
					row.createCell(cellnum++);
					row.createCell(cellnum++);
					row.createCell(cellnum++);
					row.createCell(cellnum++);
					row.createCell(cellnum++).setCellValue("Count Test Failed for this table.");
				}
			}
			endLocalDateTime = LocalDateTime.now();
			row = sheet.createRow(rownum++);
			cellnum = 0;
			cell = row.createCell(cellnum++);
			cell.setCellValue(pass ? "DATA TEST: PASS" : "DATA TEST: FAIL");
			cell.setCellStyle(pass ? passStyle : failStyle);
			row = sheet.createRow(rownum++);
			cellnum = 0;
			row.createCell(cellnum++).setCellValue(
					"Start LocalDateTime: " + startLocalDateTime + " | End LocalDateTime: " + endLocalDateTime);
			row = sheet.createRow(rownum++);
			row = sheet.createRow(rownum++);
			for (int i = 0; i < 7; i++) {
				sheet.autoSizeColumn(i);
			}
			FileOutputStream out = new FileOutputStream(file);
			workbook.write(out);
			workbook.close();
			out.close();
			assertTrue(pass);
		} catch (SQLException e) {
			row = sheet.createRow(rownum++);
			cellnum = 0;
			row.createCell(cellnum++).setCellValue("SQLException!");
			throw new SQLException(e);
		} finally {
			expectedStatement.close();
			expectedConnection.commit();
			expectedConnection.close();
			actualStatement.close();
			actualConnection.commit();
			actualConnection.close();
		}
	}

	/**
	 * Tells the project client to perform the data collection
	 * 
	 * @param identifier must be a valid ActionType (e.g. SELECT, INSERT, CLEAR)
	 */

	public String getScenarioName() {
		return scenarioName;
	}
}
