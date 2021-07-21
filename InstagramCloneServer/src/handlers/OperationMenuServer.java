package handlers;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import exceptions.NoFollowersException;

/**
 * Class which receives, processes all given operations
 * and retrieves to client side the desired operation result
 *
 */
public class OperationMenuServer {

	private ObjectOutputStream out;
	private ObjectInputStream in;
	private FileManagerServer fileManagerServer;

	public OperationMenuServer(ObjectOutputStream out, ObjectInputStream in) {
		this.out = out;
		this.in = in;
		this.fileManagerServer = new FileManagerServer();   
	}



	/**
	 * Adiciona o cliente (clientID) à lista de seguidores do utilizador userID. 
	 * Se o cliente já for seguidor de userID ou se o utilizador userID não 
	 * existir deve ser assinalado um erro.
	 * 
	 * @param clientUserId the client id, who wants to follow
	 * @param userId the user id to follow
	 * @throws IOException 
	 * @return 0 - userId is not registered
	 *  	   1 - already following userId
	 *         2 - success
	 */
	public int follow(String clientUserId, String userId) throws IOException {

		//check if users exists
		if(!UserManager.isUserRegistered(userId)) {
			return 0;
		}
		// File following of client user
		String followingFilePath = FileManagerServer.CLIENT_FILES_DIRECTORY + clientUserId + File.separator + "following.txt";

		// File followers of userId 
		String followersFilePath = FileManagerServer.CLIENT_FILES_DIRECTORY + userId + File.separator + "followers.txt";

		//Read File
		List<String> listOfFileLines = fileManagerServer.readFileToList(followersFilePath);

		//Check if client Id is already following userId
		if(listOfFileLines.contains(clientUserId)) {
			return 1;
		}

		fileManagerServer.writeStringToFile(clientUserId, followersFilePath);
		fileManagerServer.writeStringToFile(userId, followingFilePath);

		return 2;
	} 

	/**
	 * Remove o cliente (clientID) da lista de seguidores do utilizador userID.
	 * Se o cliente não for seguidor de userID ou se o utilizador userID 
	 * não existir deve ser assinalado um erro.
	 * 
	 * @param clientUserId the client id, who wants to unfollow
	 * @param userId the user id to unfollow
	 * @throws IOException 
	 * @return 0 - if userId to unfollow doesnt exist,
	 * 		   1 - if clientUserId doesnt follow userId
	 * 		   2 - in case of sucess!
	 */
	public int unFollow(String clientUserId, String userId) throws IOException  {

		//check if users exists
		if(!UserManager.isUserRegistered(userId)) {
			return 0;
		}

		String followersFilePath = FileManagerServer.CLIENT_FILES_DIRECTORY + userId + File.separator + "followers.txt"; 
		String followingFilePath = FileManagerServer.CLIENT_FILES_DIRECTORY + clientUserId + File.separator + "following.txt"; 
		//Read File
		List<String> listOfFileLines = fileManagerServer.readFileToList(followersFilePath);

		//Check if client Id is already following userId
		if(listOfFileLines.contains(clientUserId)) {

			//first remove clientUserID from followers.txt of userID

			//Removes clientId from userId File. Note: one follower per line
			int index = listOfFileLines.indexOf(clientUserId);
			listOfFileLines.remove(index);

			fileManagerServer.wipeFileAndWriteListToFile(listOfFileLines, followersFilePath);

			//secondly remove userID from following of clientUser
			List<String> listOfFileFollowingLines = fileManagerServer.readFileToList(followingFilePath);

			//Removes clientId from userId File. Note: one follower per line
			index = listOfFileFollowingLines.indexOf(userId);
			listOfFileFollowingLines.remove(index);

			fileManagerServer.wipeFileAndWriteListToFile(listOfFileFollowingLines, followingFilePath);

			return 2;
		}
		return 1;
	}


	/**
	 * Obtém a lista de seguidores do cliente ou assinala um erro caso 
	 * o cliente não tenha seguidores.
	 * 
	 * @param clientUserId the client user Id
	 * @throws IOException 
	 */
	public List<String> viewfollowers(String clientUserId) throws IOException {

		String followersFilePath = FileManagerServer.CLIENT_FILES_DIRECTORY 
				+ clientUserId + File.separator + "followers.txt"; 

		return fileManagerServer.readFileToList(followersFilePath);
	}

	/**
	 * Envia uma fotografia (photo) para o perfil do cliente armazenado no
	 * servidor. Assume-se que o conteúdo do ficheiro enviado é válido, ou seja, 
	 * uma fotografia (não é necessário fazer nenhuma verificação).
	 * 
	 * @param clientUserId client user Id
	 * @param photoId the photo Id
	 * @throws IOException 
	 * @return    0 - file already exists
	 *        	  1 - Error
	 * 	          2 - Success 		
	 */
	public int post(String clientUserId, String photoId) throws IOException {

		int operationValue;
		boolean isPhotoInFile = checkIfPhotoIdExists(photoId);

		if(isPhotoInFile) {
			operationValue = -1;
		}
		else {
			out.writeInt(1);
			out.flush();

			String stringFormatToFile =  clientUserId + ":" + photoId;
			String photosIdPath =  FileManagerServer.PHOTOS_ID;

			fileManagerServer.writeStringToFile(stringFormatToFile, photosIdPath);

			operationValue = fileManagerServer.serverReceivePhoto(in, clientUserId);
			out.writeInt(operationValue);
			out.flush();
		}
		return operationValue;

	}


