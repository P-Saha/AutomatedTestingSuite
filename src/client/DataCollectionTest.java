package client;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;

public class DataCollectionTest extends OperationTest {
	private final String[] pathsToClean = { agentLogsDestinationPath, logDestinationPath, mcmLogFolderPath,
			agentProgramCachePath, projectClientLogPath };
	private final String[] copyFrom = { sourcePath + "Agent Logs", sourcePath + "Logs", sourcePath + "DG",
			sourcePath + "RCP", sourcePath + "RCT" };
	private final String[] copyTo = { agentLogsDestinationPath, logDestinationPath, mcmLogFolderPath, mcmLogFolderPath,
			mcmLogFolderPath };
	private final String archiveDest = workingDirectory + "\\TestArchive\\DataCollect - Test Case " + testCaseNumber
			+ " " + actionType + " " + startLocalDateTime;
	private final String[] pathsToArchive = { logDestinationPath, agentLogsDestinationPath, mcmLogFolderPath,
			agentProgramCachePath, projectClientLogPath };
	protected final boolean updateLogFileDates;
	
	/**
	 * Constructor
	 * 
	 * @param test
	 * @param testData
	 */
	public DataCollectionTest(HashMap<String, String> test, HashMap<String, String> testData) {
		super(test, testData);
		updateLogFileDates = keyInHashMap(allData, "updatedatesinlogs");
	}

	/**
	 * Running a test, running project, and then checking the produced data. 
	 * 
	 * @throws SQLException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void runAction() throws SQLException, IOException, InterruptedException {// Runs the test
		startLocalDateTime = LocalDateTime.now();
		if (keyInHashMap(allData, "include")) {
			System.out.println(
					"\n-------------------------------------------------------\n" + "Starting a new test case.");
			if (keyInHashMap(allData, "logsourcepathpart2")) {
				performProjectAction(waitMinutesProjectClientDataCollection, pathsToClean, copyFrom, copyTo,
						archiveDest + "/" + startLocalDateTime.toString().replaceAll(":","-"), pathsToArchive, updateLogFileDates);
			}
			SQLConnection = connectSQLite(agentProgramCachePath + "\\Project.sqlite");
			innerRunAction("sqlite");
		}
		else {
			pass = false;
			fail(testCaseNumber + " Not included");
		}
	}
}
