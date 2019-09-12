package client;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;

public class DataPushTest extends OperationTest {
	private String serverName, databaseName;
	private int portNumber;
	private final String[] pathsToClean = { agentLogsDestinationPath, logDestinationPath, mcmLogFolderPath,
			agentProgramCachePath, projectClientLogPath };
	private final String[] copyFrom = { sourcePath + "SQLite Files/project.sqlite" };
	private final String[] copyTo = { agentProgramCachePath };
	private String archiveDest = workingDirectory + "\\TestArchive\\DataPush - Test Case " + testCaseNumber + " "
			+ actionType;
	private final String[] pathsToArchive = { agentProgramCachePath, projectClientLogPath };

	/**
	 * Constructor
	 * 
	 * @param test
	 * @param testData
	 */
	public DataPushTest(HashMap<String, String> test, HashMap<String, String> testData) {
		super(test, testData);
		serverName = testData.get("mysqlserver");
		portNumber = (int) Math.round(Double.parseDouble(testData.get("portnumber")));
		databaseName = allData.get("databasename");
	}

	/**
	 * Running a test, running project, and then checking the produced data. 
	 * 
	 * @throws SQLException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void runAction() throws SQLException, IOException, InterruptedException {
		startLocalDateTime = LocalDateTime.now();
		if (keyInHashMap(allData, "include")) {
			System.out.println(
					"\n-------------------------------------------------------\n" + "Starting a new test case.");
			if (keyInHashMap(allData, "logsourcepathpart2")) {
				performProjectAction(waitMinutesProjectClientDataPush, pathsToClean, copyFrom, copyTo,
						archiveDest + "/" + startLocalDateTime.toString().replaceAll(":","-"),
						pathsToArchive, false);
			}
			SQLConnection = connectMySQL(serverName, portNumber, databaseName);
			innerRunAction("mysql");
		}
		else {
			pass = false;
			fail(testCaseNumber + " Not included");
		}
	}
}