package client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class OperationTest extends TestBase {
	protected final HashMap<String, String> allData;
	private final String keySet[] = { "NodeId", "ObservationId", "DataPointId", "CollectionTimestamp",
			"ObservationHash", "ArchiveId", "StartLine", "EndArchive", "EndLine", "FileName", "Offset", "ContentsHash",
			"ModificationTime", "Content", "AttributeId", "AttributeValue", "AttributeValueString", "PropertyId",
			"PropertyValue" };
	protected Connection SQLConnection;
	protected Statement SQLStatement;
	protected final String actionType;
	private final String tableName;
	protected final int numberOfRows;

	/**
	 * Constructor
	 * 
	 * @param test
	 * @param testData
	 */
	protected OperationTest(HashMap<String, String> test, HashMap<String, String> testData) {
		super(testData);
//		System.out.println(this.toString());
		allData = test;
		testCaseNumber = allData.get("testcasenumber");
		if (keyInHashMap(allData, "actiontype")) {
			actionType = allData.get("actiontype");
		} else {
			actionType = null;
			fail(testCaseNumber + "... Unable to run this test. Please provide a value for the \"Action Type\" column");
		}
		tableName = allData.get("tablename");

		if (keyInHashMap(allData, "logsourcepathpart2")) {
			sourcePath = testData.get("logsourcefilepart1") + allData.get("logsourcepathpart2") + "/";
		} else {
			sourcePath = null;
		}
		if (keyInHashMap(allData, "numberofrows") && (actionType.equals("SELECT") || actionType.equals("INSERT"))) {
			numberOfRows = (int) Math.round(Double.parseDouble(allData.get("numberofrows")));
		} else if (actionType.equals("SELECT") || actionType.equals("INSERT")) {
			numberOfRows = -1;
			fail(testCaseNumber
					+ "... Unable to run this test. Please provide a value for the \"Number of Rows\" column");
		} else {
			numberOfRows = -1;
		}
	}

	/**
	 * Adds quotes to a String if it is necessary for a generated SQL statement
	 * 
	 * @param key
	 * @return
	 */
	private String quoteAdd(String key) {
		ArrayList<String> quoteArray = new ArrayList<String>(Arrays.asList("ActionType", "DatabaseType", "TableName",
				"DataPointId", "ObservationHash", "FileName", "Content", "AttributeValue", "AttributeValueString",
				"PropertyValue", "LogSamplePath", "LogDestinationPath", "ModificationTime", "CollectionTimestamp"));
		ArrayList<String> excludeForInsertArray = new ArrayList<String>(
				Arrays.asList("StartArchiveCollectionTimestamp", "ModificationTime", "CollectionTimestamp"));
		if (quoteArray.contains(key) && !(excludeForInsertArray.contains(key) && actionType.equals("INSERT"))) {
			return "'" + allData.get(key.toLowerCase()) + "'";
		}
		return allData.get(key.toLowerCase());
	}

	/**
	 * Puts together a INSERT SQL Query for testing use.
	 * 
	 * @return
	 */
	protected String concatInsert() {
		String sqlQuery = "INSERT INTO " + tableName + "(";
		for (String key : keySet) {
			if (keyInHashMap(allData, key.toLowerCase())) {
				sqlQuery += key + ", ";
			}
		}
		sqlQuery = sqlQuery.replaceAll(", $", ") ");
		sqlQuery += "VALUES(";
		for (String key : keySet) {
			if (keyInHashMap(allData, key.toLowerCase())) {
				sqlQuery += quoteAdd(key) + ",";
			}
		}
		sqlQuery = sqlQuery.replaceAll(",$", ")");
		return sqlQuery;
	}

	/**
	 * Puts together a INSERT SQL Query for testing use.
	 * 
	 * @param databaseType Used to see whether a mySQL or SQLite Database is used,
	 *                     due to formatting differences between the two.
	 * @return
	 */
	protected String concatSelect(String databaseType) {
		String equalsOperator = (databaseType.equals("mysql") ? " = " : " == ");
		String sqlQuery = "SELECT * FROM " + tableName + " WHERE ";
		for (String key : keySet) {
			if (keyInHashMap(allData, key.toLowerCase())) {
				// if (quoteAdd(key).contains("<->")) {
				// String[] betweenArray = quoteAdd(key).split("<->", 2);
				// sqlQuery += key + " BETWEEN (" + betweenArray[0] + " AND " + betweenArray[0]
				// + ") AND ";
				// } else {
				sqlQuery += key + equalsOperator + quoteAdd(key) + " AND ";
				// }
			}
		}
		sqlQuery = sqlQuery.replaceAll("AND $", "");
		sqlQuery = sqlQuery.replaceAll("WHERE $", "");
		return sqlQuery;
	}

	/**
	 * Puts together a DELETE FROM SQL Query for testing use.
	 * 
	 * @param databaseType Used to see whether a mySQL or SQLite Database is used,
	 *                     due to access restriction differences between the two
	 * @return
	 */
	protected String concatClear(String databaseType) {
		String sqlQuery = "DELETE FROM " + tableName;
		if (databaseType.equals("mysql") && keyInHashMap(allData, "nodeid")) {
			sqlQuery += " WHERE NodeId = " + (int) Math.round(Double.parseDouble(allData.get("nodeid")));
		} else if (databaseType.equals("mysql")) {
			fail("ActionType: CLEAR must specify a nodeid in MySql related tests. Aborting.");
		}
		return sqlQuery;
	}

	/**
	 * Called by runAction from its children to run a SQL Query on a SQL Database
	 * and make assertions for testing purposes.
	 * 
	 * @param databaseType Used to see whether a mySQL or SQLite Database is used,
	 *                     due to differences between the two types of SQL Databases
	 * @throws SQLException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected void innerRunAction(String databaseType) throws SQLException, IOException, InterruptedException {
		try {
			SQLStatement = SQLConnection.createStatement();
			System.out.println("\n" + "Running Project Client: Test Case " + this.getTestCaseNumber() + ", "
					+ this.getActionType() + ", " + this.getTableName() + ": ");

			if (actionType.equals("INSERT")) {
				String sqlQuery = concatInsert();
				ArrayList<Integer> returnVals = new ArrayList<Integer>();
				for (int i = 0; i < numberOfRows; i++) {
					int returnVal = SQLStatement.executeUpdate(sqlQuery);
					returnVals.add(returnVal);
				}
				for (int i = 0; i < numberOfRows; i++) {
					pass = (1 == (int) returnVals.get(i));
					System.out.println("Expected was: " + 1 + "... Actual was: " + (int) returnVals.get(i));
					assertEquals(1, (int) returnVals.get(i));
				}
			}
			if (actionType.equals("SELECT")) {
				String sqlQuery = concatSelect(databaseType);
				ResultSet queryResult = SQLStatement.executeQuery(sqlQuery);
				System.out.println("SQL rows returned: ");
				ResultSetMetaData metadata = queryResult.getMetaData();
				int columnCount = metadata.getColumnCount();
				for (int i = 1; i <= columnCount; i++) {
					System.out.print("| " + metadata.getColumnName(i) + " |");
				}
				System.out.println("");
				int countOfResults = 0;
				while (queryResult.next()) {
					++countOfResults;
					for (int i = 1; i <= columnCount; i++) {
						System.out.print("| " + queryResult.getString(i) + " |");
					}
					System.out.println("");
				}
				queryResult.close();
				pass = (numberOfRows == countOfResults);
				System.out.println("\nExpected was: " + numberOfRows + "... Actual was: " + countOfResults);
				assertEquals(numberOfRows, countOfResults);
			}
			if (actionType.equals("CLEAR")) {
				String sqlQuery = concatClear(databaseType);
				int returnVal = SQLStatement.executeUpdate(sqlQuery);
				pass = (0 == returnVal);
				System.out.println("Deleted: " + numberOfRows);
			}
		} catch (SQLException e) {
			pass = false;
			System.out.println(e.getMessage());
			throw new SQLException(e);
		} finally {
			SQLStatement.close();
			SQLConnection.commit();
			SQLConnection.close();
			endLocalDateTime = LocalDateTime.now();
		}
	}

	public String getActionType() {
		return actionType;
	}

	public String getTableName() {
		return tableName;
	}
}
