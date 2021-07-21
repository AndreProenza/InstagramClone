package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import exceptions.IncorrectArgumentsInstagramCloneServerException;
import exceptions.WrongPasswordException;
import handlers.FileManagerServer;
import handlers.OperationMenuServer;
import handlers.UserManager;

public class InstagramCloneServer {

	public static void main(String[] args) {
		System.out.println("Start InstagramCloneServer...");
		InstagramCloneServer server = new InstagramCloneServer();
		try {
			//InstagramCloneServer 45678
			if (args.length != 1) {
				throw new IncorrectArgumentsInstagramCloneServerException("Incorrect parameters.");
			}
			int port = Integer.parseInt(args[0]);
			server.startServer(port);
		}
		catch(IncorrectArgumentsInstagramCloneServerException | NumberFormatException e) {
			System.out.println("Fail Starting Server. " + e);
			System.out.println("Usage: InstagramCloneServer <port>\nUsage Example: InstagramCloneServer 45678");
		}
	}

	@SuppressWarnings("resource")
	public void startServer (int port) {
		ServerSocket serverSocket = null; 

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		while (true) {
			try {
				Socket socket = serverSocket.accept();
				ServerThread newServerThread = new ServerThread(socket);
				newServerThread.start();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		//serverSocket.close();
	}

	//Threads used to comunicate with clients
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				
				Thread.sleep(100);
				try {
					String userId = (String) in.readObject();
					String password = (String) in.readObject();
					
					//Do login
					UserManager userManager = new UserManager(); 				
					
					int login = userManager.login(userId, password);
					// 0 wrong password, 1 userNotFound, 2 everything fine.
					if(login == 2) { 
						//Send true authentication to client side
						out.writeObject("login");
						out.flush();
						System.out.println("User: " + userId + " Autenticated");
					}
					
					else if (login == 1) { //User not found proceed to Registration
						//Inform client side of the registration
						out.writeObject("registration");
						out.flush();
						
						//Thread.sleep(5*1000);//Waiting time?? Semaphore??	
						String userName = (String) in.readObject();		
						
						userManager.register(userId, userName, password);
						
						out.writeObject("registrationComplete");
						out.flush();
						System.out.println("User: " + userId + " Registered.");
						
						//Setup Client Directory
						FileManagerServer.createClientDirectory(userId);
					}
					else {
						out.writeObject("wrongPassword");
						System.out.println("User: " + userId + " Wrong Password.");
						throw new WrongPasswordException();
					}
					
					//Receive and process operations from client side
					//while does not receive exit from client
					OperationMenuServer operationMenuServer = new OperationMenuServer(out, in);
					operationMenuServer.receiveAndProcessOperations();
					
					System.out.println("User: " + userId + " finished session!");
					
				} catch (ClassNotFoundException | WrongPasswordException e) {
					//Does nothing
				}

				out.close();
				in.close();

				socket.close(); 
				//CATCH EOFException to prevent  server breaking when clients break
			} catch (IOException | InterruptedException e ) { 
				
			}
		}
	}
}
