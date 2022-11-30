package server;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class GameServer extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	JTextArea textArea;
	private JTextField txtPortNumber;

	private ServerSocket socket; // 서버소켓
	private Socket client_socket; // accept() 에서 생성된 client 소켓
	private Vector UserVec = new Vector(); // 연결된 사용자를 저장할 벡터
	private static final int BUF_LEN = 128; // Windows 처럼 BUF_LEN 을 정의
	private static final int USER_MAX_COUNT = 2;
	private Vector<GameRoom> roomVector = new Vector<GameRoom>();

	private volatile int roomNumberCnt = 1;
	private String generatedRoomNumber;
	private static long serverCnt = 1;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GameServer frame = new GameServer();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public GameServer() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 338, 440);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 10, 300, 298);
		contentPane.add(scrollPane);

		textArea = new JTextArea();
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);

		JLabel lblNewLabel = new JLabel("Port Number");
		lblNewLabel.setBounds(13, 318, 87, 26);
		contentPane.add(lblNewLabel);

		txtPortNumber = new JTextField();
		txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
		txtPortNumber.setText("30000");
		txtPortNumber.setBounds(112, 318, 199, 26);
		contentPane.add(txtPortNumber);
		txtPortNumber.setColumns(10);

		JButton btnServerStart = new JButton("Server Start");
		btnServerStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					socket = new ServerSocket(Integer.parseInt(txtPortNumber.getText()));
				} catch (NumberFormatException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				AppendText("Chat Server Running..");
				btnServerStart.setText("Chat Server Running..");
				btnServerStart.setEnabled(false); // 서버를 더이상 실행시키지 못 하게 막는다
				txtPortNumber.setEnabled(false); // 더이상 포트번호 수정못 하게 막는다
				AcceptServer accept_server = new AcceptServer();
				accept_server.start();
			}
		});
		btnServerStart.setBounds(12, 356, 300, 35);
		contentPane.add(btnServerStart);
	}

	class AcceptServer extends Thread {
		@SuppressWarnings("unchecked")
		public void run() {
			while (true) { // 사용자 접속을 계속해서 받기 위해 while문
				try {
					AppendText("Waiting new clients ...");
					client_socket = socket.accept(); // accept가 일어나기 전까지는 무한 대기중
					AppendText("새로운 참가자 from " + client_socket);
					// User 당 하나씩 Thread 생성
					UserService new_user = new UserService(client_socket);
					UserVec.add(new_user); // 새로운 참가자 배열에 추가
					new_user.start(); // 만든 객체의 스레드 실행
					AppendText("현재 참가자 수 " + UserVec.size());
				} catch (IOException e) {
					AppendText("accept() error");
					// System.exit(0);
				}
			}
		}
	}

	class UserService extends Thread { // 게임 참가자마다 스레드 생

		private ObjectInputStream ois;
		private ObjectOutputStream oos;

		private Socket client_socket;
		private Vector user_vc;
		public String UserName = "";
		public String UserStatus;

		public UserService(Socket client_socket) {
			this.client_socket = client_socket;
			this.user_vc = UserVec;
			try {
				oos = new ObjectOutputStream(client_socket.getOutputStream());
				oos.flush();
				ois = new ObjectInputStream(client_socket.getInputStream());
			} catch (Exception e) {
				AppendText("userService error");
			}
		}

		public void Login() {
			AppendText("새로운 참가자 " + UserName + " 서버에 입장.");
		}

		public void Logout() {
			String msg = "[" + UserName + "]님이 퇴장 하였습니다.\n";
			UserVec.removeElement(this); // Logout한 현재 객체를 벡터에서 지운다
			for (int i = 0; i < roomVector.size(); i++) {
				for (int j = 0; j < roomVector.elementAt(i).userList.size(); j++) {
					if (this == roomVector.elementAt(i).userList.elementAt(j)) {
						roomVector.elementAt(i).userList.remove(j);
						WriteRoomListObject(); // 방리스트 갱신
						break;
					}
				}

			}
			WriteAll(msg); // 나를 제외한 다른 User들에게 전송
			AppendText("사용자 " + "[" + UserName + "] 퇴장. 현재 참가자 수 " + UserVec.size());
		}

		// 모든 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteAll(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				user.WriteOne(str);
			}
		}

		// 모든 User들에게 Object를 방송. 채팅 message와 image object를 보낼 수 있다
		public void WriteAllObject(Object ob) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				user.WriteOneObject(ob);
			}
		}

		public void WriteGameStartObject(int roomVectorindex, String roomNumber) {
			long randomSeedNumber = System.nanoTime();
			for (int i = 0; i < roomVector.elementAt(roomVectorindex).userList.size(); i++) {
				UserService sendUser = (UserService) roomVector.elementAt(roomVectorindex).userList.elementAt(i);
				GameModelMsg objectGameStart = new GameModelMsg(roomNumber, UserName, NetworkStatus.GAME_START, i,
						randomSeedNumber);
				sendUser.WriteOneObject(objectGameStart);
				AppendObject(objectGameStart);
			}
		}

		public void WriteGameLoseObject(int roomVectorindex, String roomNumber) {
			GameModelMsg objectGameLose = null;
			for (int i = 0; i < roomVector.elementAt(roomVectorindex).userList.size(); i++) {
				UserService user = (UserService) roomVector.elementAt(roomVectorindex).userList.elementAt(i);
				objectGameLose = new GameModelMsg(roomNumber, UserName, NetworkStatus.GAME_LOSE);
				if (user != this) // 나말고 다른 유저에게 보내
					user.WriteOneObject(objectGameLose);

			}
			roomVector.remove(roomVectorindex);
			WriteRoomListObject();
			AppendObject(objectGameLose);
		}

		public void WriteRoomListObject() {
			StringBuffer roomList = new StringBuffer();
			if (roomVector.size() == 0)
				roomList.append("");
			else {
				for (int i = 0; i < roomVector.size(); i++) {
					String roomNumber = roomVector.elementAt(i).getRoomNumber();
					int userCount = roomVector.elementAt(i).getUserList().size(); // 방 유저 수
					roomList.append(roomNumber + " " + userCount + "/");
				}
			}
			GameModelMsg objectGameMsg = null;

			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				objectGameMsg = new GameModelMsg(roomList.toString(), NetworkStatus.SHOW_LIST);
				user.WriteOneObject(objectGameMsg);
			}
		}

		public void WriteGameButtonMsg(int roomVectorindex, GameModelMsg objectGameMsg) {
			for (int i = 0; i < roomVector.elementAt(roomVectorindex).userList.size(); i++) {
				UserService sendUser = (UserService) roomVector.elementAt(roomVectorindex).userList.elementAt(i);
				// System.out.println("sendUser " + sendUser);
				// System.out.println("this " + this);
				sendUser.WriteOneObject(objectGameMsg);
			}
		}

		public void WriteErrorMsg() {
			GameModelMsg errorMsg = new GameModelMsg(NetworkStatus.ERROR);
			WriteOneObject(errorMsg);
		}

		// 나를 제외한 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteOthers(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				user.WriteOne(str);
			}
		}

		// 나를 제외한 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteOthersObject(Object obj) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user != this)
					user.WriteOneObject(obj);
			}
		}

		// UserService Thread가 담당하는 Client 에게 1:1 전송
		public void WriteOne(String msg) {
			try {
				GameModelMsg obcm = new GameModelMsg("SERVER", "200");
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
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
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}

		public void WriteOneObject(Object ob) {
			try {
				oos.writeObject(ob);
			} catch (IOException e) {
				AppendText("oos.writeObject(ob) error");
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

		public void run() {
			while (true) { // 사용자 접속을 계속해서 받기 위해 while문
				try {
					Object obcm = null;
					String msg = null;
					GameModelMsg objectGameMsg = null;
					if (socket == null)
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
						AppendObject(objectGameMsg);
					} else
						continue;
					if (objectGameMsg.getCode().matches(NetworkStatus.LOG_IN)) { // 100
						UserName = "player@" + System.nanoTime() + serverCnt++;
						objectGameMsg.setPlayerName(UserName);
						Login();
						WriteOneObject(objectGameMsg);
					} else if (objectGameMsg.getCode().matches(NetworkStatus.SHOW_LIST)) { // 1200
						WriteRoomListObject();
					} else if (objectGameMsg.getCode().matches(NetworkStatus.MAKE_ROOM_REQUEST)) { // 1000
						generatedRoomNumber = objectGameMsg.getRoomNumber() + roomNumberCnt++;
						GameRoom gameRoom = new GameRoom(generatedRoomNumber);
						roomVector.add(gameRoom);
						WriteRoomListObject();
					} else if (objectGameMsg.getCode().matches(NetworkStatus.LOG_OUT)) { // 200
						Logout();
						break;
					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_READY)) {// 300
						for (int i = 0; i < roomVector.size(); i++) {
							if (roomVector.elementAt(i).getRoomNumber().matches(objectGameMsg.getRoomNumber())) {
								if (roomVector.elementAt(i).userList.size() == USER_MAX_COUNT) {
									WriteErrorMsg();
									break;
								} else {
									roomVector.elementAt(i).increaseReadyStatusCnt();
									roomVector.elementAt(i).addPlayerinGameRoom(this);
									WriteRoomListObject();
								}
							}
							if (roomVector.elementAt(i).userList.size() == USER_MAX_COUNT
									&& roomVector.elementAt(i).getReadyStatusCnt() == USER_MAX_COUNT) { // 게임 시작 프로토콜
																										// 보내기
								WriteGameStartObject(i, objectGameMsg.getRoomNumber());
								roomVector.elementAt(i).setReadyStatusCnt(0);
								break;
							} else { // 게임 대기 화면 상태 보내기
								WriteOneObject(objectGameMsg);
							}
						}
						AppendText(UserName + " 게임준비완료 ");

					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_BUTTON)) { // 600
						for (int i = 0; i < roomVector.size(); i++) {
							if (roomVector.elementAt(i).getRoomNumber().matches(objectGameMsg.getRoomNumber())) { // 받은

								WriteGameButtonMsg(i, objectGameMsg);
								AppendText(objectGameMsg.posToString());
								AppendText(objectGameMsg.velToString());
								AppendText(objectGameMsg.inputToString());
							}
						}
					} else if (objectGameMsg.getCode().matches(NetworkStatus.GAME_WIN)) { // 700 수신
						for (int i = 0; i < roomVector.size(); i++) {
							if (roomVector.elementAt(i).getRoomNumber().matches(objectGameMsg.getRoomNumber())) {
								WriteGameLoseObject(i, objectGameMsg.getRoomNumber()); // 다른 유저에게 졌다는 메세지 보내기 //800으로
																						// 보내기
							}
						}
						WriteOneObject(objectGameMsg); // 다시 승리 메세지 보내기 스레드 종료를 위해
					}

				} catch (IOException e) {
					AppendText("ois.readObject() error");
					try {
						ois.close();
						oos.close();
						client_socket.close();
						Logout(); // 에러가난 현재 객체를 벡터에서 지운다
						break;
					} catch (Exception ee) {
						break;
					} // catch문 끝
				} // 바깥 catch문끝
			} // while
		} // run
	}

	public void AppendText(String str) {
		// textArea.append("사용자로부터 들어온 메세지 : " + str+"\n");
		textArea.append(str + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	public void AppendObject(GameModelMsg msg) {
		if (msg.getPlayerName() == null)
			textArea.append("code = " + msg.getCode() + "\n");
		else
			textArea.append("id = " + msg.getPlayerName() + " code = " + msg.getCode() + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}
}
