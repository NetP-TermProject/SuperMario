package model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Vector;

import audio.Audio;
import main.GameSettings;
import view.ImageLoader;

public class GameMap {
	private BufferedImage background;
	private Vector<GameObject> deletedObjects = new Vector<GameObject>();
	private Vector<ObjectDynamic> objectDynamic = new Vector<ObjectDynamic>();
	private Vector<ObjectStatic> objectStatic = new Vector<ObjectStatic>();
	private Vector<Player> players = new Vector<Player>();
	private FlagPole flagPole;
	private GameCamera camera;
	private Audio audio = Audio.getInstance();

	public EnemyKoopa koopa; // for debug

	public GameMap() {
		background = ImageLoader.getImageLoader().getBackgroundImage();
	}
	
	public FlagPole getFlagPole() {
		return flagPole;
	}

	public void setFlagPole(FlagPole flagPole) {
		this.flagPole = flagPole;
	}

	public GameCamera getCamera() {
		return camera;
	}
	
	public void setCamera(GameCamera camera) {
		this.camera = camera;
	}
	
	public Vector<Player> getPlayers() {
		return players;
	}

	public Vector<ObjectDynamic> getObjectDynamic() {
		return objectDynamic;
	}

	public Vector<ObjectStatic> getObjectStatic() {
		return objectStatic;
	}
	
	public void addObject(Player p) {
		players.add(p);
	}
	
	public void addObject(ObjectDynamic od) {
		objectDynamic.add(od);
	}
	
	public void addObject(ObjectStatic os) {
		objectStatic.add(os);
	}

	public ObjectStatic createBlockedBlock(double x, double y) {
		return new BlockBlocked(x, y);
	}

