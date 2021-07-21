package handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Class which implements all client side file operations
 *
 */
public class FileManagerClient {

	final static String CLIENTS_DIRECTORY = ".." + File.separator + "Clients" + File.separator;

	
	static void clientSend(ObjectOutputStream out, File file) {

		try {
			InputStream bis = new BufferedInputStream(new FileInputStream(file)); 
			String fileName = file.getName();
			int fileSize = (int) file.length();

			out.writeObject(fileName);
			out.writeObject(fileSize);

			byte[] buffer = new byte[2048];
			int nbytes = 0;

			while((nbytes = bis.read(buffer)) != -1) {
				out.write(buffer, 0, nbytes);
			}
			bis.close();
			out.flush();

		} catch (IOException e) {
			e.printStackTrace();
		} 

	}

	static File clientReceivePhoto(ObjectInputStream in, String userClient) {

		try {
			//clients/user1/photos/photo.jpg:likes:date
			String fileName = (String) in.readObject();

			System.out.println("Photo id: " + fileName);

			int fileSize = (int) in.readObject();
			String nameUserFrom = (String) in.readObject();
			String numberOfLikes = (String) in.readObject();

			@SuppressWarnings("unused")
			String date = (String) in.readObject();


			//Note: add data later 
			System.out.println("Photo from: " + nameUserFrom + "\nNumber of likes: " 
					+ numberOfLikes + "\n");

			String filePath = CLIENTS_DIRECTORY + userClient + File.separator + "receivedPhotos" + File.separator + fileName;

			File file = new File(filePath);

			OutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

			byte [] buffer = new byte[2048];
			int nbytes = 0;
			int temp = fileSize;

			while(temp > 0) {
				nbytes = in.read(buffer, 0 , temp > 2048 ? 2048 : temp);
				bos.write(buffer, 0, nbytes);
				temp -= nbytes;
			}
			file.createNewFile();
			bos.close();
			return file;
		}
		catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	static File clientReceive(ObjectInputStream in) {

		try {
			String fileName = (String) in.readObject();
			System.out.println(fileName);

			int fileSize = (int) in.readObject();
			String[] separate = fileName.split(":");



			String filePath = CLIENTS_DIRECTORY + File.separator + separate[1] + File.separator + separate[2];

			File file = new File(filePath);

			OutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

			byte [] buffer = new byte[2048];
			int nbytes = 0;
			int temp = fileSize;

			while(temp > 0) {
				nbytes = in.read(buffer, 0 , temp > 2048 ? 2048 : temp);
				bos.write(buffer, 0, nbytes);
				temp -= nbytes;
			}

			bos.close();
			return file;
		}
		catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Creates a client Directory inside clients directory, 
	 * and for each client creates a history, myPhotos and receivedPhotos Directories
	 * 
	 * @param userId the user Id
	 * @throws IOException 
	 */
	public static void createClientDirectory(String userId) throws IOException {

		String userDirectoryPath = CLIENTS_DIRECTORY + userId; 
		String historyDirectoryPath = userDirectoryPath + File.separator + "history";
		String myPhotosDirectoryPath = userDirectoryPath + File.separator + "myPhotos"; 
		String receivedPhotosDirectoryPath = userDirectoryPath + File.separator + "receivedPhotos"; 

		createDirectory(userDirectoryPath);
		createDirectory(historyDirectoryPath);
		createDirectory(myPhotosDirectoryPath);
		createDirectory(receivedPhotosDirectoryPath);
	}
	
	
	/**
	 * Creates a directory
	 * 
	 * @param directoryPath the directory path
	 */
	private static void createDirectory(String directoryPath) {

		File directoryFilePath = new File(directoryPath);
		if (!directoryFilePath.exists()){
			directoryFilePath.mkdirs();
		}
	}
	
	
	/**
	 * Write to file each index of the given list. 
	 * Writes one index per line.
	 * 
	 * @param list the list
	 * @param filePath the file path
	 * @throws IOException 
	 */
	static void writeListToFile(List<String> list, String filePath) throws IOException {

		File file = new File(filePath);
		FileWriter fileWriter = new FileWriter(file, true);

		for(String index : list) {
			fileWriter.write(index);
			fileWriter.write("\n");
		}
		fileWriter.close();
	}
	
	
	/**
	 * Erase all file content and then writes to file 
	 * each index of the given list. Writes one index per line.
	 * 
	 * @param list the list
	 * @param filePath  the file path
	 * @throws IOException 
	 */
	static void wipeFileAndWriteListToFile(List<String> list, String filePath) 
			throws IOException {

		//Remove old content of text file
		PrintWriter writer = new PrintWriter(filePath);
		writer.print("");
		writer.close();

		//Writes new updated content to file
		File file = new File(filePath);
		FileWriter fileWriter = new FileWriter(file, true);

		for(String index : list) {
			fileWriter.write(index);
			fileWriter.write("\n");
		}
		fileWriter.close();
	}
	
	
	/**
	 * Creates a new file if not exists
	 * 
	 * @param filePath the file path
	 * @throws IOException
	 */
	static void createFile(String filePath) throws IOException {

		File file = new File(filePath);
		
		if(!file.exists()) {
			file.createNewFile();			
		}
		
	}
	
	/**
	 * Checks if file exists in directory
	 * 
	 * @param filePath the file path
	 * @return true if exists, false otherwise
	 */
	static boolean fileExistsInDirectory(String filePath) {
		
		File file = new File(filePath);
		return file.exists();		
	}
	
	
	/**
	 * Reads a file and returns a list containing 
	 * in each index a line of the file 
	 * 
	 * @param filePath the file path
	 * @return a list containing in each index a line of the file
	 * @throws IOException
	 */
	static List<String> readFileToList(String filePath) throws IOException { 

		Path path = Paths.get(filePath);
		Stream<String> lines = Files.lines(path);

		List<String> list = lines.collect(Collectors.toList());
		lines.close();

		return list;
	}
	
	/**
	 * Check if group id is valid
	 * 
	 * @param groupId the groupId
	 * @return
	 */
	static boolean isGroupInvalid(String groupId) {
		
		String specialName1 = "groupSettings";

		return groupId.equals(specialName1);
	}
}	




