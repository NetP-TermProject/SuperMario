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
	private GamePanel gamePanel = GamePanel.getInstance();
	private OthersController otherController = (OthersController) gamePanel.getOthersController();
	private PlayerController playerController = (PlayerController) gamePanel.getController();
	private String roomNumber = "";
	private int playerNum = 0;

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

						// ???????????? ?????? ????????? ??????
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
						gamePanel.gameRunning(objectGameMsg.getRandomSeedNumber());
					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_BUTTON)) {

						if (playerNum != objectGameMsg.getPlayerNum()) {
							// ?????? ?????????
							otherController.getPlayer().setX(objectGameMsg.getX());
							otherController.getPlayer().setY(objectGameMsg.getY());
							// ?????? ?????????
							otherController.getPlayer().setxLeftVel(objectGameMsg.getxLeftVel());
							otherController.getPlayer().setxRightVel(objectGameMsg.getxRightVel());
							otherController.getPlayer().setyVel(objectGameMsg.getyVel());
							// ??? ?????????
							otherController.getPlayer().leftPressed = objectGameMsg.isLeftPressed();
							otherController.getPlayer().rightPressed = objectGameMsg.isRightPressed();
							otherController.getPlayer().upPressed = objectGameMsg.isUpPressed();
						} else if (playerNum == objectGameMsg.getPlayerNum()) {
							playerController.getPlayer().setX(objectGameMsg.getX());
							playerController.getPlayer().setY(objectGameMsg.getY());
							
							playerController.getPlayer().setxLeftVel(objectGameMsg.getxLeftVel());
							playerController.getPlayer().setxRightVel(objectGameMsg.getxRightVel());
							playerController.getPlayer().setyVel(objectGameMsg.getyVel());
							
							playerController.getPlayer().leftPressed = objectGameMsg.isLeftPressed();
							playerController.getPlayer().rightPressed = objectGameMsg.isRightPressed();
							playerController.getPlayer().upPressed = objectGameMsg.isUpPressed();
						}
					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_LOSE)) {
						gamePanel.gameLose();
						SendLogoutMessage();
						break; // ??????
					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_WIN)) {
						SendLogoutMessage();
						break; // ??????
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
					} // catch??? ???
				} // ?????? catch??????
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
		// ????????? ??? ?????? ??? ??????
		GameModelMsg objectGameMsg = new GameModelMsg(playerNum, roomNumber, userName, NetworkStatus.GAME_BUTTON, x, y,
				xLeftVel, xRightVel, yVel, upPressed, downPressed, leftPressed, rightPressed, spacePressed);
		SendObject(objectGameMsg);
	}

	public void SendWinMessage() {
		GameModelMsg obejctGameMsg = new GameModelMsg(roomNumber, userName, NetworkStatus.GAME_WIN);
		SendObject(obejctGameMsg);
	}

	public void SendReadyMessage(String roomNumber) {
		this.roomNumber = roomNumber; // ?????? ????????? ??????
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
