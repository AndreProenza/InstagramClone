package handlers;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Class which implements all menu operations
 *
 */
public class OperationMenu {

	private ObjectOutputStream out;
	private ObjectInputStream in;
	private String clientUserId;
	private String commandToSever;

	public OperationMenu(ObjectOutputStream out, ObjectInputStream in, String clientUserId) {
		this.out = out;
		this.in = in;
		this.clientUserId = clientUserId;
		this.commandToSever = null;
	}

	/**
	 * Adiciona o cliente (clientID) à lista de seguidores do utilizador userID. 
	 * Se o cliente já for seguidor de userID ou se o utilizador userID não 
	 * existir deve ser assinalado um erro.
	 * 
	 * @param userId the user Id
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void follow(String userId) throws IOException, ClassNotFoundException {

		if(userId.equals(clientUserId)) {
			System.out.println("You can not follow yourself");
		}
		else {
			commandToSever = "follow:" + clientUserId + ":" + userId;
			out.writeObject(commandToSever);
			out.flush();

			int operationValue = in.readInt();
			if(operationValue == 0) {
				System.out.println(userId + " does not exist");
			}
			else if(operationValue == 1) {
				System.out.println("You already follow " + userId);			
			}
			else {
				System.out.println("You now are following " + userId);
			}			
		}
	}

	/**
	 * Remove o cliente (clientID) da lista de seguidores do utilizador userID.
	 * Se o cliente não for seguidor de userID ou se o utilizador userID 
	 * não existir deve ser assinalado um erro.
	 * 
	 * @param userId the user Id
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void unFollow(String userId) throws IOException, ClassNotFoundException {

		if(userId.equals(clientUserId)) {
			System.out.println("You can not unfollow yourself");
		}
		else {
			commandToSever = "unfollow:" + clientUserId + ":" + userId;
			out.writeObject(commandToSever);
			out.flush();

			int operationValue = in.readInt();
			if(operationValue == 0) {
				System.out.println(userId + " does not exist");
			}
			else if(operationValue == 1) {
				System.out.println("You cant unfollow " + userId);			
			}
			else {
				System.out.println(userId + " sucessfully unfollowed");
			}			
		}
	}


	/**
	 * Obtém a lista de seguidores do cliente ou assinala um erro caso 
	 * o cliente não tenha seguidores.

	 * @param userId the user Id
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void viewfollowers() throws IOException, ClassNotFoundException {

		commandToSever = "viewfollowers:" + clientUserId;
		out.writeObject(commandToSever);
		out.flush();
		@SuppressWarnings("unchecked")
		List<String> followersList = (List<String>) in.readObject();
		if(followersList == null) {
			System.out.println("You have no followers yet");
		}
		else {
			System.out.println("You have " + followersList.size() + " followers\n\nFollowers:");
			for (String follower : followersList) {
				System.out.println(follower);
			}
		}
	}

	/**
	 * Envia uma fotografia (photo) para o perfil do cliente armazenado no
	 * servidor. Assume-se que o conteúdo do ficheiro enviado é válido, ou seja, 
	 * uma fotografia (não é necessário fazer nenhuma verificação).
	 * 
	 * @param photo
	 * @throws IOException 
	 */
	public void post(String photoId) throws IOException {

		//command to send to server post:clientUserId/photoName.jpg (jpg already included in name)
		String commandToServer = "post:" + clientUserId + ":" + photoId;
		String filePath = FileManagerClient.CLIENTS_DIRECTORY + clientUserId + File.separator + "myPhotos" + File.separator + photoId;

		//find file in the client directory
		File fileToSend = new File(filePath);

		//if file doesn't exists
		if (!fileToSend.exists()) {
			System.out.println("File does not exist in client " + clientUserId + "/myPhotos/ directory.");
			return; 
		}

		out.writeObject(commandToServer);
		out.flush();
		int operationValue =  in.readInt();
		if(operationValue == -1) {
			System.out.println("You can not post photo: " + photoId + ",\n"
					+ "because photoId already exists");
			return;
		}
		else {
			//file manager class to send files to server
			FileManagerClient.clientSend(out, fileToSend);
			/*		   0 - file already exists
			 * 		   1 - Error
			 * 		   2 - Success */
			operationValue =  in.readInt();
			if (operationValue == 0 ) {
				System.out.println("Photo already posted");
			}
			else if (operationValue == 1) {
				System.out.println("File corrupted!");
			}
			else {
				System.out.println("Photo posted sucessfully!");
			} 
		}
	}