	/**
	 * Check if photoId exists in photosId.txt file
	 * 
	 * @param photoId the photo id
	 * @return true if photo id exists, false otherwise
	 * @throws IOException 
	 */
	private boolean checkIfPhotoIdExists(String photoId) throws IOException {

		//Format of list received: [userId:photosId,  userId:photosId, userId:photosId, ...]
		List<String> listOfPhotosInfo = fileManagerServer.readFileToList(FileManagerServer.PHOTOS_ID);

		//File photosId.txt is empty
		if(listOfPhotosInfo == null || listOfPhotosInfo.isEmpty()) {
			return false;
		}

		for(String photoInfo : listOfPhotosInfo) {
			if(photoInfo.contains(":")) {
				String[] photoInfoSplit = photoInfo.split(":");
				if(photoInfoSplit[1].equals(photoId)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * recebe as nPhotos fotografias mais recentes que se encontram nos perfis 
	 * dos utilizadores seguidos, bem como o número de likes de cada fotografia 
	 * (mostrando tudo no mural). Se existirem menos de nPhotos disponíveis, 
	 * recebe as que existirem (ou assinala um erro se não existir nenhuma).
	 * 
	 * @param clientUserId client user Id
	 * @throws IOException 
	 */
	public void wall(int nPhotos, String clientUserId) throws IOException {

		int operationValue;

		List<String> listOfPhotos = getMostRecentPhotosOfFollowing(nPhotos, clientUserId);
		if(listOfPhotos == null) {
			operationValue = -1;
			out.writeObject(operationValue);
			out.flush();
		}
		else {
			int size = listOfPhotos.size();
			out.writeObject(size);
			for(int i = 0; i < size; i++) {
				fileManagerServer.serverSendPhoto(out, clientUserId, listOfPhotos.get(i));
			}					
		}
	}


	/**
	 * Gets most recent photos by date, of users which clientUserId is following 
	 * 
	 * @param nPhotos the number of photos client wants to see
	 * @param clientUserId the client id
	 * @return a list of strings where each index represents a 
	 * 		   a photo by date, of users which clientUserId is following  
	 * @throws IOException 
	 */
	private List<String> getMostRecentPhotosOfFollowing(int nPhotos, String clientUserId) 
			throws IOException {

		// get following first
		String followingFilePath = FileManagerServer.CLIENT_FILES_DIRECTORY + clientUserId + File.separator + "following.txt";
		List<String> followingList = fileManagerServer.readFileToList(followingFilePath);

		if(followingList == null) {
			return null;
		}
		//Then for each follower get all photos
		List<String> photosList = new ArrayList<>();

		// array to add all photoInfo [clients/user1/photos/photo.jpg:likes:date, 
		//clients/user2/photos/photo.jpg:likes:date]
		ArrayList<String> userPhotos = new ArrayList<>();

		for(String follower : followingList) {
			String filePathPhotoInfo = FileManagerServer.CLIENT_FILES_DIRECTORY + follower + File.separator + "photos" 
					+ File.separator + "photos.txt";

			//List of all photos 
			List<String> list = fileManagerServer.readFileToList(filePathPhotoInfo);

			for(String fileInfo : list) {
				userPhotos.add(fileInfo);
			}
		}
		if(nPhotos >= userPhotos.size()) {
			return userPhotos;	
		}
		//now get n most recent photos from all followers to photoList
		//finally compare all photo dates from all clients and send to client
		for (int i = 0; i < nPhotos; i++) {
			int maxIndex = 0;

			for(int j = 1; j < userPhotos.size(); j++) {
				if(fileManagerServer.fileDateComparator(userPhotos.get(j), userPhotos.get(maxIndex)) == -1) {
					maxIndex = j;
				}
			}
			photosList.add(userPhotos.remove(maxIndex));
		}
		return photosList;
	}


	/**
	 * Coloca um like na fotografia photoID. Todas as fotografias têm um 
	 * identificador único atribuído pelo servidor. Os clientes obtêm os identificadores das 
	 * fotografias através do comando wall. Se a fotografia não existir deve ser assinalado um erro.
	 * 
	 * @return 
	 * @throws IOException 
	 */
	public int like(String photoId) throws IOException {

		boolean isPhotoInFile = checkIfPhotoIdExists(photoId);
		if(!isPhotoInFile) {
			return -1;
		}
		else {
			String photoIdOwner = getPhotoIdOwner(photoId);
			updateLikes(photoId, photoIdOwner);
			return 0;
		}
	}


	/**
	 * Gets the photoId user
	 * Reads file photosId.txt and returns userId corresponding to photoId
	 * 
	 * @param photoId the userId
	 * @throws IOException 
	 * @return photo id owner name
	 */
	private String getPhotoIdOwner(String photoId) throws IOException {

		//Format of list received: [userId:photosId,  userId:photosId, userId:photosId, ...]
		List<String> listOfPhotosInfo = fileManagerServer.readFileToList(FileManagerServer.PHOTOS_ID);

		for(String photoInfo : listOfPhotosInfo) {
			String[] photoInfoSplit = photoInfo.split(":");
			if(photoInfoSplit[1].equals(photoId)) {
				return photoInfoSplit[0];
			}
		}
		return null;
	}


	/**
	 * Updates a photoId likes
	 * Reads photos.txt file, and update photoId likes
	 * 
	 * @param photoId the photoId
	 * @throws IOException 
	 */
	private void updateLikes(String photoId, String userId) throws IOException {

		//Format: [clients/user1/photos/photo.jpg:likes:date, clients/user1/photos/photo.jpg:likes:date]
		String filePathPhotoInfo = FileManagerServer.CLIENT_FILES_DIRECTORY + userId + File.separator + "photos" 
				+ File.separator + "photos.txt";
		List<String> listOfphotoInfo = fileManagerServer.readFileToList(filePathPhotoInfo);

		List<String> newListOfphotoInfo = new ArrayList<>();

		for(String photoInfo : listOfphotoInfo) {
			String[] photoInfoSplit = photoInfo.split(":");

			String photoPath = photoInfoSplit[0];
			
			String[] photoPathSplit;
			
			String separator = File.separator;
			if(separator.equals("/")) { //Linux
				photoPathSplit = photoPath.split(separator);
			}
			else { //Windows
				photoPathSplit = photoPath.split("\\" + separator);
			}

			String photoIdFromPath = photoPathSplit[3];
			String likes = photoInfoSplit[1];

			String date = photoInfoSplit[2];

			if(photoId.equals(photoIdFromPath)) {
				int likesUpdate = Integer.parseInt(likes);
				likesUpdate++;

				String newPhotoInfo = photoPath + ":" + String.valueOf(likesUpdate) + ":" + date; 
				newListOfphotoInfo.add(newPhotoInfo);
			} else {
				newListOfphotoInfo.add(photoInfo);
			}
		}
		fileManagerServer.wipeFileAndWriteListToFile(newListOfphotoInfo, filePathPhotoInfo);
	}


	/**
	 * Cria um grupo privado, cujo dono (owner) será o cliente que o criou. 
	 * Se o grupo já existir assinala um erro.
	 * 
	 * @param clientUserId the client id 
	 * @param groupId the group id
	 * @throws IOException 
	 * @return 
	 */
	public int newgroup(String clientUserId, String groupId) throws IOException {

		List<String> listOfgroupInfo = getGroupInfoList();

		boolean isGroupIdExistent = checkIfGroupIdExists(groupId, listOfgroupInfo);
		if(isGroupIdExistent) {
			return -1;
		}
		else {
			createNewGroup(groupId, clientUserId);
			return 0;
		}
	}



	/**
	 * Reads groupIdSettings.txt file and adds to a list each line
	 * Each list index has a file line
	 * 
	 * @return a list where each list index has a file line
	 * @throws IOException
	 */
	private List<String> getGroupInfoList() throws IOException {

		//Format of list received: [groupId:owner:member1,member2,memberN,...]
		List<String> listOfgroupInfo = fileManagerServer.readFileToList(FileManagerServer.GROUP_ID_SETTINGS);
		return listOfgroupInfo;
	}


	/**
	 * Checks listOfgroupInfoif contains the groupId
	 * 
	 * @param groupId the group Id
	 * @param listOfgroupInfo a list containing 
	 * @return true if groupId exists, false otherwise
	 * @throws IOException 
	 * @requires getGroupInfoList() as input list
	 */
	private boolean checkIfGroupIdExists(String groupId, List<String> listOfgroupInfo) 
			throws IOException {

		//File photosId.txt is empty
		if(listOfgroupInfo == null || listOfgroupInfo.isEmpty()) {
			return false;
		}

		for(String groupInfo : listOfgroupInfo) {
			if(groupInfo.contains(":")) {
				String[] groupInfoSplit = groupInfo.split(":");
				if(groupInfoSplit[0].equals(groupId)) {
					return true;
				}
			}
		}
		return false;
	}



	/**
	 * Creates a new groupId. Writes to groupIdSettings.txt
	 * in one line in this format: groupId:owner:
	 * 
	 * @param groupId the group id
	 * @param groupIdOwner the group id owner
	 * @throws IOException 
	 */
	private void createNewGroup(String groupId, String groupIdOwner) throws IOException {

		String newGroupInfo = groupId + ":" + groupIdOwner + ":";
		fileManagerServer.writeStringToFile(newGroupInfo, FileManagerServer.GROUP_ID_SETTINGS);

		//Create grouId.txt
		String groupIdPath = FileManagerServer.GROUP_ID_DIRECTORY + File.separator + groupId + ".txt";
		String groupIdHistory = FileManagerServer.GROUP_ID_DIRECTORY + File.separator + groupId + "History.txt"; 

		FileManagerServer.createFile(groupIdPath);
		FileManagerServer.createFile(groupIdHistory);
	}


	/**
	 * Adiciona o utilizador userID como membro do grupo indicado.
	 * Se userID já pertencer ao grupo ou se o grupo não existir deve ser assinalado um erro.
	 * Apenas os donos dos grupos podem adicionar utilizadores aos seus grupos, pelo que 
	 * deverá ser assinalado um erro caso o cliente não seja dono do grupo.
	 * 
	 * @param clientUserId the client id
	 * @param userId the user id
	 * @param groupId the group id
	 * @return -2 - userId do not exists
	 *   	   -1 - groupId do not exists
	 *  	    0 - clienteUserId is not the group owner
	 *   	    1 - userId is already in the group
	 *   		2 - Success
	 * @throws IOException 
	 */
	public int addu(String clientUserId, String userId, String groupId) throws IOException {

		if(!UserManager.isUserRegistered(userId)) {
			return -2;
		}
		else {
			//Format of list received: [groupId:owner:member1,member2,memberN,...]
			List<String> listOfgroupInfo = getGroupInfoList();

			boolean isGroupIdExistent = checkIfGroupIdExists(groupId, listOfgroupInfo);
			if(!isGroupIdExistent) {
				return -1;
			}
			else {
				boolean isGroupIdOwner = isOwner(groupId, clientUserId, listOfgroupInfo);
				if(!isGroupIdOwner) {
					return 0;
				}
				else {
					boolean isUserIdMemberOfGroupId = isMemberOfGroup(userId, groupId, listOfgroupInfo);
					if(isUserIdMemberOfGroupId) {
						return 1;
					}
					else {
						addUser(groupId, userId, listOfgroupInfo);
						return 2;
					}
				}
			}	
		}
	}


	/**
	 * Reads groupSettings.txt and checks if groupId owner 
	 * matches given groupIdOwner
	 * 
	 * @param groupId the group id
	 * @param groupIdOwner the owner
	 * @param listOfgroupInfo a list where index match a file line
	 * @return true if groupIdOwner is the groupId owner, false otherwise
	 * @throws IOException 
	 * @requires checkIfGroupIdExists(String groupId, String clientUserId) == true
	 */
	private boolean isOwner(String groupId, String groupIdOwner, List<String> listOfgroupInfo) 
			throws IOException {

		for(String groupInfo : listOfgroupInfo) {
			if(groupInfo.contains(":")) {
				String[] groupInfoSplit = groupInfo.split(":");
				if(groupInfoSplit[0].equals(groupId)) {
					if(groupInfoSplit[1].equals(groupIdOwner)) {
						return true;
					}
				}
			}
		}
		return false;
	}


	/**
	 * Given a list where each list index match a file line,
	 * checks for each list index if userId is member of groupId
	 * Note: Each index format: [groupId:owner:member1,member2,memberN,...]
	 * 
	 * @param userId the user id
	 * @param groupId the group id
	 * @param listOfgroupInfo a list where index match a file line
	 * @return true if userId is member of groupId
	 * @throws IOException 
	 */
	private boolean isMemberOfGroup(String userId, String groupId, List<String> listOfgroupInfo) 
			throws IOException {

		for(String groupInfo : listOfgroupInfo) {
			if(groupInfo.contains(":")) {

				String[] groupInfoSplit = groupInfo.split(":");
				if(groupInfoSplit[0].equals(groupId)) {

					if(groupInfoSplit.length != 2) {
						String[] groupMembers = groupInfoSplit[2].split(",");

						for(String member : groupMembers) {
							if(member.equals(userId)) {
								return true;													
							}
						}
					}
				}
			}
		}
		return false;
	}


	/**
	 * Adds user id to groupSettings.txt
	 * Wipes up old file and writes new updated content
	 * 
	 * @param groupId the groupId
	 * @param userId the userId
	 *  @param listOfgroupInfo a list where index match a file line
	 * @throws IOException 
	 */
	private void addUser(String groupId, String userId, List<String> listOfgroupInfo) throws IOException {

		List<String> updatedListOfgroupInfo = new ArrayList<>();

		for(String groupInfo : listOfgroupInfo) {
			if(groupInfo.contains(":")) {
				String[] groupInfoSplit = groupInfo.split(":");
				if(groupInfoSplit[0].equals(groupId)) {
					groupInfo = groupInfo + userId + ",";
				}
			}
			updatedListOfgroupInfo.add(groupInfo);
		}
		fileManagerServer.wipeFileAndWriteListToFile(updatedListOfgroupInfo, FileManagerServer.GROUP_ID_SETTINGS);
	}

	/**
	 * Remove o utilizador userID do grupo indicado. Se userID 
	 * não pertencer ao grupo ou o grupo não existir deve ser assinalado um erro. Apenas os 
	 * donos dos grupos podem remover utilizadores dos seus grupos, pelo que deverá ser
	 * assinalado um erro caso o cliente não seja dono do grupo.
	 * 
	 * @param clientUserId the client Id
	 * @param userId the user Id
	 * @param groupId the group Id
	 * @throws IOException 
	 * @return 
	 *  		-2 - userId do not exists
	 *  		-1 - groupId do not exists
	 *   		 0 - clienteUserId is not the group owner
	 *   		 1 - userId is not in in the group
	 *   		 2 - Success
	 */
	public int removeu(String clientUserId, String userId, String groupId) throws IOException {

		if(!UserManager.isUserRegistered(userId)) {
			return -2;
		}
		else {
			List<String> listOfgroupInfo = getGroupInfoList();

			boolean isGroupIdExistent = checkIfGroupIdExists(groupId, listOfgroupInfo);
			if(!isGroupIdExistent) {
				return -1;
			}
			else {
				boolean isGroupIdOwner = isOwner(groupId, clientUserId, listOfgroupInfo);
				if(!isGroupIdOwner) {
					return 0;
				}
				else {
					boolean isUserIdMemberOfGroupId = isMemberOfGroup(userId, groupId, listOfgroupInfo);
					if(isUserIdMemberOfGroupId) {
						removeUser(groupId, userId,listOfgroupInfo);
						return 2;
					}
					else {					
						return 1;
					}
				}
			}	
		}
	}


	/**
	 * Removes user id from groupId
	 * 
	 * @param groupId the group id
	 * @param userId the user id
	 * @param listOfgroupInfo a list where each index match a file line 
	 * @throws IOException 
	 * @requires isMemberOfGroupNew(userId, groupId, listOfgroupInfo) == true
	 */
	public void removeUser(String groupId, String userId, List<String> listOfgroupInfo) throws IOException {

		List<String> updatedListOfgroupInfo = new ArrayList<>();

		for(String groupInfo : listOfgroupInfo) {
			if(groupInfo.contains(":")) {
				String[] groupInfoSplit = groupInfo.split(":");
				if(groupInfoSplit[0].equals(groupId)) {
					String [] members = groupInfoSplit[2].split(",");
					StringBuilder sb = new StringBuilder();
					for(String member : members) {
						if(!member.equals(userId)) {
							sb.append(member + ",");
						}
					}
					groupInfo = groupInfoSplit[0] + ":" + groupInfoSplit[1] + ":" + sb.toString();
				}
			}
			updatedListOfgroupInfo.add(groupInfo);
		}
		fileManagerServer.wipeFileAndWriteListToFile(updatedListOfgroupInfo, FileManagerServer.GROUP_ID_SETTINGS);
		
		//Read groupId.txt and check if userId has name after the message
		String groupIdPath = FileManagerServer.GROUP_ID_DIRECTORY + File.separator + groupId + ".txt";
		
		List<String> groupContentList = fileManagerServer.readFileToList(groupIdPath);
		
		if(!hasCollect(groupId, userId, groupContentList)) {
			
			List<String> groupContentWithoutUserId = new ArrayList<>();
			List<String> readByAll = new ArrayList<>();
			
			StringBuilder sb = new StringBuilder();
			
			for(String message : groupContentList) {
				String [] messageSplit = message.split("&\\|&");
				String [] viewers = messageSplit[2].split(",");
				
				if(viewers.length == 1) {
					readByAll.add(messageSplit[0] + ":" +  messageSplit[1]);
				}
				else {
					for (String viewer : viewers) {
						if(!viewer.equals(userId)) {
							sb.append(viewer + ",");
						}
					}
					groupContentWithoutUserId.add(messageSplit[0] + "&|&" + messageSplit[1] + "&|&" + sb.toString());
				}
			}
			String groupIdHistoryPath = FileManagerServer.GROUP_ID_DIRECTORY + File.separator + groupId + "History.txt";
			fileManagerServer.writeListToFile(readByAll, groupIdHistoryPath);
			fileManagerServer.wipeFileAndWriteListToFile(groupContentWithoutUserId, groupIdPath);
		}
		
	}
	
	/**
	 * Checks if user id has collected all his messages before being removed from groupId
	 * 
	 * @param groupId the group id
	 * @param userId the user id
	 * @param groupContentList list containing groudId messages
	 * @return true if user id has already collected all his messages, false otherwise
	 */
	private boolean hasCollect(String groupId, String userId, List<String> groupContentList) {
		
		for(String message : groupContentList) {
			String [] messageSplit = message.split("&\\|&");
			String [] viewers = messageSplit[2].split(",");
			
			for (String viewer : viewers) {
				if(viewer.equals(userId)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Se for especificado o groupID, mostra o dono do grupo e uma lista dos membros do grupo. Se o 
	 * groupID especificado não existir ou se o cliente não for dono nem membro do grupo, deve 
	 * ser assinalado um erro.	
	 * 
	 * @param clientUserId the client id
	 * @param groupId the group id
	 * @throws IOException 
	 * @return 
	 *		  -2 group does not exist
	 * 		  -1 user is not member or owner of that group
	 *  	   0  Success	
	 *  	   1 confirmation function out			 
	 */
	public int gInfo(String clientUserId, String groupId) throws IOException {

		List<String> listOfgroupInfo = getGroupInfoList();

		boolean groupExists = checkIfGroupIdExists(groupId, listOfgroupInfo);
		if(!groupExists) {
			return -2;
		}
		else {
			boolean isUserOwner = isOwner(groupId, clientUserId, listOfgroupInfo);
			boolean isUserMember = isMemberOfGroup(clientUserId, groupId,listOfgroupInfo);

			if(!isUserOwner && !isUserMember) {
				return -1;
			} 
			else {
				//HashMap type ['Owner':<group1,group2, group3> , 'Member':<group1, group2..>]
				HashMap<String, List<String>> groups = 
						getGroups(clientUserId, Optional.of(groupId), listOfgroupInfo);
				out.writeObject(0);
				out.writeObject(groups);
				out.flush();			
				return 1;
			}
		}
	}


	/**
	 * Se groupID não for especificado, mostra uma lista dos grupos de que o 
	 * cliente é dono, e uma lista dos grupos a que pertence. Caso não seja dono de nenhum 
	 * grupo ou não seja membro de nenhum grupo, esses factos deverão ser assinalados. 
	 * 
	 * @param clientUserId the client id
	 * @throws IOException
	 */
	public void gInfo(String clientUserId) throws IOException {

		List<String> listOfgroupInfo = getGroupInfoList();

		HashMap<String,List<String>> groups = 
				getGroups(clientUserId, Optional.empty(),listOfgroupInfo);

		out.writeObject(groups);
		out.flush();
	}


	/**
	 * If groupId is empty get all the groups that contain clientUserId
	 * If the groupId is not empty get that group
	 * 
	 * @param clientUserId
	 * @param groupId the group Id
	 * @return HashMap containing all the groups that contain clientUserId
	 * @throws IOException 
	 */
	private HashMap<String,List<String>> getGroups(String clientUserId, 
			Optional<String> groupId, List<String> listOfgroupInfo) throws IOException {

		//separate the groups where clientUserID is Owner and groups where id Member
		// ['Owner':[group1,group2,...] , 'Member': [group1, group2,...]
		HashMap<String,List<String>> listOfGroupsOwnerMember = new HashMap<>();
		listOfGroupsOwnerMember.put("Owner", new ArrayList<>());
		listOfGroupsOwnerMember.put("Member", new ArrayList<>());


		if(groupId.isPresent()) {

			for (String groupIdInfo : listOfgroupInfo) {

				String[] groupSplitInfo = groupIdInfo.split(":");
				String groupIdFromFile = groupSplitInfo[0];
				String owner = groupSplitInfo[1];

				if (groupIdFromFile.equals(groupId.get())) {
					if(owner.equals(clientUserId)) {

						List<String> groupsOwner = listOfGroupsOwnerMember.get("Owner");
						groupsOwner.add(groupIdInfo);
						listOfGroupsOwnerMember.replace("Owner", groupsOwner);
						return listOfGroupsOwnerMember;	
					}
					else {
						List<String> groupsMember = listOfGroupsOwnerMember.get("Member");
						groupsMember.add(groupIdInfo);
						listOfGroupsOwnerMember.replace("Member", groupsMember);
						return listOfGroupsOwnerMember;
					}
				}	
			}
		}
		else {
			for (String groupIdInfo : listOfgroupInfo) {

				String[] groupSplitInfo = groupIdInfo.split(":");
				String owner = groupSplitInfo[1];

				if(owner.equals(clientUserId)) {
					List<String> groupsOwner = listOfGroupsOwnerMember.get("Owner");

					groupsOwner.add(groupIdInfo);
					listOfGroupsOwnerMember.replace("Owner", groupsOwner);
				}
				else {
					if(groupSplitInfo.length == 3) {

						String[] members = groupSplitInfo[2].split(",");
						for (String member : members) {

							if(member.equals(clientUserId)) {
								List<String> groupsMember = listOfGroupsOwnerMember.get("Member");

								groupsMember.add(groupIdInfo);
								listOfGroupsOwnerMember.replace("Member", groupsMember);
							}
						}
					}
				}
			}
			return listOfGroupsOwnerMember;
		}
		return null;
	}


	/**
	 * Envia uma mensagem (msg) para o grupo groupID, que ficará 
	 * guardada numa caixa de mensagens do grupo, no servidor. A mensagem ficará acessível 
	 * aos membros do grupo através do comando collect. Se o grupo não existir ou o cliente não 
	 * pertencer ao grupo deve ser assinalado um erro.
	 * 
	 * @param clientUserId the client id
	 * @param groupId the group Id
	 * @param msg the message to send
	 * @throws IOException 
	 */
	public int msg(String clientUserId, String groupId, String msg) 
			throws IOException { 

		List<String> listOfGroupInfo = getGroupInfoList();
		StringBuilder sb = new StringBuilder() ;
		boolean groupIdExists = checkIfGroupIdExists(groupId,listOfGroupInfo);

		if(!groupIdExists) {
			return -1;
		}
		String groupIdPath =  FileManagerServer.GROUP_ID_DIRECTORY + File.separator + groupId + ".txt";
		boolean isUserOwner = isOwner(groupId, clientUserId, listOfGroupInfo);
		boolean isUserMember = isMemberOfGroup(clientUserId, groupId,listOfGroupInfo);
		String owner = "";

		if(!isUserOwner && !isUserMember) {
			return 0;
		} 	
		for (String groupInfo : listOfGroupInfo) {
			String [] separateGroupInfo = groupInfo.split(":");

			if(separateGroupInfo[0].equals(groupId)) {
				owner = separateGroupInfo[1];

				if(separateGroupInfo.length == 3) {					
					String members = separateGroupInfo[2];
					String[] separateMembers = members.split(",");

					for (String member : separateMembers) {
						sb.append(member + ",");						
					}
				}		
			}			
		}
		/**
		 * Os donos dos grupos contam como
		 * membros para efeito da remoção de mensagens da caixa de mensagens, ou seja, os donos
		 * também recebem as mensagens que eles próprios enviaram.
		 */
		sb.append(owner + ",");
		sb.deleteCharAt(sb.length()-1);

		//(Delimiter &|& that is never written in a message!)
		String messageFormat = clientUserId + "&|&" + msg + "&|&" + sb.toString();

		fileManagerServer.writeStringToFile(messageFormat, groupIdPath); 

		return 1;
	}


	/**
	 * Recebe todas as mensagens que tenham sigo enviadas para o grupo 
	 * groupID e que o cliente ainda não tenha recebido. Por exemplo, se a caixa de mensagens 
	 * do grupo tem 3 mensagens (m1, m2, m3), se o utilizador u1 já recebeu as mensagens m1 
	 * e m2 e o utilizador u2 ainda não recebeu nenhuma, então a execução do comando pelo 
	 * utilizador u1 retornará apenas m3, mas se for executado pelo utilizador u2 retornará as 3 
	 * mensagens. Se não existir nenhuma nova mensagem, esse facto deverá ser assinalado. Os 
	 * utilizadores apenas têm acesso às mensagens enviadas depois da sua entrada no grupo. 
	 * Quando uma mensagem é lida por todos os utilizadores, esta é removida da caixa de 
	 * mensagens e colocada num histórico do grupo. Os donos dos grupos contam como 
	 * membros para efeito da remoção de mensagens da caixa de mensagens, ou seja, os donos 
	 * também recebem as mensagens que eles próprios enviaram. Se o grupo não existir ou o 
	 * cliente não pertencer ao grupo deve ser assinalado um erro.
	 * 
	 * @param clientUserId the client id
	 * @param groupId the group id
	 * @throws IOException 
	 * @return
	 *  		-1 - groupId do not exists
	 *   	 	 0 - clienteUserId is not member or owner of group
	 *   	 	 1 - Success
	 */
	public int collect(String clientUserId ,String groupId) throws IOException {	
		List<String> listOfGroupInfo = getGroupInfoList();

		boolean groupIdExists = checkIfGroupIdExists(groupId,listOfGroupInfo);				
		if(!groupIdExists) {
			return -1;
		}
		boolean isUserOwner = isOwner(groupId, clientUserId, listOfGroupInfo);
		boolean isUserMember = isMemberOfGroup(clientUserId, groupId,listOfGroupInfo);

		if(!isUserOwner && !isUserMember) {
			return 0;
		} 
		out.writeObject(1); 
		List<String> listGroupText = fileManagerServer.readFileToList(FileManagerServer.GROUP_ID_DIRECTORY
				+ File.separator + groupId + ".txt");
		List<String> groupContent = new ArrayList<>();  //to update groupFile.txt
		List<String> collectResult = new ArrayList<>(); //to send to user
		List<String> readByAll = new ArrayList<>();     //to history group

		boolean viewed = false;
		for(String message : listGroupText) {
			String [] messageSplit = message.split("&\\|&");
			String [] viewers = messageSplit[2].split(",");
			StringBuilder sb = new StringBuilder();
			if(viewers.length > 1) {
				for (String viewer : viewers) {
					if(viewer.equals(clientUserId)) {
						viewed = true; //has seen the message
						String sender = messageSplit[0];
						String content = messageSplit[1];
						collectResult.add(sender +":"+ content);
					}
					else {
						sb.append(viewer + ",");
					}	
				}
			}
			else if(viewers.length == 1) {
				String viewer = viewers[0]; //has seen the message

				if(viewer.equals(clientUserId)) {
					viewed = true;
					String sender = messageSplit[0];
					String content = messageSplit[1];
					collectResult.add(sender +":"+ content);
					readByAll.add(sender+":"+content);
				}
				else {
					sb.append(viewer + ",");
				}
			}
			else {
				//does nothing 	
			}
			//to remove lines when no more viewers
			if(sb.length() != 0) {
				if(sb.charAt(sb.length()-1) == ',') {
					sb.deleteCharAt(sb.length()-1);
				}
				String messageFormat = messageSplit[0] + "&|&" +  messageSplit[1] + "&|&" + sb.toString();
				groupContent.add(messageFormat);
			}
		}
		if(viewed) {
			String destinyGroupPath = FileManagerServer.GROUP_ID_DIRECTORY + File.separator + groupId + ".txt";
			fileManagerServer.wipeFileAndWriteListToFile(groupContent, destinyGroupPath);

			//now write to history of client the collected lines
			String historyGroupOnClientDirectory = FileManagerServer.CLIENT_FILES_DIRECTORY 
					+ clientUserId + File.separator + "history" + File.separator + groupId + ".txt" ;
			String historyOfReadByAllMessages = FileManagerServer.GROUP_ID_DIRECTORY + File.separator + groupId + "History.txt";

			fileManagerServer.writeListToFile(collectResult, historyGroupOnClientDirectory);
			fileManagerServer.writeListToFile(readByAll, historyOfReadByAllMessages);
		}
		out.writeObject(collectResult);
		return 1;
	}

	/**
	 * Mostra o histórico das mensagens do grupo indicado que o cliente já 
	 * leu anteriormente. Se o grupo não existir ou o cliente não pertencer ao grupo deve ser 
	 * assinalado um erro.
	 * 
	 * @param groupId the group Id
	 * @throws IOException 
	 * @return 
	 *  	   -1 - groupId do not exists
	 *   		0 - clienteUserId is not member or owner of group
	 *   		1 - Sucess (avisar quando nao ha mensagens) 
	 */
	public int history(String clientUserId, String groupId) throws IOException {
		
		String historyGroupFilePath = FileManagerServer.CLIENT_FILES_DIRECTORY + clientUserId
				+ File.separator + "history" + File.separator + groupId + ".txt";
		
		List<String> listOfGroupInfo = getGroupInfoList();

		boolean groupIdExists = checkIfGroupIdExists(groupId, listOfGroupInfo);				
		if(!groupIdExists) {
			return -1;
		}
		//Check if historyGroupFilePath exists in directory
		if(!fileManagerServer.fileExistsInDirectory(historyGroupFilePath)) {
			return -2;
		}
		boolean isUserOwner = isOwner(groupId, clientUserId, listOfGroupInfo);
		boolean isUserMember = isMemberOfGroup(clientUserId, groupId,listOfGroupInfo);

		if(!isUserOwner && !isUserMember) {
			return 0;
		}
		out.writeObject(1); 


		List<String> historyGroup = fileManagerServer.readFileToList(historyGroupFilePath);

		out.writeObject(historyGroup);
		out.flush();
		return 1;
	}


	/**
	 * Receives and process each operation sent by the client side 
	 * 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws NoFollowersException
	 */
	public void receiveAndProcessOperations() 
			throws ClassNotFoundException, IOException, NumberFormatException {

		String commandFromClient = (String) in.readObject();

		//Format message received: "operationName:clientId:parameter1:parameter2:parameterN"
		String[] operationSplit = commandFromClient.split(":");
		String command = operationSplit[0];

		//Note: clientUserId is the id of the client requesting the operation
		//Note: userId is the id of the user we want to make operations with
		String clientUserId, userId, photoId, groupId;
		int operationValue;

		while(!command.equals("exit")) {

			switch(command) {
			case "follow":	//Format: follow:clientUserId:userId				
				clientUserId = operationSplit[1];
				userId = operationSplit[2];
				operationValue = follow(clientUserId, userId);
				out.writeInt(operationValue);
				out.flush();
				break;
			case "unfollow": //Format: unfollow:clientUserId:userId
				clientUserId = operationSplit[1];
				userId = operationSplit[2];
				operationValue = unFollow(clientUserId, userId);
				out.writeInt(operationValue);
				out.flush();
				break;
			case "viewfollowers": //Format: viewfollowers:clientUserId
				clientUserId = operationSplit[1];
				out.writeObject(viewfollowers(clientUserId));
				out.flush();
				break;
			case "post": //Format: post:clientUserId:photoId
				clientUserId = operationSplit[1];
				photoId = operationSplit[2];
				operationValue = post(clientUserId, photoId);
				if(operationValue == -1 ) {
					out.writeInt(operationValue);
					out.flush();								
				}
				break;
			case "wall": //Format: wall:clientUserId:nPhotos
				clientUserId = operationSplit[1];
				int nPhotos = Integer.parseInt(operationSplit[2]);
				wall(nPhotos, clientUserId);
				break;
			case "like": //Format: like:PhotoId
				photoId = operationSplit[1];
				operationValue = like(photoId);
				out.writeInt(operationValue);
				out.flush();
				break;
			case "newgroup": //Format: newgroup:clientUserId:groupId
				clientUserId = operationSplit[1];
				groupId = operationSplit[2];
				operationValue = newgroup(clientUserId, groupId);
				out.writeInt(operationValue);
				out.flush();
				break;
			case "addu": //Format: addu:clientUserId:userId:groupId
				clientUserId = operationSplit[1];
				userId = operationSplit[2];
				groupId = operationSplit[3];
				operationValue = addu(clientUserId, userId, groupId);
				out.writeInt(operationValue);
				out.flush();
				break;
			case "removeu": //Format: rm:clientUserId:userId:groupId
				clientUserId = operationSplit[1];
				userId = operationSplit[2];
				groupId = operationSplit[3];
				operationValue = removeu(clientUserId, userId, groupId);
				out.writeInt(operationValue);
				out.flush();
				break;
			case "ginfo": //Format: ginfo:clientUserId:groupId
				if (operationSplit.length == 3) {
					clientUserId = operationSplit[1];
					groupId = operationSplit[2];
					operationValue = gInfo(clientUserId, groupId);
					if(operationValue != 1) {
						out.writeObject(operationValue);
						out.flush();						
					}
				} 
				else { //Format: ginfo:clientUserId
					clientUserId = operationSplit[1];
					gInfo(clientUserId);
				}
				break;
			case "msg": //Format msg:clientUserId:groupId:msg
				clientUserId = operationSplit[1];
				groupId = operationSplit[2];
				String msg = operationSplit[3];
				operationValue = msg(clientUserId, groupId, msg);
				out.writeObject(operationValue);
				out.flush();						
				break;
			case "collect": //Format collect:clientUserId:groupId
				clientUserId = operationSplit[1];
				groupId = operationSplit[2];
				operationValue = collect(clientUserId, groupId);
				if(operationValue != 1) {
					out.writeObject(operationValue);
					out.flush();						
				}
				break;
			case "history":
				clientUserId = operationSplit[1];
				groupId = operationSplit[2];
				operationValue = history(clientUserId, groupId);
				if(operationValue != 1) {
					out.writeObject(operationValue);
					out.flush();						
				}
				break;
			default:
				break;		
			}
			commandFromClient = (String) in.readObject();	
			operationSplit = commandFromClient.split(":");
			command = operationSplit[0];
		}
	}
}

