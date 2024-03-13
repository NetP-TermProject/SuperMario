package server;

import java.awt.EventQueue;
import java.io.IOException;
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
    private JTextArea textArea;
    private JTextField txtPortNumber;

    private ServerSocket socket; // 서버소켓
    private Socket client_socket; // accept() 에서 생성된 client 소켓
    public static Vector<UserService> UserVec = new Vector(); // 연결된 사용자를 저장할 벡터

    public ServerSocket getSocket() {
        return socket;
    }

    public Socket getClient_socket() {
        return client_socket;
    }

    public void setClient_socket(Socket client_socket) {
        this.client_socket = client_socket;
    }

    public static int roomNumberCnt = 0;
    public static int serverUserCnt = 0;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                GameServer frame = GameServer.getInstance();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static class LazyHolder {
        public static final GameServer INSTANCE = new GameServer();
    }

    public static GameServer getInstance() {
        return LazyHolder.INSTANCE;
    }

    private GameServer() {
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

        makePortTextField();
        contentPane.add(txtPortNumber);
        JButton btnServerStart = new JButton("Server Start");

        btnServerStart.addActionListener(e -> {
            makeServer(btnServerStart);
        });
        btnServerStart.setBounds(12, 356, 300, 35);
        contentPane.add(btnServerStart);
    }

    public void makePortTextField() {
        txtPortNumber = new JTextField();
        txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
        txtPortNumber.setText("30000");
        txtPortNumber.setBounds(112, 318, 199, 26);
        txtPortNumber.setColumns(10);
    }

    private void makeServer(JButton btnServerStart) {
        try {
            socket = new ServerSocket(Integer.parseInt(txtPortNumber.getText()));
        } catch (NumberFormatException | IOException e1) {
            e1.printStackTrace();
        }
        AppendText("Chat Server Running..");
        btnServerStart.setText("Chat Server Running..");
        btnServerStart.setEnabled(false); // 서버를 더이상 실행시키지 못 하게 막는다
        txtPortNumber.setEnabled(false); // 더이상 포트번호 수정못 하게 막는다
        AcceptServer accept_server = new AcceptServer(socket);
        accept_server.start();
    }

    public void AppendText(String str) {
        textArea.append(str + "\n");
        textArea.setCaretPosition(textArea.getText().length());
    }

    public void makeNewUser(Socket client_socket) {
        UserService new_user = new UserService(client_socket);
        UserVec.add(new_user);
        new_user.start();
        AppendText("현재 참가자 수 " + UserVec.size());
    }


    public void AppendObject(GameModelMsg msg) {
        if (msg.getPlayerName() == null)
            textArea.append("code = " + msg.getCode() + "\n");
        else
            textArea.append("id = " + msg.getPlayerName() + " code = " + msg.getCode() + "\n");
        textArea.setCaretPosition(textArea.getText().length());
    }
}
