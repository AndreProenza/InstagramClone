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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;



/**
 * This class manages all server files
 *
 */
public class FileManagerServer {

	final static String CLIENT_FILES_DIRECTORY = "ClientFiles" + File.separator;
	final static String GROUP_ID_DIRECTORY = CLIENT_FILES_DIRECTORY + "groups";
	final static String PHOTOS_ID = CLIENT_FILES_DIRECTORY + "photosId.txt";
	final static String GROUP_ID_SETTINGS = GROUP_ID_DIRECTORY + File.separator + "groupSettings.txt";

	/*
	 * Receives all kinds of files from client (up until now just photos)
	 * @return 
	 * 		   0 - file already exists
	 * 		   1 - Error
	 * 		   2 - Success 
	 */
	int serverReceivePhoto(ObjectInputStream in, String userId) {

		try {
			String fileName = (String) in.readObject();
			
			String separator = File.separator;
			String [] fileNameFinal;
			if(separator.equals("/")) { //Linux
				fileNameFinal = fileName.split(separator);
			}
			else { //Windows
				fileNameFinal = fileName.split("\\" + separator);
			}

			String nameToDest = fileNameFinal[fileNameFinal.length-1];
			
			int fileSize = (int) in.readObject();
			
			String dest = CLIENT_FILES_DIRECTORY + userId + File.separator + "photos" + File.separator + nameToDest; 
			String photosInfoFile = CLIENT_FILES_DIRECTORY + userId + File.separator + "photos" + File.separator + "photos.txt"; 

			File file = new File(dest);

			if(file.exists()) {
				return 0;
			}

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

			file.createNewFile();

			// now update the photos.txt file
			File filePhotoInfo = new File(photosInfoFile);

			FileWriter fileWriter = new FileWriter(filePhotoInfo,true);

			// get the time of photo post
			LocalDateTime now = LocalDateTime.now(); 


			String year = Integer.toString(now.getYear());
			String dayOfYear = Integer.toString(now.getDayOfYear()); // returns a day 1 to 365
			String hour =  Integer.toString(now.getHour());
			String minute = Integer.toString(now.getMinute());
			String second = Integer.toString(now.getSecond());

			String dateFormat = year + File.separator + dayOfYear + File.separator + hour + File.separator + minute + File.separator + second;


			// photoname:number_of_likes:date------- date (ano/mes/dia/hora/minuto)
			fileWriter.write(dest+":" + "0" + ":" + dateFormat);
			fileWriter.write("\n");
			fileWriter.close();

			return 2;
		} 
		catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return 1;
	}
	
	/*
	 * Receives all kinds of files from client
	 * @return 0 - file already exists
	 * 		   1 - Error
	 * 		   2 - Success 
	 * 
	 */
	int serverReceive(ObjectInputStream in, String userId) {

		try {
			String fileName = (String) in.readObject();
			int fileSize = (int) in.readObject();

			String dest = CLIENT_FILES_DIRECTORY + userId + File.separator + fileName; 

			File file = new File(dest);
			if(file.exists()) {
				return 0;
			}

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

			file.createNewFile();

			return 2;

		} 
		catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return 1;
	}