	/**
	 * recebe as nPhotos fotografias mais recentes que se encontram nos perfis 
	 * dos utilizadores seguidos, bem como o número de likes de cada fotografia 
	 * (mostrando tudo no mural). Se existirem menos de nPhotos disponíveis, 
	 * recebe as que existirem (ou assinala um erro se não existir nenhuma).
	 * 
	 * @param nPhotos number of photos
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void wall(int nPhotos) throws IOException, ClassNotFoundException {

		if(nPhotos <= 0) {
			System.out.println("You can not wall negative numbers\nUsage: "
					+ "Wall nPhotos, nPhotos > 0");
			return;
		}

		commandToSever = "wall:" + clientUserId + ":" + nPhotos;
		out.writeObject(commandToSever);
		out.flush();

		int nPhotosReturn = (int) in.readObject();
		if(nPhotosReturn == -1) {
			System.out.println("You have no photos to show,\n"
					+ "because you are not following any user");
		}
		else {
			System.out.println("\n---------------------- Feed ----------------------\n");
			if(nPhotosReturn == 0) {
				System.out.println("You have no photos to receive right now\n");
			}
			else {
				for (int i = 0; i < nPhotosReturn; i++) {
					FileManagerClient.clientReceivePhoto(in, clientUserId);
				}					
			}
			System.out.println("----------------------------------------------------");
		}
	}


	/**
	 * Coloca um like na fotografia photoID. Todas as fotografias têm um 
	 * identificador único atribuído pelo servidor. Os clientes obtêm os identificadores das 
	 * fotografias através do comando wall. Se a fotografia não existir deve ser assinalado um erro.
	 * 
	 * @param PhotoId the photo Id
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void like(String PhotoId) throws IOException, ClassNotFoundException {

		commandToSever = "like:" + PhotoId;
		out.writeObject(commandToSever);
		out.flush();

		int isLikeValid = in.readInt();
		if(isLikeValid == -1) {
			System.out.println("Photo: " + PhotoId + " do not exist");
		}
		else if(isLikeValid == 0) {
			System.out.println("Like posted sucessfully");
		}
	}

	/**
	 * Cria um grupo privado, cujo dono (owner) será o cliente que o criou. 
	 * Se o grupo já existir assinala um erro.
	 * 
	 * @param groupId the group Id
	 * @throws IOException 
	 */
	public void newgroup(String groupId) throws IOException {
		
		if(FileManagerClient.isGroupInvalid(groupId)) {
			System.out.println("Group: " + groupId + " can not be created\nPlease try other name");
		}
		else {
			commandToSever = "newgroup:" + clientUserId + ":" + groupId;
			out.writeObject(commandToSever);
			out.flush();
			
			int operationValue =  in.readInt();
			if(operationValue == -1) {
				System.out.println("Group: " + groupId + " already exists");
			}
			else if(operationValue == 0) {
				System.out.println("Group: " + groupId + " created sucessfully");
			}			
		}
	}


