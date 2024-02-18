package server;

import java.io.IOException;
import java.net.ServerSocket;

public class AcceptServer extends Thread {

    private GameServer gameServer = GameServer.getInstance();
    private ServerSocket socket;

    public AcceptServer(ServerSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        while (true) {
            try {
                gameServer.AppendText("Waiting new clients ...");
                gameServer.setClient_socket(socket.accept());
                gameServer.AppendText("새로운 참가자 from " + gameServer.getClient_socket());
                gameServer.makeNewUser(gameServer.getClient_socket());
            } catch (IOException e) {
                gameServer.AppendText("accept() error");
            }
        }
    }
}