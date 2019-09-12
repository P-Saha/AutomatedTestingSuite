package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * TestBase data model object.
 * 
 * <P>
 * Setup, data collections and helper methods for tests.
 * 
 * <P>
 * Some methods here should likely be placed in their owning test type object
 * for single responsibility
 * 
 * @author Alex Shim, Nick Kitching, Priyonto Saha
 * @version 1.0
 */
public class TestBase {
	protected final String workingDirectory, agentProgramCachePath, projectClientLogPath, serviceName,
			agentLogsDestinationPath, logDestinationPath, mcmLogFolderPath;
	protected String testCaseNumber, sourcePath;
	LocalDateTime startLocalDateTime, endLocalDateTime;
	protected boolean pass;
	protected int waitMinutesProjectClientDataPush;
	protected int waitMinutesProjectClientDataCollection;
	protected File from, to;

	/**
	 * Constructor
	 * 
	 * @param testData must be the HashMap of the Excel sheet columns & the row
	 *                 containing the values for the current test
	 */
	protected TestBase(HashMap<String, String> testData) {
		workingDirectory = new File("").getAbsolutePath();
		agentProgramCachePath = System.getenv(testData.get("agentprogramcache").replaceAll("%|/", ""));
		projectClientLogPath = agentProgramCachePath + "/../Logs";
		serviceName = testData.get("servicename");
		waitMinutesProjectClientDataCollection = (int) Math
				.round(Double.parseDouble(testData.get("waitminutesforprojectclientlogcollection")));
		waitMinutesProjectClientDataPush = (int) Math
				.round(Double.parseDouble(testData.get("waitminutesforprojectclientdatapush")));
		agentLogsDestinationPath = testData.get("agentlogdestinationpath");
		logDestinationPath = testData.get("logdestinationfile");
		mcmLogFolderPath = testData.get("mcmlogfolderpath");
		pass = true;
	}

