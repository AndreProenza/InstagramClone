package handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Class to manage users
 * Login and register users
 * Writes and Reads from users.txt file
 */
public class UserManager {

	private static final String FILE_NAME = "users.txt";
	
	private File file;

	public UserManager() throws IOException {
		this.file = new File(FILE_NAME);
		checkIfExistsIfNotCreate();		
	}
	
	
	/**
	 * Check if users.txt and clientsFileDirectory exists in server
	 * if any of them does not exist, create
	 * @throws IOException
	 */
	public void checkIfExistsIfNotCreate() throws IOException {
		if(!file.exists()) {
			file.createNewFile();
		}
		File clientFilesDirectory = new File(FileManagerServer.CLIENT_FILES_DIRECTORY);
		if (!clientFilesDirectory.exists()){
			clientFilesDirectory.mkdirs();
		}
	}



	/**
	 * Login a user if user id and user password matches 
	 * to one of the file lines user id and user password.
	 * 
	 * @param userIdFromClient the user id.
	 * @param userPasswordFromClient the user password.
	 * @return 0 wrong password, 1 userNotFound, 2 everything fine.
	 * @throws WrongPasswordException 
	 * @throws FileNotFoundException.
	 */
	public int login(String userIdFromClient, String userPasswordFromClient) 
			throws FileNotFoundException {

		Scanner sc = new Scanner(file);
		/* 0 -> user exists, wrong password 
		   1 -> user not found
		   2 -> everything fine. 
		 */
		boolean userFound = false, wrongPass = false; 
		while (sc.hasNextLine() && !userFound) {

			//(<user>:<nome user>:<password>)
			String userInfoFromFile = sc.nextLine();

			String[] userInfo = userInfoFromFile.split(":");
			if(!(userInfo.length != 3)) { //qdo ficheiro esta vazio mas pode ler a primeira linha
				String userId = userInfo[0], userPassword = userInfo[2];

				if(userIdFromClient.equals(userId)) {
					userFound = true;
					if(!userPasswordFromClient.equals(userPassword)) {			
						wrongPass = true; //Wrong password
					}
				}
			}
		}
		sc.close();

		if(wrongPass) { //user exists, wrong password 
			return 0;
		}
		else if(!userFound) { //user not found
			return 1;	
		}
		return 2; //everything fine. 
	}

	/**
	 * Registers a user by writing to users.txt file in this format:
	 * Format: (<user>:<userName>:<password>)
	 * 
	 * @param userIdFromClient the user id.
	 * @param userPasswordFromClient the user password.
	 * @throws IOException.
	 */
	public void register(String userIdFromClient, String userNameFromClient,
			String userPasswordFromClient) throws IOException {

		FileWriter fileWriter = new FileWriter(file, true);
		fileWriter.write(userIdFromClient + ":" + userNameFromClient + ":" + userPasswordFromClient);
		fileWriter.write("\n");
		fileWriter.close();
	}


	/**
	 * Checks if user id is in the users.txt file
	 * 
	 * @param userId the user id
	 * @return true if user id is registered
	 * @throws IOException 
	 */
	public static boolean isUserRegistered(String userId) throws IOException {

		boolean isRegistered = readFile(FILE_NAME).contains(userId);
		return isRegistered;
	}

	/**
	 * Reads a file and returns a list containing 
	 * in each index a line of the file 
	 * Output format: [userId, userId, ...]
	 * 
	 * @param filePath the file path
	 * @return a list containing in each index a line of the file
	 * @throws IOExceptio
	 */
	private static List<String> readFile(String filePath) throws IOException {

		Path path = Paths.get(filePath);
		Stream<String> lines = Files.lines(path);

		List<String> list = lines.collect(Collectors.toList());
		lines.close();

		List<String> newList = new ArrayList<>();
		for(String user : list) {
			String[] temp = user.split(":");
			newList.add(temp[0]);
		}

		return newList;
	}
}
