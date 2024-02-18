package server;

import java.util.Vector;

public class GameRoom {
    private String roomNumber;
    private int readyStatusCnt = 0;

    public Vector<UserService> userList;

    public GameRoom(String roomNumber) {
        this.roomNumber = roomNumber;
        userList = new Vector(2);
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

    public void addPlayerinGameRoom(UserService userService) {
        userList.add(userService);
    }

    public Vector<UserService> getUserList() {
        return userList;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

}