	/**
	 * Adiciona o utilizador userID como membro do grupo indicado.
	 * Se userID já pertencer ao grupo ou se o grupo não existir deve ser assinalado um erro.
	 * Apenas os donos dos grupos podem adicionar utilizadores aos seus grupos, pelo que 
	 * deverá ser assinalado um erro caso o cliente não seja dono do grupo.
	 * 
	 * @param userID the user Id
	 * @param groupId the group Id
	 * @throws IOException 
	 */
	public void addu(String userId, String groupId) throws IOException {

		if(clientUserId.equals(userId)) {
			System.out.println("You can not add yourself");
		}
		else {
			commandToSever = "addu:" + clientUserId + ":" + userId + ":" + groupId;
			out.writeObject(commandToSever);
			out.flush();

			/*
			 *  -2 - userId do not exists
			 *  -1 - groupId do not exists
			 *   0 - clienteUserId is not the group owner
			 *   1 - userId is already in the group
			 *   2 - sucess
			 */
			int operationValue =  in.readInt();
			if(operationValue == -2) {
				System.out.println("User: " + userId + " do not exists");
			}
			else if(operationValue == -1) {
				System.out.println("Group: " + groupId + " do not exists");
			}
			else if(operationValue == 0) {
				System.out.println("You can not add " + userId + " to groupId: " + groupId + ", \n"
						+ "because you are not the group owner");
			}
			else if(operationValue == 1) {
				System.out.println("User: " + userId + " is already in the group");
			}
			else {
				System.out.println("User: " + userId + " sucessfully added to group: " + groupId);
			}			
		}
	}

	/**
	 * Remove o utilizador userID do grupo indicado. Se userID 
	 * não pertencer ao grupo ou o grupo não existir deve ser assinalado um erro. Apenas os 
	 * donos dos grupos podem remover utilizadores dos seus grupos, pelo que deverá ser
	 * assinalado um erro caso o cliente não seja dono do grupo.
	 * 
	 * @param userID the user Id
	 * @param groupId the group Id
	 * @throws IOException 
	 */
	public void removeu(String userId, String groupId) throws IOException {

		if(clientUserId.equals(userId)) {
			System.out.println("You can not remove yourself");
		}
		else {
			commandToSever = "removeu:" + clientUserId + ":" + userId + ":" + groupId;
			out.writeObject(commandToSever);
			out.flush();

			/*
			 *  -2 - userId do not exists
			 *  -1 - groupId do not exists
			 *   0 - clienteUserId is not the group owner
			 *   1 - userId is already in the group
			 *   2 - sucess
			 */
			int operationValue =  in.readInt();
			if(operationValue == -2) {
				System.out.println("User: " + userId + " do not exists");
			}
			else if(operationValue == -1) {
				System.out.println("Group: " + groupId + " do not exists");
			}
			else if(operationValue == 0) {
				System.out.println("You can not remove " + userId + " from groupId: " + groupId + ", \n"
						+ "because you are not the group owner");
			}
			else if(operationValue == 1) {
				System.out.println("User: " + userId + " is not in the group");
			}
			else {
				System.out.println("User: " + userId + " sucessfully removed from the group: " + groupId);
			}			
		}
	}


