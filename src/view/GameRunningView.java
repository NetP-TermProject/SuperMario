package view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Vector;

import audio.Audio;
import controller.Controller;
import controller.OthersController;
import controller.PlayerController;
import main.GamePanel;
import main.GameSettings;
import model.GameCamera;
import model.GameMap;
import model.ObjectDynamic;
import model.Player;

public class GameRunningView implements GameStatusView {

	private Vector<Player> players;
	private Vector<ObjectDynamic> objectDynamic;

	private Player player1;
	private Player player2;

	private GameMap map;
	private GameCamera camera;

	private Audio audio = Audio.getInstance();
	private GamePanel gamePanel;

	private boolean win = false;
	private double animation = 1.0;

	public GameRunningView(GamePanel gamePanel, Controller controller, Controller othersController, int playerNumber) {

		this.gamePanel = gamePanel;

		// 게임 첫 시작 -> 맵 생성, 플레이어 설정
		map = new GameMap();
		players = map.getPlayers();
		camera = map.getCamera();
		objectDynamic = map.getObjectDynamic();

		// players settings (controller and 1p 2p)
		for (int i = 0; i < GameSettings.maxPlayerCount; i++) {
			Player p = players.get(i);
			if (i == playerNumber) {
				p.setController(controller);
				player1 = p; // 카메라 이동을 위해
				((PlayerController) controller).setPlayer(p);
			} else {
				p.setController(othersController);
				player2 = p;
				((OthersController) othersController).setPlayer(p);
			}
		}

		audio.playBackground("smb_background");
	}

	public Vector<Player> getPlayers() {
		return players;
	}

	public void playersInputUpdate() {
		for (Player p : players) {
			p.move();
		}
	}

	public void objectDynamicUpdate() {
		for (ObjectDynamic od : objectDynamic) {
			od.move();
		}
	}

	public void cameraPositionUpdate() {
		camera.moveX(player1.getX() - GameSettings.scaledSize * 5); // 플레이어가 화면 왼쪽에서 6칸 이상을 넘어갈 때, 카메라를 이동시킴
	}

	public void blockAllPlayer() {
		for (Player p : players) {
			p.setControlBlocked(true);
		}
	}

	public void checkPlayer1Won() {
		if (player1.getCenterHitbox().intersects(map.getFlagPole().getHitbox()) && !win) {
			win = true;
			blockAllPlayer();
			audio.play("smb_flagpole");
		}
	}

	public void winAnimation(Graphics2D g) {
		g.setColor(Color.black);
		g.fillRect(0, 0, GameSettings.screenWidth,
				(int) (GameSettings.screenHeight - GameSettings.screenHeight * animation));
		animation -= 0.017;
		if (animation <= 0.0) {
			gamePanel.gameWin();
		}
	}

	@Override
	public void updates() {
		map.playersInputUpdate();
		map.objectDynamicUpdate();

		map.playerCollisionDetection();
		map.objectDynamicCollisionDetection();

		map.dynamicObjectsUpdateCoordinate();

		cameraPositionUpdate();
		checkPlayer1Won();
	}

	@Override
	public void draw(Graphics2D g) {
		g.translate(-camera.getX(), 0); // 그릴 맵 정보들의 원점을 현재 카메라 x좌표만큼 왼쪽으로 이동
		map.draw(g); // 바뀐 원점에서 그려서 카메라가 이동하는 것처럼 보임
		g.translate(camera.getX(), 0); // 원점 원상복구

		// for debug
		g.drawString("player1 x : " + (int) player1.getX() + " xleftvel: " + player1.getxLeftVel()
		+ " yrightVel: " + player1.getxRightVel(), 0, 100);
		g.drawString("player2 x : " + (int) player2.getX() + " xleftvel: " + player2.getxLeftVel()
		+ " yrightVel: " + player2.getxRightVel(), 0, 120);
		g.drawString("koopa x : " + (int) map.koopa.getX(), 0, 140);

		if (win) {
			winAnimation(g);
		}
	}
}