	/**
	 * Used for debugging
	 */
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("WorkingDirectory", workingDirectory)
				.append("agentProgramCachePath", agentProgramCachePath).append("serviceName", serviceName)
				.append("WaitMinutesProjectClientDataCollection", waitMinutesProjectClientDataCollection)
				.append("WaitMinutesProjectClientDataPush", waitMinutesProjectClientDataPush)
				.append("agentLogsDestinationPath", agentLogsDestinationPath)
				.append("logDestinationPath", logDestinationPath).append("mcmLogFolderPath", mcmLogFolderPath)
				.toString();
	}

	/**
	 * makes a SQL connection to a SQLite Database with a connection string
	 * 
	 * @param connectionString which points to a file location
	 * @return Connection a SQLite database connection object
	 */
	protected static Connection connectSQLite(String connectionString) throws SQLException {
		String url = "jdbc:sqlite:" + connectionString;
		Connection conn = null;
		conn = DriverManager.getConnection(url);
		conn.setAutoCommit(false);
		return conn;
	}

	/**
	 * makes a SQL connection to a mySQL Database with a connection string
	 * 
	 * @param serverName
	 * @param portNumber
	 * @param databaseName
	 * @return a Connection to the mySql database
	 * @throws SQLException if mistakes were made.
	 */
	protected static Connection connectMySQL(String serverName, int portNumber, String databaseName)
			throws SQLException { // Database
		String url = "jdbc:mysql://" + serverName + ":" + portNumber + "/" + databaseName + "?useSSL=false";
		Connection conn = null;
		System.out.println("\nConnecting to MySql database url: " + url);
		conn = DriverManager.getConnection(url, "root", "RI@server!");
		conn.setAutoCommit(false);
		return conn;
	}
	
	/**
	 * Checks if a key is in a hashmap for use.
	 * 
	 * @param map The hashmap that we are looking in
	 * @param key The key we are looking for
	 * @return
	 */
	protected boolean keyInHashMap(HashMap<String,String> map, String key) {
		return map.containsKey(key) && map.get(key) != null && !map.get(key).isEmpty();
	}

	/**
	 * Starts Project Service.
	 * 
	 * @param rt Runtime
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void projectStart(Runtime rt) throws IOException, InterruptedException {
		System.out.println("     Starting " + serviceName);
		rt.exec("cmd /c net start " + serviceName);
		System.out.println("     agentProgram running");
	}

	/**
	 * Stops Project service
	 * 
	 * @param rt Runtime
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void projectStop(Runtime rt) throws IOException, InterruptedException {
		System.out.println("     Stopping " + serviceName);
		rt.exec("cmd /c net stop " + serviceName);
		TimeUnit.SECONDS.sleep(10);
	}

	/**
	 * Cleans a list of folder paths, deleting all files within the folders of the
	 * array.
	 * 
	 * @param pathsToClean The list of paths for the directories we would like to
	 *                     clean.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void CleanFolderPaths(String[] pathsToClean) throws IOException, InterruptedException {
		int countValidCleanPaths = 0;
		for (int i = 0; i < pathsToClean.length; i++) {
			if (pathsToClean[i] != null && new File(pathsToClean[i]).exists()) {
				countValidCleanPaths++;
				System.out.println("     Deleting old logs or cache... " + pathsToClean[i]);
				File toClean = new File(pathsToClean[i]);
				try {
					FileUtils.cleanDirectory(toClean); // cleanDirectory should not delete the directory, but it does,
					if (!toClean.exists()) { // hence the forceMkdir
						FileUtils.forceMkdir(toClean);
					}
				} catch (IOException ex) {
					System.out.println(ex);
				}
			}
		}
		if (countValidCleanPaths == 0) {
			throw new IOException("No folders/files in the test specified folders to clean.");
		}
		TimeUnit.SECONDS.sleep(5);
	}

	/**
	 * Helper used for updateLogLineDates, finding offset in hours, minutes, and
	 * seconds between two LocalDateTime objects.
	 * 
	 * @param prev Past date
	 * @param last Future Date
	 * @return long[] with the DateTime offset. [ hours, minutes, seconds]
	 */
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

	/**
	 * Changes the dates in the log to be relative to the current date, using
	 * different date time formatting for agentProgramV3.log. Used as a helper, if
	 * agent logs have different names, update the overidding function.
	 * 
	 * @param logPath The log path for the log we want to update.
	 * @param agent  True if log is a agentProgramV3.log, false otherwise.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void updateLogLineDates(String logPath, boolean agent) throws IOException, InterruptedException {
		String tempLine = null;
		String regexLogDate = agent ? "^[F-W][a-u][d-u] [A-S][a-u][b-y] [\\d, ]\\d \\d{2}:\\d{2}:\\d{2} \\d{4}"
				: "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}";
		LocalDateTime finalDateTime = null;
		LocalDateTime curDateTime = LocalDateTime.now();
		DateTimeFormatter dateFormatForAgentProgramV3Log = DateTimeFormatter.ofPattern("EEE MMM ppd HH:mm:ss yyyy");
		try (BufferedReader in = new BufferedReader(new FileReader(logPath))) {
			while (((tempLine = in.readLine()) != null)) {
				if (tempLine.matches(regexLogDate + ".*")) {
					finalDateTime = agent ? LocalDateTime.parse(tempLine.split(",")[0], dateFormatForAgentProgramV3Log)
							: LocalDateTime.parse(tempLine.split(" ")[0]);
				}
			}
		}

		try (BufferedReader in = new BufferedReader(new FileReader(logPath))) {
			try (BufferedWriter tempFile = new BufferedWriter(new FileWriter(logPath + ".tmp"))) {
				tempLine = null;
				while (((tempLine = in.readLine()) != null)) {
					if (tempLine.trim().matches(regexLogDate + ".*")) {
						LocalDateTime prevDateTime = agent
								? LocalDateTime.parse(tempLine.split(",")[0], dateFormatForAgentProgramV3Log)
								: LocalDateTime.parse(tempLine.split(" ")[0]);
						Period offsetDate = Period.between(prevDateTime.toLocalDate(), finalDateTime.toLocalDate());
						long offsetTime[] = getOffsetTime(prevDateTime, finalDateTime);
						LocalDateTime tempDateTime = curDateTime.minus(offsetDate).minusHours(offsetTime[0])
								.minusMinutes(offsetTime[1]).minusSeconds(offsetTime[2]);
						tempDateTime = tempDateTime.minusNanos(tempDateTime.getNano());
						tempLine = tempLine.replaceAll(regexLogDate,
								(agent ? dateFormatForAgentProgramV3Log.format(tempDateTime)
										: (tempDateTime.getSecond() == 0) ? (tempDateTime.toString() + ":00")
												: (tempDateTime.toString())));
						tempFile.write(tempLine, 0, tempLine.length());
						tempFile.newLine();
					} else {
						tempFile.write(tempLine, 0, tempLine.length());
						tempFile.newLine();
					}
				}
			}
		}
		new File(logPath).delete();
		FileUtils.moveFile(new File(logPath + ".tmp"), new File(logPath));
		TimeUnit.SECONDS.sleep(1);
	}

	/**
	 * Takes in a path for a folder or a file. If it is a folder, it updates all log
	 * files in the folder. If a file, it updates that file. Uses a overrode
	 * function as a helper. Code in here primarily checks if file is a agentProgramV3
	 * log and also handles folders, allowing log changing functionality to lie
	 * within the overrode helper.
	 * 
	 * @param source String path to a folder or file.
	 * @throws IOException
	 */
	private static void updateLogLineDates(String source) throws IOException {

		if (new File(source).isDirectory()) {
			try (Stream<Path> paths = Files.walk(Paths.get(source))) {
				paths.filter(Files::isRegularFile).forEach(path -> {
					try {
						if (path.toString().endsWith(".log") || path.toString().endsWith(".dg")
								|| path.toString().endsWith(".rcp") || path.toString().endsWith(".rct")) {
							if (path.toString().matches(".*agentProgramV3.log$")) {
								System.out.println("Updating dates found in agentProgramV3 log file... " + path);
								updateLogLineDates(path.toString(), true);
							} else {
								System.out.println("Updating dates found in log file... " + path);
								updateLogLineDates(path.toString(), false);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				});
				System.out.println("Finished updating dates in log files to LocalDateTime.now");
			}
		} else {
			try {
				if (source.endsWith(".log") || source.endsWith(".dg") || source.endsWith(".rcp")
						|| source.endsWith(".rct")) {
					if (source.matches(".*agentProgramV3.log$")) {
						System.out.println("Updating dates found in agentProgramV3 log file... " + source);
						updateLogLineDates(source, true);
					} else {
						System.out.println("Updating dates found in log file... " + source);
						updateLogLineDates(source, false);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Copies canned logs from a QA testcase folder to a specified location. Used
	 * for testing agent.net's ability to observe logs within configured folders.
	 * 
	 * @param source                   file/directory source
	 * @param destination              destination directory
	 * @param preserveFileModifiedDate boolean preserves data modified if true, and
	 *                                 changes it if not true.
	 */
	private static void copyFoldersOrFiles(File source, File destination, boolean preserveFileModifiedDate)
			throws IOException {
		if (source.isDirectory()) {
			FileUtils.copyDirectory(source, destination, preserveFileModifiedDate);
		} else {
			FileUtils.copyFileToDirectory(source, destination, preserveFileModifiedDate);
		}
	}

	/**
	 * Copies an array of files and folders to their proper destinations, preserving
	 * the file modified date for them if needed. sourcePaths and destPaths must be
	 * the same lengths and correspond with each other based on position.
	 * 
	 * @param sourcePaths              Array of paths to be copied.
	 * @param destPaths                Array of paths where files and folders will
	 *                                 be copied to.
	 * @param preserveFileModifiedDate
	 * @param updateLogFileDates
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void copyToProjectPaths(String[] sourcePaths, String[] destPaths, boolean preserveFileModifiedDate,
			boolean updateLogFileDates) throws IOException, InterruptedException {
		int countValidSourcePaths = 0;
		for (int i = 0; i < sourcePaths.length; i++) {
			if (sourcePaths[i] != null && new File(sourcePaths[i]).exists()) {

				countValidSourcePaths++;
				from = new File(sourcePaths[i]);
				to = new File(destPaths[i]);
				System.out.println("     Copying... " + from.getName() + "\n     To... " + to);
				copyFoldersOrFiles(from, to, preserveFileModifiedDate);
				if (updateLogFileDates) {
					updateLogLineDates(destPaths[i]);
				}
			}
		}
		if (countValidSourcePaths == 0) {
			throw new IOException("No source folders/files in the test case folder.");
		}
		TimeUnit.SECONDS.sleep(5);
	}

	/**
	 * Stuffing the TestArchive folder with results and details from the test based
	 * on pathsToArchive array
	 * 
	 * @param archivePathStr destination for the TestArchive Folder and within it.
	 * @param pathsToArchive String[] of relevant paths that should be archived for
	 *                       the test.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void archiveTestData(String archivePathStr, String[] pathsToArchive)
			throws IOException, InterruptedException {
		for (String path : pathsToArchive) {
			from = new File(path);
			to = new File(archivePathStr);
			if (from.exists()) {
				System.out.println("     Archiving..." + from);
				copyFoldersOrFiles(from, to, true);
			} else if (from.getName().contains("sqlite")) {
				System.out.println("ERROR! DATABASE PATH DOES NOT EXIST... " + from);
			}
		}
		System.out.println("     Done archiving to... " + to);
	}

	/**
	 * Allows Project to do it's thing with all the set up
	 * 
	 * @param waitTime
	 * @param pathsToClean
	 * @param copyFrom
	 * @param copyTo
	 * @param archiveDest
	 * @param pathsToArchive
	 * @param updateLogFileDates
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected void performProjectAction(int waitTime, String[] pathsToClean, String[] copyFrom, String[] copyTo,
			String archiveDest, String[] pathsToArchive, boolean updateLogFileDates)
			throws IOException, InterruptedException {
		System.out.println("     Beginning Project tasks ...");
		Runtime rt = Runtime.getRuntime();
		projectStop(rt);
		CleanFolderPaths(pathsToClean);
		copyToProjectPaths(copyFrom, copyTo, true, updateLogFileDates);
		projectStart(rt);
		TimeUnit.MINUTES.sleep(waitTime);
		archiveTestData(archiveDest, pathsToArchive);
		projectStop(rt);
		System.out.println("     Ending Project tasks.");
	}

	public String getTestCaseNumber() {
		return testCaseNumber;
	}

	public LocalDateTime getStartTime() {
		return startLocalDateTime;
	}

	public LocalDateTime getEndTime() {
		return endLocalDateTime;
	}

	public boolean getPass() {
		return pass;
	}
}