	void serverSend(ObjectOutputStream out, String userId, String fileName) {
		try {

			String filePath = CLIENT_FILES_DIRECTORY + userId + File.separator + fileName;

			File file = new File(filePath);
			InputStream bis = new BufferedInputStream(new FileInputStream(file));

			out.writeObject(file.getName());
			out.writeObject((int) file.length());

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


	/**
	 * Send photo to client
	 * 
	 * @param out
	 * @param userId
	 * @param photoName
	 */
	void serverSendPhoto(ObjectOutputStream out, String userId, String photoName) {
		try {
			//separate photo name format: clients/user1/photos/photo.jpg   : likes:date
			String[] separatePhotoName = photoName.split(":");
			String[] separateFilePath;
			
			String separator = File.separator;
			if(separator.equals("/")) { //Linux
				separateFilePath = photoName.split(separator);
			}
			else { //Windows
				separateFilePath = photoName.split("\\" + separator);
			}


			String filePath = separatePhotoName[0];
			String nameUser = separateFilePath[1];

			String numberOfLikes = separatePhotoName[1];
			String date = separatePhotoName[2];


			File file = new File(filePath);
			InputStream bis = new BufferedInputStream(new FileInputStream(file));

			out.writeObject(file.getName());
			out.writeObject((int) file.length());

			out.writeObject(nameUser);
			out.writeObject( numberOfLikes);
			out.writeObject(date);

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


	/**
	 * Compares two the date between two files
	 * 
	 * print.jpg:0:2021/55/15/32/59
	 * @return 
	 * 		-1 if file1 is more recent than file 2
	 * 		 1 if file1 is less recent than file 2
	 * 		 0 if both dates are equal
	 */
	int fileDateComparator(String file1, String file2) {


		String[] file1Split = file1.split(":");
		String[] file2Split = file2.split(":");
		
		String[] date1;
		String[] date2;
		
		String separator = File.separator;
		if(separator.equals("/")) { //Linux
			date1 = file1Split[2].split(separator);
			date2 = file2Split[2].split(separator);
		}
		else { //Windows
			date1 = file1Split[2].split("\\" + separator);
			date2 = file2Split[2].split("\\" + separator);
		}

		int year1 = Integer.parseInt(date1[0]);
		int dayOfYear1 = Integer.parseInt(date1[1]);
		int hour1 = Integer.parseInt(date1[2]);
		int minute1 = Integer.parseInt(date1[3]);
		int second1 = Integer.parseInt(date1[4]);

		int year2 = Integer.parseInt(date2[0]);
		int dayOfYear2 = Integer.parseInt(date2[1]);
		int hour2 = Integer.parseInt(date2[2]);
		int minute2 = Integer.parseInt(date2[3]);
		int second2 = Integer.parseInt(date2[4]);
		// to improve compare each with local time instead
		if(year1 < year2) {
			return 1;
		}else if(year1 > year2) {
			return -1;
		}else {
			if (dayOfYear1 < dayOfYear2) {
				return 1;
			} else if(dayOfYear1 > dayOfYear2) {
				return -1;
			} else {
				if (hour1 < hour2) {
					return 1;
				}else if (hour1 > hour2) {
					return -1;
				} else {
					if (minute1 < minute2) {
						return 1;
					} else if(minute1 > minute2) {
						return -1;
					} else {
						if (second1 < second2) {
							return 1;
						}else if(second1 > second2) {
							return -1;
						} else {
							return 0;
						}
					}
				}
			}
		}
	}



	/**
	 * Creates a client Directory inside Client Files directory.
	 * That includes creating a followers and a photos text file, and also
	 * creating two nested history directory to save conversations text files 
	 * and photos directory to store photos
	 * 
	 * @param userId the user Id
	 * @throws IOException 
	 */
	public static void createClientDirectory(String userId) throws IOException {

		//Create userId Directory
		String userIdDirectoryPath = CLIENT_FILES_DIRECTORY + userId;	
		createDirectory(userIdDirectoryPath);

		//Create history directory inside userId directory
		String historyDirectoryPath = userIdDirectoryPath + File.separator + "history";
		createDirectory(historyDirectoryPath);

		//Create photos directory inside userId directory
		String photosDirectoryPath = userIdDirectoryPath + File.separator + "photos";
		createDirectory(photosDirectoryPath);

		//Create photos directory inside userId directory
		String groupsDirectoryPath = GROUP_ID_DIRECTORY;
		createDirectory(groupsDirectoryPath);


		//Create groupIdSettings.txt
		String groupSettingsFilePath = GROUP_ID_SETTINGS;
		createFile(groupSettingsFilePath);

		//Create photosId.txt
		String photosIdFilePath = PHOTOS_ID;
		createFile(photosIdFilePath);

		//Create Followers.txt
		String followersFilePath = userIdDirectoryPath + File.separator + "followers.txt";
		createFile(followersFilePath);

		//Create following.txt
		String followingFilePath = userIdDirectoryPath + File.separator + "following.txt";
		createFile(followingFilePath);

		//Create photos.txt
		String photosFilePath = photosDirectoryPath + File.separator + "photos.txt";
		createFile(photosFilePath);
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
	 * Creates a new file
	 * 
	 * @param filePath the file path
	 * @throws IOException
	 */
	static void createFile(String filePath) throws IOException {

		File file = new File(filePath);
		file.createNewFile();
	}


	/**
	 * Reads a file and returns a list containing 
	 * in each index a line of the file 
	 * 
	 * @param filePath the file path
	 * @return a list containing in each index a line of the file
	 * @throws IOException
	 */
	List<String> readFileToList(String filePath) throws IOException { 

		Path path = Paths.get(filePath);
		Stream<String> lines = Files.lines(path);

		List<String> list = lines.collect(Collectors.toList());
		lines.close();

		return list;
	}


	/**
	 * Erase all file content and then writes to file 
	 * each index of the given list. Writes one index per line.
	 * 
	 * @param list the list
	 * @param filePath  the file path
	 * @throws IOException 
	 */
	void wipeFileAndWriteListToFile(List<String> list, String filePath) 
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
	 * Write to file each index of the given list. 
	 * Writes one index per line.
	 * 
	 * @param list the list
	 * @param filePath the file path
	 * @throws IOException 
	 */
	void writeListToFile(List<String> list, String filePath) throws IOException {

		File file = new File(filePath);
		FileWriter fileWriter = new FileWriter(file, true);

		for(String index : list) {
			fileWriter.write(index);
			fileWriter.write("\n");
		}
		fileWriter.close();
	}

	/**
	 *  Write a string to a given file
	 * 
	 * @param stringToFile the string to be written to the file
	 * @param filePath the file path
	 * @throws IOException
	 */
	void writeStringToFile(String stringToFile, String filePath) throws IOException {

		File file = new File(filePath);
		FileWriter fileWriter = new FileWriter(file, true);
		fileWriter.write(stringToFile);
		fileWriter.write("\n");
		fileWriter.close();
	} 
	
	/**
	 * Checks if file exists in directory
	 * 
	 * @param filePath the file path
	 * @return true if exists, false otherwise
	 */
	boolean fileExistsInDirectory(String filePath) {
		
		File file = new File(filePath);
		return file.exists();		
	}
}
