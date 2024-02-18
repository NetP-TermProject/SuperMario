package server;

import main.GameSettings;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

public class UserService extends Thread {

    private GameServer gameServer = GameServer.getInstance();

    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    private Socket client_socket;
    private String UserName;

    private static Vector<GameRoom> roomVector = new Vector();

    public UserService(Socket client_socket) {
        this.client_socket = client_socket;
        try {
            oos = new ObjectOutputStream(client_socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(client_socket.getInputStream());
        } catch (Exception e) {
            gameServer.AppendText("userService error");
        }
    }

    public void Login() {
        gameServer.AppendText("새로운 참가자 " + UserName + " 서버에 입장.");
    }

    public void Logout() {
        GameServer.UserVec.removeElement(this);
        for (int i = 0; i < roomVector.size(); i++) {
            for (int j = 0; j < roomVector.elementAt(i).userList.size(); j++) {
                if (this == roomVector.elementAt(i).userList.elementAt(j)) {
                    roomVector.elementAt(i).userList.remove(j);
                    WriteRoomListObject(); // 방리스트 갱신
                    break;
                }
            }
        }
        gameServer.AppendText("사용자 " + "[" + UserName + "] 퇴장. 현재 참가자 수 " + GameServer.UserVec.size());
    }

    public void WriteGameStartObject(int roomVectorindex, String roomNumber) {
        long randomSeedNumber = System.nanoTime();
        for (int i = 0; i < roomVector.elementAt(roomVectorindex).userList.size(); i++) {
            UserService sendUser = roomVector.elementAt(roomVectorindex).userList.elementAt(i);
            GameModelMsg objectGameStart = new GameModelMsg(roomNumber, UserName, NetworkStatus.GAME_START, i,
                    randomSeedNumber);
            sendUser.WriteOneObject(objectGameStart);
            gameServer.AppendObject(objectGameStart);
        }
    }

    public void WriteGameLoseObject(int roomVectorindex, String roomNumber) {
        GameModelMsg objectGameLose = null;
        for (int i = 0; i < roomVector.elementAt(roomVectorindex).userList.size(); i++) {
            UserService user = roomVector.elementAt(roomVectorindex).userList.elementAt(i);
            objectGameLose = new GameModelMsg(roomNumber, UserName, NetworkStatus.GAME_LOSE);
            if (user != this)
                user.WriteOneObject(objectGameLose);

        }
        roomVector.remove(roomVectorindex);
        WriteRoomListObject();
        gameServer.AppendObject(objectGameLose);
    }

    public void WriteRoomListObject() {
        StringBuffer roomList = new StringBuffer();
        if (roomVector.isEmpty())
            roomList.append("");
        else {
            for (int i = 0; i < roomVector.size(); i++) {
                String roomNumber = roomVector.elementAt(i).getRoomNumber();
                int userCount = roomVector.elementAt(i).getUserList().size(); // 방 유저 수
                roomList.append(roomNumber + " " + userCount + "/");
            }
        }

        for (int i = 0; i < GameServer.UserVec.size(); i++) {
            UserService user = GameServer.UserVec.elementAt(i);
            GameModelMsg objectGameMsg = new GameModelMsg(roomList.toString(), NetworkStatus.SHOW_LIST);
            user.WriteOneObject(objectGameMsg);
        }
    }

    public void WriteGameButtonMsg(int roomVectorindex, GameModelMsg objectGameMsg) {
        for (int i = 0; i < roomVector.elementAt(roomVectorindex).userList.size(); i++) {
            UserService sendUser = roomVector.elementAt(roomVectorindex).userList.elementAt(i);
            sendUser.WriteOneObject(objectGameMsg);
        }
    }

    public void WriteErrorMsg() {
        GameModelMsg errorMsg = new GameModelMsg(NetworkStatus.ERROR);
        WriteOneObject(errorMsg);
    }

    public void WriteOneObject(Object ob) {
        try {
            oos.writeObject(ob);
        } catch (IOException e) {
            gameServer.AppendText("oos.writeObject(ob) error");
            try {
                ois.close();
                oos.close();
                client_socket.close();
                client_socket = null;
                ois = null;
                oos = null;
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            Logout();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Object obcm = null;
                GameModelMsg objectGameMsg = null;
                if (gameServer.getSocket() == null)
                    break;
                try {
                    obcm = ois.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                if (obcm == null)
                    break;
                if (obcm instanceof GameModelMsg) {
                    objectGameMsg = (GameModelMsg) obcm;
                    gameServer.AppendObject(objectGameMsg);
                } else
                    continue;
                if (objectGameMsg.getCode().matches(NetworkStatus.LOG_IN)) { // 100
                    synchronized (this) {
                        UserName = "player@" + System.nanoTime() + ++GameServer.serverCnt;
                    }
                    objectGameMsg.setPlayerName(UserName);
                    Login();
                    WriteOneObject(objectGameMsg);
                } else if (objectGameMsg.getCode().matches(NetworkStatus.SHOW_LIST)) { // 1200
                    WriteRoomListObject();
                } else if (objectGameMsg.getCode().matches(NetworkStatus.MAKE_ROOM_REQUEST)) { // 1000
                    synchronized (this) {
                        String generatedRoomNumber = objectGameMsg.getRoomNumber() + ++GameServer.roomNumberCnt;
                        GameRoom gameRoom = new GameRoom(generatedRoomNumber);
                        roomVector.add(gameRoom);
                        WriteRoomListObject();
                    }
                } else if (objectGameMsg.getCode().matches(NetworkStatus.LOG_OUT)) { // 200
                    Logout();
                    break;
                } else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_READY)) {// 300
                    for (int i = 0; i < roomVector.size(); i++) {
                        if (roomVector.elementAt(i).getRoomNumber().matches(objectGameMsg.getRoomNumber())) {
                            if (roomVector.elementAt(i).userList.size() == GameSettings.maxPlayerCount) {
                                WriteErrorMsg();
                                break;
                            } else {
                                roomVector.elementAt(i).increaseReadyStatusCnt();
                                roomVector.elementAt(i).addPlayerinGameRoom(this);
                                WriteRoomListObject();
                            }
                        }
                        if (roomVector.elementAt(i).userList.size() == GameSettings.maxPlayerCount
                                && roomVector.elementAt(i).getReadyStatusCnt() == GameSettings.maxPlayerCount) { // 게임 시작 프로토콜
                            WriteGameStartObject(i, objectGameMsg.getRoomNumber());
                            roomVector.elementAt(i).setReadyStatusCnt(0);
                            break;
                        } else {
                            WriteOneObject(objectGameMsg);// 게임 대기 화면 상태 보내기
                        }
                    }
                    gameServer.AppendText(UserName + " 게임준비완료 ");

                } else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_BUTTON)) { // 600
                    for (int i = 0; i < roomVector.size(); i++) {
                        if (roomVector.elementAt(i).getRoomNumber().matches(objectGameMsg.getRoomNumber())) { // 받은

                            WriteGameButtonMsg(i, objectGameMsg);
                            gameServer.AppendText(objectGameMsg.posToString());
                            gameServer.AppendText(objectGameMsg.inputToString());
                        }
                    }
                } else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_WIN)) { // 700 수신
                    for (int i = 0; i < roomVector.size(); i++) {
                        if (roomVector.elementAt(i).getRoomNumber().matches(objectGameMsg.getRoomNumber())) {
                            WriteGameLoseObject(i, objectGameMsg.getRoomNumber()); //800 송신
                        }
                    }
                    WriteOneObject(objectGameMsg);
                }

            } catch (IOException e) {
                gameServer.AppendText("ois.readObject() error");
                try {
                    ois.close();
                    oos.close();
                    client_socket.close();
                    Logout();
                    break;
                } catch (Exception ee) {
                    break;
                } // catch문 끝
            } // 바깥 catch문끝
        } // while
    } // run
}
