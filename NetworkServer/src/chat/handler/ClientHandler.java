package chat.handler;

import chat.MyServer;
import chat.User;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import chat.auth.*;

public class ClientHandler {

    private static final String AUTH_CMD_PREFIX = "/auth";
    private static final String AUTHOK_CMD_PREFIX = "/authok";
    private static final String AUTHERR_CMD_PREFIX = "/autherr";
    private static final String PRIVATE_MESSAGE_PREFIX = "/w";

    private final MyServer myServer;
    private final Socket clientSocket;
    private DataInputStream io;
    private DataOutputStream out;
    private String clientUsername;

    public ClientHandler(MyServer myServer, Socket clientSocket) {
        this.myServer = myServer;
        this.clientSocket = clientSocket;
    }

    public void handle() throws IOException {
        io = new DataInputStream(clientSocket.getInputStream());
        out = new DataOutputStream(clientSocket.getOutputStream());


        new Thread(() -> {
            try {
                authentication();
                readMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void authentication() throws IOException {
        String message = io.readUTF();
        while (true) {
            if (message.startsWith(AUTH_CMD_PREFIX)) {
                String[] parts = message.split("\\s+", 3);
                String login = parts[1];
                String password = parts[2];

                AuthService authService = myServer.getAuthService();
                this.clientUsername = authService.getUsernameByLoginAndPassword(login, password);
                if (clientUsername != null) {
                    if (myServer.isUsernameBusy(clientUsername)) {
                        out.writeUTF(AUTHERR_CMD_PREFIX + " Логин уже используется");
                    }

                    out.writeUTF(AUTHOK_CMD_PREFIX + " " + clientUsername);
                    myServer.broadcastMessage(clientUsername + " присоединился к чату", this);
                    myServer.subscribe(this);
                    break;
                } else {
                    out.writeUTF(AUTHERR_CMD_PREFIX + " Логин или пароль не соответствуют действительности");
                }
            }
            else {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Ошибка авторизации");
            }
        }
    }


    private void readMessage() throws IOException {
        while (true) {
            String message = io.readUTF();
            System.out.println("message | " + clientUsername + ": " + message);
            if (message.startsWith("/end")) {
                return;
            }

            if (message.startsWith(PRIVATE_MESSAGE_PREFIX)) {
                String arrPrivateMessage[] = message.split(" ", 3);
                String userNamePrivateMessage = arrPrivateMessage[1];
                String privateMessage = arrPrivateMessage[2];

                myServer.sendPrivateMessage( clientUsername+ ": " + privateMessage, userNamePrivateMessage);
            }
            else {
                myServer.broadcastMessage(clientUsername + ": " + message, this);
            }
        }
    }

    public String getClientUsername() {
        return clientUsername;
    }



    public void sendMessage(String s) throws IOException {
        out.writeUTF( s);
    }

    public void privateMessage(String s, String senderName) throws IOException {
        out.writeUTF( s);
    }
}
