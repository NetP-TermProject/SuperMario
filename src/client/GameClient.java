package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import controller.OthersController;
import controller.PlayerController;
import main.GamePanel;
import server.GameModelMsg;
import server.NetworkStatus;

public class GameClient {
	private String userName;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private Socket socket;
	// private ListenNetwork net;
	private GamePanel gamePanel = GamePanel.getInstance();
	private OthersController otherController = (OthersController) gamePanel.getOthersController();
	private PlayerController playerController = (PlayerController) gamePanel.getController();
	private int playerNum = 0;

	public int getPlayerNum() {
		return playerNum;
	}

	private String roomNumber = "";

	private Thread net;

	public String getRoomNumber() {
		return roomNumber;
	}

	public GameClient(String username, String ip_addr, String port_no) {
		try {
			socket = new Socket(ip_addr, Integer.parseInt(port_no));
			oos = new ObjectOutputStream(socket.getOutputStream());
			oos.flush();
			ois = new ObjectInputStream(socket.getInputStream());

			SendLoginMessage(username);

			net = new Thread(new ListenNetwork());
			net.start();

		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	public String getUserName() {
		return userName;
	}

	class ListenNetwork implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {

					Object obgm = null;
					GameModelMsg objectGameMsg;
					try {
						obgm = ois.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						break;
					}
					if (obgm == null)
						break;
					if (obgm instanceof GameModelMsg) {
						objectGameMsg = (GameModelMsg) obgm;
					} else
						continue;
					if (objectGameMsg.getCode().matches(NetworkStatus.ERROR)) {
						gamePanel.errorMsgGameRoomList();
					} else if (objectGameMsg.getCode().matches(NetworkStatus.LOG_IN)) {
						userName = objectGameMsg.getPlayerName();
					} else if (objectGameMsg.getCode().matches(NetworkStatus.SHOW_LIST)) {

						// 방리스트 뷰가 아니면 무시
						if (!gamePanel.isGameRoomView())
							continue;

						if (objectGameMsg.getRoomList().matches(""))
							gamePanel.updateGameRoomList(null);
						else {
							String roomList[] = null;
							roomList = objectGameMsg.getRoomList().split("/");
							gamePanel.updateGameRoomList(roomList);
						}
					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_READY)) {
						// gamePanel.gameReady(true);
					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_START)) {
						roomNumber = objectGameMsg.getRoomNumber();
						playerNum = objectGameMsg.getPlayerNum();
						gamePanel.setPlayerNumber(playerNum);
						System.out.println(playerNum);
						gamePanel.gameRunning();
					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_BUTTON)) {
						if (playerNum != objectGameMsg.getPlayerNum()) { // 다른 플레이어한테 키보드 받음
							System.out.println("other" + playerNum);
							// 좌표 동기화
							otherController.getPlayer().setX(objectGameMsg.getX());
							otherController.getPlayer().setY(objectGameMsg.getY());
							otherController.getPlayer().leftPressed = objectGameMsg.isLeftPressed();
							otherController.getPlayer().rightPressed = objectGameMsg.isRightPressed();
							otherController.getPlayer().upPressed = objectGameMsg.isUpPressed();
							// otherController.getPlayer().setyVel(objectGameMsg.getyVel());

//							otherController.setKeyPressed(objectGameMsg.isUpPressed(), objectGameMsg.isDownPressed(),
//									objectGameMsg.isLeftPressed(), objectGameMsg.isRightPressed(),
//									objectGameMsg.isSpacePressed());
//							otherController.getPlayer().setxLeftVel(objectGameMsg.getxLeftVel());
//							otherController.getPlayer().setxRightVel(objectGameMsg.getxRightVel());
//							otherController.getPlayer().setyVel(objectGameMsg.getyVel());
//							otherController.getPlayer().move();
						}
						if (playerNum == objectGameMsg.getPlayerNum()) { // 내가 보낸 키보드 값
							// 좌표 동기화
							System.out.println("me" + playerNum);
							playerController.getPlayer().setX(objectGameMsg.getX());
							playerController.getPlayer().setY(objectGameMsg.getY());

							playerController.getPlayer().leftPressed = objectGameMsg.isLeftPressed();
							playerController.getPlayer().rightPressed = objectGameMsg.isRightPressed();
							playerController.getPlayer().upPressed = objectGameMsg.isUpPressed();
							// playerController.getPlayer().setyVel(objectGameMsg.getyVel());
//							playerController.setKeyPressed(objectGameMsg.isUpPressed(), objectGameMsg.isDownPressed(),
//									objectGameMsg.isLeftPressed(), objectGameMsg.isRightPressed(),
//									objectGameMsg.isSpacePressed());
//							playerController.getPlayer().setxLeftVel(objectGameMsg.getxLeftVel());
//							playerController.getPlayer().setxRightVel(objectGameMsg.getxRightVel());
//							playerController.getPlayer().setyVel(objectGameMsg.getyVel());
//							playerController.getPlayer().move();
						}

						// 속도 동기화

						// 키 동기화

					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_LOSE)) {
						gamePanel.gameLose();
						SendLogoutMessage();
						break; // 종료
					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_WIN)) {
						SendLogoutMessage();
						break; // 종료
					}

				} catch (IOException e) {
					try {
						SendLogoutMessage();

						ois.close();
						oos.close();
						socket.close();

						break;
					} catch (Exception ee) {
						break;
					} // catch문 끝
				} // 바깥 catch문끝
			}
		}
	}

	public void SendObject(GameModelMsg objectGameMsg) {
		try {
			oos.writeObject(objectGameMsg);
		} catch (IOException e) {

		}
	}

	public void SendButtonAction(String roomNumber, double x, double y, double xLeftVel, double xRightVel, double yVel,
			boolean upPressed, boolean downPressed, boolean leftPressed, boolean rightPressed, boolean spacePressed) {
		// 좌표와 키 입력 값 보냄
		GameModelMsg objectGameMsg = new GameModelMsg(playerNum, roomNumber, userName, NetworkStatus.GAME_BUTTON, x, y,
				xLeftVel, xRightVel, yVel, upPressed, downPressed, leftPressed, rightPressed, spacePressed);
		SendObject(objectGameMsg);
	}

	public void SendWinMessage() {
		GameModelMsg obejctGameMsg = new GameModelMsg(roomNumber, userName, NetworkStatus.GAME_WIN);
		SendObject(obejctGameMsg);
	}

	public void SendReadyMessage(String roomNumber) {
		this.roomNumber = roomNumber; // 유저 방번호 저장
		GameModelMsg gameReadMsg = new GameModelMsg(roomNumber, userName, NetworkStatus.GAME_READY);
		SendObject(gameReadMsg);
	}

	public void SendLoginMessage(String username) {
		GameModelMsg objectGameMsg = new GameModelMsg(username, NetworkStatus.LOG_IN);
		SendObject(objectGameMsg);
	}

	public void SendLogoutMessage() {
		GameModelMsg objectGameMsg = new GameModelMsg(userName, NetworkStatus.LOG_OUT);
		SendObject(objectGameMsg);
	}

	public void SendShowRoomListMessage() {
		GameModelMsg showListMsg = new GameModelMsg(NetworkStatus.SHOW_LIST);
		SendObject(showListMsg);
	}

	public void SendMakeRoomRequestMessage() {
		long roomNumber = System.nanoTime();
		GameModelMsg gameReadMsg = new GameModelMsg(String.valueOf(roomNumber), userName,
				NetworkStatus.MAKE_ROOM_REQUEST);
		SendObject(gameReadMsg);
	}
}
