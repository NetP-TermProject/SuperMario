package controller;

import java.awt.event.KeyEvent;

import client.GameClient;
import model.Player;

public class PlayerController implements Controller {

	private boolean upPressed, downPressed, leftPressed, rightPressed;
	private boolean spacePressed;

	private GameClient gameClient;
	private Player player;

	public void setPlayer(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public void setGameClient(GameClient gameClient) {
		this.gameClient = gameClient;
	}

	@Override
	public void initKey() {
		this.upPressed = false;
		this.downPressed = false;
		this.leftPressed = false;
		this.rightPressed = false;
		this.spacePressed = false;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();

		if (code == KeyEvent.VK_W) {
			if (upPressed)
				return; // 한번만 보내기
			upPressed = true;
		}
		if (code == KeyEvent.VK_S) {
			if (downPressed)
				return;
			downPressed = true;
		}
		if (code == KeyEvent.VK_A) {
			if (leftPressed)
				return;
			leftPressed = true;
		}
		if (code == KeyEvent.VK_D) {
			if (rightPressed)
				return;
			rightPressed = true;
		}
		if (code == KeyEvent.VK_SPACE) {
			if (spacePressed)
				return;
			spacePressed = true;
		}
		// 키 누르면 전송
		if (player != null && !player.isControlBlocked())
			gameClient.SendButtonAction(gameClient.getRoomNumber(), player.getX(), player.getY(), player.getxLeftVel(),
					player.getxRightVel(), player.getyVel(), upPressed, downPressed, leftPressed, rightPressed,
					spacePressed);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int code = e.getKeyCode();

		if (code == KeyEvent.VK_W) {
			upPressed = false;
		}
		if (code == KeyEvent.VK_S) {
			downPressed = false;
		}
		if (code == KeyEvent.VK_A) {
			leftPressed = false;
		}
		if (code == KeyEvent.VK_D) {
			rightPressed = false;
		}
		if (code == KeyEvent.VK_SPACE) {
			spacePressed = false;
		}
		// 키 떼면 전송
		if (player != null)
			gameClient.SendButtonAction(gameClient.getRoomNumber(), player.getX(), player.getY(), player.getxLeftVel(),
					player.getxRightVel(), player.getyVel(), upPressed, downPressed, leftPressed, rightPressed,
					spacePressed);
	}

	@Override
	public boolean getUpPressed() {
		return upPressed;
	}

	@Override
	public boolean getDownPressed() {
		return downPressed;
	}

	@Override
	public boolean getLeftPressed() {
		return leftPressed;
	}

	@Override
	public boolean getRightPressed() {
		return rightPressed;
	}

	@Override
	public boolean getSpacePressed() {
		return spacePressed;
	}

	public void setKeyPressed(boolean upPressed, boolean downPressed, boolean leftPressed, boolean rightPressed,
			boolean spacePressed) {
		this.upPressed = upPressed;
		this.downPressed = downPressed;
		this.leftPressed = leftPressed;
		this.rightPressed = rightPressed;
		this.spacePressed = spacePressed;
	}

}