	public ObjectDynamic createItem(int itemNum, double x, double y) {
		ObjectDynamic od = null;
		switch (itemNum) {
		case 1:
			od = new ItemMushroom(x, y, background.getWidth());
			break;

		case 2:
			od = new ItemCoin(x, y, background.getWidth());
			break;
		}
		return od;
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

	public void playerCollisionDetection() {

		for (Player p : players) {
			if (!p.hasCollision())
				continue;

			p.setCollided(false); // ?????? ????????? ??????????????? ???????????? ??????

			// ????????? ???????????? (?????? ???)?????? ?????? ??????
			for (int i = 0; i < objectStatic.size(); i++) {

				ObjectStatic os = objectStatic.get(i);
				if (!p.isItInHitboxSpace(os.x, os.y)) // ???????????? ????????? ?????? ?????? ?????? ?????????
					continue;

				// ??????????????? ????????? ????????? ???????????? ???
				if (p.getTopHitbox().intersects(os.getHitbox())) {
					p.setY(os.getY() + os.getHeight());
					p.setyVel(GameSettings.gravity);

					audio.play("smb_bump");
					int itemNum = os.touch();
					if (itemNum != 0) {
						objectStatic.set(i, createBlockedBlock(os.getX(), os.getY()));
						objectDynamic.add(createItem(itemNum, os.getX(), os.getY()));
					}
				}

				// ??????????????? ?????? ?????? ??? ?????? ???
				else if (p.getBottomHitbox().intersects(os.getHitbox())) {
					p.landing(os.getY() + 3);
					p.setCollided(true); // ?????? ?????? ????????? ?????????
				}

				// ??????????????? ?????? ????????? ???????????? ???
				else if (p.getCenterHitbox().intersects(os.getHitbox())) {
					p.setxLeftVel(0);
					p.setxRightVel(0);
				}
			}

			// ????????? ????????? ???????????? ?????????, ??? ????????? ????????? ????????????
			if (!p.isCollided()) {
				p.setyGround(625.0); // ?????? ?????? ??????????????? yGround ??????
			}

			// ????????? ???????????? (???, ????????? ???)?????? ?????? ??????
			for (ObjectDynamic od : objectDynamic) {

				if (!od.hasCollision() || !p.isItInHitboxSpace(od.x, od.y))
					continue;

				// ??????????????? ????????? ???
				if (p.getBottomHitbox().intersects(od.getHitbox()) && !od.isItem()) {
					p.initVelocity();
					if (od.getObjectNum() == 10) // ??????(?????????)??? ???
						p.kick(11);
					else
						p.stomp(11);
					od.attacked((int) p.getX());
				}
				// ??????????????? ????????? ???
				else if (p.getCenterHitbox().intersects(od.getHitbox())) {
					switch (od.getObjectNum()) {
					case 1: // ??????
						p.startSpeedUp();
						od.setDestroy(true);
						break;
					case 2: // ??????
						break;
					case 10: // ??????
						if (!od.isMoving()) {
							audio.play("smb_kick");
							p.initVelocity();
							od.attacked((int) p.getX() + p.getWidth() / 2);
						} else
							p.die();

						break;
					default: // ?????????
						p.die();
						break;
					}
				}
			}

			// ???????????? ?????? ?????? ??????
			for (Player pJ : players) {
				if (p == pJ || !pJ.hasCollision())
					continue;

				// ?????? ??????????????? ????????? ???
				if (p.getBottomHitbox().intersects(pJ.getTopHitbox())) {
					p.initVelocity();
					p.stomp(13);
					pJ.attacked(0);
				}
			}

			// ????????? ??????
			addDeletedObjects();
			clearDeletedObjects();
		}
	}

	public void objectDynamicCollisionDetection() {
		for (ObjectDynamic od : objectDynamic) {

			if (!od.hasCollision())
				continue;

			od.setCollided(false);

			for (ObjectStatic os : objectStatic) {

				if (!od.isItInHitboxSpace(os.x, os.y)) // ????????? ?????? ?????? ?????? ?????????
					continue;
				
				if (od.getHitbox().intersects(os.getHitbox())) {
					od.changeDir();
				}

				else if (od.getBottomHitbox().intersects(os.getHitbox())) {
					od.landing(os.getY() + 3);
					od.setCollided(true);
				}
			}

			if (!od.isCollided()) {
				od.setyGround(625.0);
			}
			
			for(ObjectDynamic od2 : objectDynamic) {
				if (!od2.hasCollision() || od == od2 || od2.objectNum != 10 || Math.abs(od2.getxLeftVel()) < 7)
					continue;
				if (od.getHitbox().intersects(od2.getHitbox())) {
					if(od2.getxLeftVel() < 0)
						od.attacked(-1);
					else 
						od.attacked(1);
				}
			}
		}
	}

	public void dynamicObjectsUpdateCoordinate() {
		for (Player p : players) {
			p.updatesCoordinate();
		}
		for (ObjectDynamic od : objectDynamic) {
			od.updatesCoordinate();
		}
	}

	public void addDeletedObjects() {
		for (GameObject go : objectDynamic) {
			if (go.isDestroy()) {
				deletedObjects.add(go);
			}
		}
		for (GameObject go : objectStatic) {
			if (go.isDestroy()) {
				deletedObjects.add(go);
			}
		}
	}

	public void clearDeletedObjects() {
		if (deletedObjects.size() == 0)
			return;

		for (GameObject go : deletedObjects) {
			if (go instanceof ObjectDynamic) {
				objectDynamic.remove(go);
			} else {
				objectStatic.remove(go);
			}
		}
		deletedObjects.clear();
	}

	public void drawPlayers(Graphics2D g2) {
		for (Player player : players) {
			player.draw(g2);
		}
	}

	public void drawObjectDynamic(Graphics2D g2) {
		for (GameObject go : objectDynamic) {
			go.draw(g2);
		}
	}

	public void drawObjectStatic(Graphics2D g2) {
		for (GameObject go : objectStatic) {
			go.draw(g2);
		}
	}

	public void draw(Graphics2D g2) {
		g2.drawImage(background, 0, 0, null);
		drawObjectDynamic(g2);
		drawObjectStatic(g2);
		drawPlayers(g2);
	}
}
