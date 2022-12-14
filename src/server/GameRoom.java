package server;

import java.util.Vector;

import server.GameServer.UserService;

public class GameRoom {
	private String roomNumber;
	private int readyStatusCnt = 0;
	
	public Vector<UserService> userList;
	
	public GameRoom(String roomNumber) {
		this.roomNumber = roomNumber;
		userList = new Vector<UserService>();
	}
	
	public int getReadyStatusCnt() {
		return readyStatusCnt;
	}
	
	public void setReadyStatusCnt(int readyStatusCnt) {
		this.readyStatusCnt = readyStatusCnt;
	}

	public void increaseReadyStatusCnt() {
		readyStatusCnt++;
	}

	public void addPlayerinGameRoom(UserService userSerivce) {
		userList.add(userSerivce);
	}

	public Vector<UserService> getUserList() {
		return userList;
	}

	public void setUserList(Vector<UserService> userList) {
		this.userList = userList;
	}

	public String getRoomNumber() {
		return roomNumber;
	}

}
