package main;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import audio.Audio;
import client.GameClient;
import controller.Controller;
import controller.OthersController;
import controller.PlayerController;
import view.GameLoseView;
import view.GameReadyView;
import view.GameRoomListView;
import view.GameRunningView;
import view.GameStatusView;
import view.GameWinView;
import view.StartScreenView;

public class GamePanel extends JPanel implements Runnable {

	private static GamePanel instance = new GamePanel();

	private static final long serialVersionUID = 1L;
	private Thread gameThread;

	private Controller controller; // 플레이어의 input값
	public Controller getController() {
		return controller;
	}

	private Controller othersController; // 다른 플레이어의 input data값

	private int playerNumber; // 서버한테 받은 클라이언트 번호

	private GameStatusView gameStatusView; // 시작화면, 인게임 화면 --> GameStatusView로 캡슐화
	private GameClient gameClient;

	private String ip_addr = "127.0.0.1";
	private String port = "30000";

	private GamePanel() {
		controller = new PlayerController(); // 플레이어의 input값
		othersController = new OthersController(); // 다른 플레이어의 input data값

		this.setPreferredSize(new Dimension(GameSettings.screenWidth, GameSettings.screenHeight));
		this.setDoubleBuffered(true);
		this.addKeyListener(controller);
		this.setFocusable(true);

		gameStartScreen();
	}

	public void initControllers() {
		controller = new PlayerController();
		othersController = new OthersController();
		this.addKeyListener(controller);
	}

	public Controller getOthersController() {
		return othersController;
	}

	public void setPlayerNumber(int playerNumber) {
		this.playerNumber = playerNumber;
	}

	public GameClient getClient() {
		return gameClient;
	}

	public void setGameStatusView(GameStatusView gameStatusView) {
		this.gameStatusView = gameStatusView;
	}

	public void gameStart() {
		gameThread = new Thread(this);
		gameThread.start();
		gameClient = new GameClient("player", ip_addr, port);
		((PlayerController) controller).setGameClient(gameClient); // controller에 gameClient 등록
	}

	public boolean isGameRoomView() {
		return (gameStatusView instanceof GameRoomListView);
	}
	
	public void gameRoomMake() {
		if (!(gameStatusView instanceof GameRoomListView)) {
			setGameStatusView(new GameRoomListView(this, gameClient, null));
			gameClient.SendShowRoomListMessage();
		}
	}

	public void updateGameRoomList(String roomList[]) {
		if(gameStatusView instanceof GameRoomListView)
			((GameRoomListView) gameStatusView).updateRoomListView(roomList);
	}
	
	public void errorMsgGameRoomList() {
		if(gameStatusView instanceof GameRoomListView)
			((GameRoomListView) gameStatusView).errorMsg();
	}
	
	public void gameReady(boolean isPlayer1) {
		if (!(gameStatusView instanceof GameReadyView)) { // ready 두번 보내지는 문제 방지
			setGameStatusView(new GameReadyView(gameClient, isPlayer1));
		}
	}

	public void gameRunning() {
		controller.initKey();
		othersController.initKey();

		// 게임이 시작하면 게임 스레드 초기화하여 동기화
		gameThread.interrupt();
		
		gameThread = null;
		gameThread = new Thread(this);
		gameThread.start();

		// gameStatus == 게임 중
		setGameStatusView(new GameRunningView(this, controller, othersController, playerNumber));
	}

	public void gameStartScreen() {
		controller.initKey();
		// gameStatus == 시작 화면
		setGameStatusView(new StartScreenView(this, controller));
	}

	public void gameWin() {
		gameClient.SendWinMessage();
		gameClient.SendLogoutMessage();
		setGameStatusView(new GameWinView(this, controller));
	}

	public void gameLose() {
		setGameStatusView(new GameLoseView(this, controller));
	}

	public void gameRestart() {
		Audio.getInstance().closeAll();
		// 스레드 종료
		gameThread.interrupt();

		// 초기화
		controller = null;
		othersController = null;
		gameStatusView = null;
		gameClient = null;
		gameThread = null;

		initControllers();
		gameStartScreen();
		gameStart(); // 재시작
	}

	public void update() {
		gameStatusView.updates();
		repaint();
	}

	@Override
	public void run() {
		double fps = 60.0;
		double interval = 1000000000 / fps;
		double delta = 0;

		long lastTime = System.nanoTime();
		long currentTime;

		while (gameThread != null) {
			currentTime = System.nanoTime();
			delta += (currentTime - lastTime) / interval;
			lastTime = currentTime;

			// draw every 1/60 second
			if (delta >= 1) {
				update();
				delta--;
			}

			if (Thread.currentThread().interrupted())
				break;
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		gameStatusView.draw(g2);

		g2.dispose();
	}

	public synchronized static GamePanel getInstance() {
		return instance;
	}
}
