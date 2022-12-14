package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import client.GameClient;
import main.GameSettings;

public class GameReadyView implements GameStatusView {

	private ImageLoader imageLoader = ImageLoader.getImageLoader();
	
	private String msg = "WAITING OTHER PLAYER...";
	private int fontSize = 15;	
	private Font font = FontLoader.getInstance().loadMarioFont();
	
	private boolean isPlayer1 = false;
	
	public GameReadyView(GameClient gameClient , boolean isPlayer1) {
		this.isPlayer1 = isPlayer1;
	}
	
	@Override
	public void updates() {
		
	}

	@Override
	public void draw(Graphics2D g) {
		g.setColor(Color.black);
		g.fillRect(0, 0, GameSettings.screenWidth, GameSettings.screenHeight);
		g.setColor(Color.white);	
		g.setFont(font.deriveFont(20f));
		g.drawString(msg,
				GameSettings.screenWidth / 2 - msg.length() / 2 * fontSize,  
				GameSettings.screenHeight/2 - 48);
		g.drawImage(imageLoader.getPlayerStartImage(isPlayer1), 
				GameSettings.screenWidth/2 - GameSettings.scaledSize/2, 
				GameSettings.screenHeight/2, 
				GameSettings.scaledSize, GameSettings.scaledSize, null);
	}

}