	/**
	 * Se groupID não for especificado, mostra uma lista dos grupos de que o 
	 * cliente é dono, e uma lista dos grupos a que pertence. Caso não seja dono de nenhum 
	 * grupo ou não seja membro de nenhum grupo, esses factos deverão ser assinalados. Se for 
	 * especificado o groupID, mostra o dono do grupo e uma lista dos membros do grupo. Se o 
	 * groupID especificado não existir ou se o cliente não for dono nem membro do grupo, deve 
	 * ser assinalado um erro.
	 * 
	 * @param groupId the group Id
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @return -1 - client is not member or owner of a group
	 * 			
	 */
	public void gInfo(Optional<String> groupId) throws IOException, ClassNotFoundException {
		/**Se for 
		 * especificado o groupID, mostra o dono do grupo e uma lista dos membros do grupo. Se o 
		 * groupID especificado não existir ou se o cliente não for dono nem membro do grupo, deve 
		 * ser assinalado um erro.
		 */
		StringBuilder sb = new StringBuilder();
		if(groupId.isPresent()) {
			commandToSever = "ginfo:" + clientUserId + ":" + groupId.get();
			out.writeObject(commandToSever);
			out.flush();
			/**
			 * -2 group does not exist
			 * -1 user is not member or owner of that group
			 *  0  Success
			 * 
			 */	
			int operationValue = (int) in.readObject();

			if(operationValue == -2) {
				System.out.println("Group : " + groupId.get() + " does not exist");
			}
			else if(operationValue == -1) {
				System.out.println("User : " + clientUserId + " is not member or owner of the group: "
						+ groupId.get());
			}
			else{
				@SuppressWarnings("unchecked")
				HashMap<String,List<String>> groups =  (HashMap<String, List<String>>) in.readObject();
				List<String> groupsOwner = groups.get("Owner");
				List<String> groupsMember = groups.get("Member");

				//only one of the them has values 
				if(!groupsOwner.isEmpty()) {

					//format groupId:Owner:members
					String[] groupSplit = groupsOwner.get(0).split(":");
					String members = "";

					if(groupSplit.length == 3) { //contains members
						members = groupSplit[2];
						sb.append(members);				
						sb.deleteCharAt(sb.length()-1);
					}
					System.out.println("Group name: " + groupSplit[0] + "\n Owner: " + groupSplit[1] 
							+ "\n Members: "+ sb.toString());
				}
				else {
					//format groupId:Owner:members
					String[] groupSplit = groupsMember.get(0).split(":");
					String members = "";

					sb.delete(0, sb.length());

					if(groupSplit.length == 3) { //contains members
						members = groupSplit[2];
						sb.append(members);				
						sb.deleteCharAt(sb.length()-1);
					}
					System.out.println("Group name: " + groupSplit[0] + "\n Owner: " + groupSplit[1] 
							+ "\n Members: "+ sb.toString() + "\n");
				}
			}
		}
		else {

			// Se groupID não for especificado, mostra uma lista dos grupos de que o 
			// cliente é dono e uma lista dos grupos a que pertence
			commandToSever = "ginfo:" + clientUserId;
			out.writeObject(commandToSever);
			out.flush();

			@SuppressWarnings("unchecked")
			HashMap<String,List<String>> groups =  (HashMap<String, List<String>>) in.readObject();
			List<String> groupsOwner = groups.get("Owner");
			List<String> groupsMember = groups.get("Member");

			System.out.println("\n------------------ Groups Owner ------------------\n");
			if(groupsOwner.isEmpty()) {
				System.out.println("User: " + clientUserId + " is not owner of any group\n");
			} 
			else {

				for(String group : groupsOwner) {
					//format groupId:Owner:members
					String[] groupSplit = group.split(":");
					String members = "";
					sb.delete(0, sb.length());
					if(groupSplit.length == 3) { //contains members
						members = groupSplit[2];
						sb.append(members);				
						sb.deleteCharAt(sb.length()-1);
					}
					System.out.println("Group name: " + groupSplit[0] + "\n Owner: " + groupSplit[1] 
							+ "\n Members: "+ sb.toString() + "\n");
				}
			}
			System.out.println("------------------ Groups Member -----------------\n");
			if(groupsMember.isEmpty()) {
				System.out.println("User: " + clientUserId + " is not member of any group\n");
			}
			else {
				for(String group : groupsMember) {
					//format groupId:Owner:members
					String[] groupSplit = group.split(":");
					String members = "";
					sb.delete(0, sb.length());
					if(groupSplit.length == 3) {
						members = groupSplit[2];
						sb.append(members);				
						sb.deleteCharAt(sb.length()-1);
					}
					System.out.println("Group name: " + groupSplit[0] + "\n Owner: " + groupSplit[1] 
							+ "\n Members: "+ sb.toString() + "\n");
				}
			}
			System.out.println("--------------------------------------------------");
		}				
	}

