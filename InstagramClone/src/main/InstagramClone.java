package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import exceptions.IncorrectArgumentsInstagramCloneException;
import exceptions.WeakPasswordException;
import handlers.FileManagerClient;
import handlers.OperationMenu;

public class InstagramClone {
	
	private static Scanner sc;

	public static void main(String[] args) {
		System.out.println("Start InstagramClone Client...");
	    sc = new Scanner(System.in);
	    
		try {
			String userId;
			String userPassword;
			
			if(args.length == 1) {  
				if(args[0].contains(":")) {
					sc.close();
					throw new IncorrectArgumentsInstagramCloneException(); 
				}
			}

			if(args.length > 3 || args.length < 1 ) {
				sc.close();
				throw new IncorrectArgumentsInstagramCloneException();
			}
			if(args[0].contains(":")) { //ServerAdress visible

				//serverAdress Format: 127.0.0.1:45678 OR localhost:45678 
				String[] serverAdress = args[0].split(":");
				String adress = serverAdress[0];
				int port = Integer.parseInt(serverAdress[1]);
				userId = args[1];
				
				if(args.length == 3) {
					userPassword = args[2];
					if(!OperationMenu.isPasswordStrong(userPassword)) {
						throw new WeakPasswordException();
					}
					// InstagramClone <serverAddress> <clientID> [password] 				
					startClient(adress, port, userId, userPassword);
				}
				else if(args.length == 2) {
					userPassword = OperationMenu.typePassword(sc);
					//InstagramClone <serverAdress> <clientID>
					startClient(adress, port, userId, userPassword);
				}
			}
			else { //ServerAdress hidden
				userId = args[0]; 

				if(args.length == 2) { //With password
					userPassword = args[1];
					if(!OperationMenu.isPasswordStrong(userPassword)) {
						throw new WeakPasswordException();
					}
					//InstagramClone <clientID> <password>
					startClient("127.0.0.1", 45678, userId, userPassword);
				}
				else if(args.length == 1) {//Without password
				
					userPassword = OperationMenu.typePassword(sc);
				
					//InstagramClone <clientID>
					startClient("127.0.0.1", 45678, userId, userPassword);
				}
				else {
					sc.close();
					throw new IncorrectArgumentsInstagramCloneException();
				}
			}
			
		} catch(IncorrectArgumentsInstagramCloneException | NumberFormatException e) {
			System.out.println("Fail Starting Server. " + e);
			System.out.println("Usage: InstagramClone <serverAddress> <clientID> <password>");
			System.out.println("Usage Example: 127.0.0.1:45678 John helloWorld");
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			//Does nothing
		} catch (WeakPasswordException e) {
			System.out.println("Weak password\n"
					+ "Password must have at least 5 characters and must not have spaces");
		}
	}

	public static void startClient(String adress, int port, String userId, String password) 
			throws IOException, ClassNotFoundException, InterruptedException {

		Socket clientSocket = new Socket(adress, port);

		ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
		ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
		
		
		//Send to server clientId and password
		out.writeObject(userId);
		out.writeObject(password);
		out.flush();

		//Wait server authentication
		String responseFromServer = (String) in.readObject();

		if(responseFromServer.equals("login")) {
			System.out.println("Welcome again to InstagramClone.");
		}
		else if(responseFromServer.equals("registration")) {
			System.out.println("User not found.\nPlease Register!");

			System.out.print("Enter a user name: ");
			String userName = sc.nextLine();

			out.writeObject(userName);
			out.flush();
			
			responseFromServer = (String) in.readObject();
			if(responseFromServer.equals("registrationComplete")) {
				System.out.println("Welcome to InstagramClone.");
				
				//Setup Client Directory
				FileManagerClient.createClientDirectory(userId);
			}
		}
		else {
			System.out.println("User and password dont match.\nPlease try again.");
			System.exit(-1);
		}

		//Choose an operation from the menu
		//"exit" to stop choosing operations
		OperationMenu menu = new OperationMenu(out, in, userId);
		menu.showOperationMenu();
		menu.manageOperations(sc);
		
		sc.close();
		menu.byeMessage();
		out.close();
		in.close();

		clientSocket.close();	
	}
}
