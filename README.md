# InstagramClone

InstagramClone is an application that aims to simulate Instagram.
It's a client-server type system that allows users (clients) to use a central server for sharing photos and communicating with other users. 

The system supports two modes of functioning. The Feed Mode (wall) and the Chat Mode. In Feed mode (wall), users post photos on their public profile saved on the server. Users can follow any other users, and can see photos posted by others on their feed (wall). 

The application also allows you to drop and remove a like from these photos. In Chat Mode, users can send messages to private groups of users, and read messages sent to the groups they belong to. Each user has an account on the server,  can follow any other user freely, and can belong to several private access groups. 

The application has no instagram lookalike interface. It's only java backend, with a terminal interface.

---

## Project Goal

Develop and practice fundamental security and reliability concepts in secure distributed applications, where server and clients run in sandboxes (server.policy and client.policy) 

---

## Main Activities

- Plan and design project architecture. 
- Development using (JSE) and Java Security API

---

## Technologies

- Java SE
- Java Security API
- Bash

---

## Instructions to Execute

1 - unzip project

2 - There are two different ways to run the project

> ***Note to run clients***: You can register or login with any name and password you want. Example: name: John, Password: helloWorld  
> Format to run the program: \<other arguments> John helloWorld  
> You can change the parameters John and helloWorld as much as you want, but keep the remaining parameters as shown below.

2.1 - Run with Jar:

- To run the server:
go to Jars/InstagramCloneServer/ open a terminal and write:
```java
java -Djava.security.manager -Djava.security.policy=server.policy -jar InstagramCloneServer.jar 45678
```

- To run a client:
go to Jars/InstagramClone/ open a terminal and write:
```java
java -Djava.security.manager -Djava.security.policy=client.policy -jar InstagramClone.jar 127.0.0.1:45678 John helloWorld
```
				
- Notes:
To reset all files generated by InstagramCloneServer and by InstagramClone:
go to the Jars directory/ open a terminal and write: 
```bash
bash virgin.sh or ./virgin.sh
```
					
					
2.2 - Run with eclipse:

- Open two projects in eclipse for InstagramClone and for InstagramCloneServer
- To run the server: 
Run configurations with arguments: 
```java
45678
``` 
and VM arguments: 
```java
-Djava.security.manager -Djava.security.policy=server.policy
```

- To run a client: 
Run configurations with arguments: 
```java
127.0.0.1:45678 John helloWorld
```
and VM arguments: 
```java
-Djava.security.manager -Djava.security.policy=client.policy
```

- Notes:
To reset all files generated by InstagramCloneServer and by InstagramClone:
go to the InstagramClone-master directory/ open a terminal and write: 
```bash
bash virgin.sh or ./virgin.sh
```

---

## Instructions to request an action on the application terminal

| Instruction                      | Function                            
|:--------------------------------:|:--------------------------------
| follow \<userID>                 | adds the client (clientID) to the list of followers of the user userID.
| unfollow \<userID>               | removes the client (clientID) from the userID user list of followers.
| viewfollowers                    | get the list of the customer's followers.
| post \<photo>                    | sends a photograph (photo) to the user profile stored in the server.
| wall \<nPhotos>                  | receives the latest nPhotos photos posted by users followed, as well as the number of likes of each photo (showing everything on the wall).
| newgroup \<groupID>              | creates a private group, whose owner will be the client that created it.
| addu \<userID> \<groupID>        | adds the user userID as a member of the specified group. Only group owners can add users to their groups
| removed \<userID> \<groupID>     | removes the user userID from the specified group. Only the group owners can remove users from their groups
| ginfo \<groupID>                 | if groupID is not specified, displays a list of groups from which the client owns, and a list of the groups to which it belongs. If specified the groupID, shows the owner of the group and a list of the group members.
| msg \<groupID> \<msg>            | sends a message (msg) to the group groupID, which will remain saved in a group mailbox on the server. The message will be accessible to group members via the collect command. 
| collect \<groupID>               | receive all messages that have been sent to the group groupID and that the client has not yet received. For example, if the message box from the group has 3 messages (m1, m2, m3), if user u1 has already received the messages m1 and m2 and user u2 hasn't received any yet, so command execution by user u1 will only return m3, but if executed by user u2 it will return 3 posts. If there is no new message, this fact should be noted. Users only have access to messages sent after joining the group. When a message is read by all users, it is removed from the mailbox and placed in a group history. Group owners count as members for the purpose of removing messages from the mailbox, ie the owners they also receive the messages they themselves sent.
| history \<groupID>               | shows the mailbox history of the indicated group that the client has already read previously.