	/**
	 * Envia uma mensagem (msg) para o grupo groupID, que ficará 
	 * guardada numa caixa de mensagens do grupo, no servidor. A mensagem ficará acessível 
	 * aos membros do grupo através do comando collect. Se o grupo não existir ou o cliente não 
	 * pertencer ao grupo deve ser assinalado um erro.
	 * 
	 * @param groupId the group Id
	 * @param msg the message to send
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void msg(String groupId, String msg) throws IOException, ClassNotFoundException {

		commandToSever = "msg:" + clientUserId + ":" + groupId + ":" + msg;
		out.writeObject(commandToSever);
		out.flush();

		/* 
		 *  -1 - groupId do not exists
		 *   0 - clienteUserId is not member or owner of group
		 *   1 - Sucess 
		 */
		int operationValue = (int) in.readObject();
		if(operationValue == -1) {
			System.out.println("Group: " + groupId + " does not exists");
		}
		else if(operationValue == 0) {
			System.out.println("You are not member or owner of group: " + groupId);
		}
		else {
			System.out.println("Message posted sucessfully!");			
		}		
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
	 * @param groupId the group Id
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void collect(String groupId) throws IOException, ClassNotFoundException {

		commandToSever = "collect:" + clientUserId + ":" + groupId;
		out.writeObject(commandToSever);
		out.flush();

		/*
		 *  -1 - groupId do not exists
		 *   0 - clienteUserId is not member or owner of group
		 *   1 - Success
		 */
		int operationValue =(int) in.readObject();
		if(operationValue == -1) {
			System.out.println("Group id: " + groupId + " does not exist.");
		}
		else if(operationValue == 0){
			System.out.println("User id: " + clientUserId + " is not member of group.");
		}
		else {
			@SuppressWarnings("unchecked")
			List<String> messagesReceived = (List<String>) in.readObject();
			
			System.out.println("\n--------------------- Collect --------------------\n");
			if(messagesReceived.isEmpty()) {
				System.out.println("There are no new messages.");
			}
			else {
				for(String msg : messagesReceived) {
					System.out.println(msg);
				}
			}
			System.out.println("\n--------------------------------------------------");
			
			//Client does not need history when needed upload from server.
			//Write collect content to userId/history.txt
			String groupIdHistoryFile = FileManagerClient.CLIENTS_DIRECTORY + clientUserId 
					+ File.separator + "history" + File.separator + groupId + ".txt";
			FileManagerClient.createFile(groupIdHistoryFile);
			FileManagerClient.writeListToFile(messagesReceived, groupIdHistoryFile);
		}
	}


