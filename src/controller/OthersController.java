package controller;

import java.awt.event.KeyEvent;

import model.Player;

public class OthersController implements Controller {

	private boolean upPressed, downPressed, leftPressed, rightPressed;
	private boolean spacePressed;
	
	private Player player;
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	public Player getPlayer() {
		return player;
	}
	
	@Override
	public void initKey() {
		this.upPressed = false;
		this.downPressed = false;
		this.leftPressed = false;
		this.rightPressed = false;
		this.spacePressed = false;
	}

	public void setKeyPressed(boolean upPressed, boolean downPressed, boolean leftPressed, boolean rightPressed,
			boolean spacePressed) {
		this.upPressed = upPressed;
		this.downPressed = downPressed;
		this.leftPressed = leftPressed;
		this.rightPressed = rightPressed;
		this.spacePressed = spacePressed;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		
	}

	@Override
	public void keyReleased(KeyEvent e) {

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
}