	/**
	 * Mostra o histórico das mensagens do grupo indicado que o cliente já 
	 * leu anteriormente. Se o grupo não existir ou o cliente não pertencer ao grupo deve ser 
	 * assinalado um erro.
	 * 
	 * @param groupId the group Id
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void history(String groupId) throws IOException, ClassNotFoundException {
		
		//Check first if groupId exists in client directory
		String groupIdPath = FileManagerClient.CLIENTS_DIRECTORY + clientUserId 
				+ File.separator + "history" + File.separator + groupId + ".txt";
		
		if(FileManagerClient.fileExistsInDirectory(groupIdPath)) {
			List<String> groupIdHistoryList = FileManagerClient.readFileToList(groupIdPath);
			
			for(String msg : groupIdHistoryList) {
				System.out.println(msg);
			}
		}
		else {
			commandToSever = "history:" + clientUserId + ":" + groupId;
			out.writeObject(commandToSever);
			out.flush();
			
			/*
			 *  -1 - groupId do not exists
			 *   0 - clienteUserId is not member or owner of group
			 *   1 - Success  
			 */
			int operationResult = (int) in.readObject();
			if(operationResult == -1) {
				System.out.println("Group id: " + groupId + " does not exist.");
			}
			else if(operationResult == -2) {
				System.out.println("There is no " + groupId + " history to show");
			}
			else if(operationResult == 0){
				System.out.println("User id: " + clientUserId + " is not member of group.");
			}
			else {
				@SuppressWarnings("unchecked")
				List<String> messagesReceived = (List<String>) in.readObject();
				
				System.out.println("\n--------------------- History --------------------\n");
				if(messagesReceived.isEmpty()) {
					System.out.println("There are no messages.");
				}
				else {
					for(String msg : messagesReceived) {
						System.out.println(msg);
					}
				}
				System.out.println("\n--------------------------------------------------");
				
				//Client does not need history when needed upload from server.
				//Write collect content to userId/history.txt
				FileManagerClient.createFile(groupIdPath);
				FileManagerClient.writeListToFile(messagesReceived, groupIdPath);
			}				
		}
	}


	/**
	 * Shows client all possible operations
	 */
	public void showOperationMenu() {
		System.out.println("\n__________________________________________________\n"
				+ "**************************************************\n"
				+ "                InstagramClone Menu               \n"
				+ "__________________________________________________\n"
				+ "**************************************************\n"
				+ "follow <userID> or f <userID>\n"
				+ "unfollow <userID> or u <userID>\n"
				+ "viewfollowers or v\n"
				+ "post <photo> or p <photo>\n"
				+ "wall <nPhotos> or w <nPhotos>\n"
				+ "like <photoID> or l <photoID>\n"
				+ "newgroup <groupID> or n <groupID>\n"
				+ "addu <userID> <groupID> or a <userID>\n"
				+ "removeu <userID> <groupID> or r <userID> <groupID>\n"
				+ "ginfo <groupID> or g <groupID>\n"
				+ "ginfo or g\n"
				+ "msg <groupID> <msg> or m <groupID> <msg>\n"
				+ "collect <groupID> or c <groupID>\n"
				+ "history <groupID> or h <groupID>\n"
				+ "show or s\n"
				+ "exit\n"
				+ "__________________________________________________\n"
				+ "**************************************************");
	}


	/**
	 * Prints an incorrect message if input operation does not exist 
	 */
	public void incorrectCommandMessage() {
		System.out.println("Not a valid operation.\nTry again.\nNote: "
				+ "\"show\" or \"s\" to show menu");
	}


	/**
	 * Prints a Bye message when closing the client
	 */
	public void byeMessage() {
		System.out.println("\n__________________________________________________\n"
				+ "**************************************************\n"
				+ "                 See you later!                   \n"
				+ "       InstagramClone All rights reserved!        \n"
				+ "__________________________________________________\n"
				+ "**************************************************\n");
	}

	/**
	 * Continuously asks user for a command and processes given command,
	 * calling specific methods.
	 * 
	 * @param sc scanner to receive input
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void manageOperations(Scanner sc) 
			throws ClassNotFoundException, IOException, NumberFormatException {

		System.out.print("Enter an operation: ");
		String operation = sc.nextLine();

		String[] operationSplit = operation.split(" ");
		String command = operationSplit[0];
		String userId, photoId, groupId;

		while (!command.equals("exit")) { 

			switch(command) {
			case "follow":
			case "f":
				if(operationSplit.length == 2) {
					userId = operationSplit[1]; 
					follow(userId);					
				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: follow <userID> or f <userID>");
				}
				break;
			case "unfollow":
			case "u":
				if(operationSplit.length == 2) {	
					userId = operationSplit[1];
					unFollow(userId);
				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: unfollow <userID> or u <userID>");
				}
				break;
			case "viewfollowers":
			case "v":
				viewfollowers();
				break;
			case "post":
			case "p": 
				if(operationSplit.length == 2) {
					photoId = operationSplit[1]; 
					post(photoId);		
					//if (photoId.contains(".jpg") || photoId.contains(".jpeg") || photoId.contains(".png")) {
						//post(photoId);
					//}
				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: post <photo> or p <photo>");
					System.out.println("Hint: post <photo>.jpg or .jpeg or p <photo>.jpg or p <photo>.jpeg");
				}
				//else {
				//	incorrectCommandMessage();
				//	System.out.println("Hint: post <photo>.jpg or .jpeg or p <photo>.jpg or p <photo>.jpeg");
				//}
				break;
			case "wall":
			case "w":
				if(operationSplit.length == 2) {	
					int nPhotos = Integer.parseInt(operationSplit[1]);
					wall(nPhotos);
				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: wall <nPhotos> or w <nPhotos>");
				}
				break;
			case "like":
			case "l":
				if(operationSplit.length == 2) {
					photoId = operationSplit[1];
					like(photoId);
				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: like <photoID> or l <photoID>");
				}
				break;
			case "newgroup":
			case "n":
				if(operationSplit.length == 2) {
					groupId = operationSplit[1];
					newgroup(groupId);
				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: newgroup <groupID> or n <groupID>");
				}
				break;
			case "addu":
			case "a":
				if(operationSplit.length == 3) {
					userId = operationSplit[1];
					groupId = operationSplit[2];
					addu(userId, groupId);
				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: addu <userID> <groupID> or a <userID> <groupID>");
				}
				break;
			case "removeu":
			case "r":
				if(operationSplit.length == 3) {
					userId = operationSplit[1];
					groupId = operationSplit[2];
					removeu(userId, groupId);
				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: removeu <userID> <groupID> or r <userID> <groupID>");
				}
				break;
			case "ginfo":
			case "g":
				if(operationSplit.length == 2) {	
					groupId = operationSplit[1];
					gInfo(Optional.of(groupId));

				}else if (operationSplit.length == 1){
					gInfo(Optional.empty());

				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: ginfo <groupID> or g <groupID>");
				}
				break;
			case "msg":
			case "m":
				if (operationSplit.length >= 2) {
					groupId = operationSplit[1];
					StringBuilder message = new StringBuilder();
					for(int i = 2; i < operationSplit.length; i++) {
						message.append(operationSplit[i] + " ");
					}
					message.deleteCharAt(message.length()-1);
					msg(groupId, message.toString());	
				}else {
					incorrectCommandMessage();
					System.out.println("Hint: ginfo <groupID> or g <groupID>");
				}
				break;
			case "collect":
			case "c":
				if(operationSplit.length == 2) {
					groupId = operationSplit[1];
					collect(groupId);

				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: collect <groupID> or c <groupID>");
				}
				break;
			case "history":
			case "h":
				if(operationSplit.length == 2) {	
					groupId = operationSplit[1];
					history(groupId);
				}
				else {
					incorrectCommandMessage();
					System.out.println("Hint: history <groupID> or h <groupID>");
				}
				break;
			case "show":
			case "s":
				showOperationMenu();
				break;
			default:
				incorrectCommandMessage();
			}
			System.out.print("\nEnter an operation (\"exit\" to finish): ");
			operation = sc.nextLine();

			operationSplit = operation.split(" ");
			command = operationSplit[0];
		}

		//Send exit to server side
		out.writeObject(command);
		out.flush();
	}

	/**
	 * Asks user to enter a password.
	 * Password must have at least 5 characters 
	 * and must not have spaces
	 * 
	 * @return String password
	 */
	public static String typePassword(Scanner sc) {

		String password = null;
		System.out.print("Enter your password: ");
		password = sc.nextLine();

		while(password.length() < 5 || password.contains(" ")) { //Check if password is a white space
			System.out.println("\nPassword must have at least 5 characters and must not have spaces");
			System.out.print("Enter your password: ");
			password = sc.nextLine();
		}

		return password;
	}
	
	
	/**
	 * Checks if password is strong
	 * A password is strong if has at least 5 characters and does not have spaces
	 * 
	 * @param password the password
	 * @return true if password is strong, false otherwise
	 */
	public static boolean isPasswordStrong(String password) {
		
		return password.trim().length() >=  5 && !password.contains(" ");
	}

}






























