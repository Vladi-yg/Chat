package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket = null;
    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;

    public String getNickname() { return nickname; }

    ClientHandler(final Server server, final Socket socket) {
        this.server = server;
        this.socket = socket;

        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Авторизация
                    authorization();

                    while (true) {
                        String msg = in.readUTF();
                        if (msg.equals("/end")) {
                            out.writeUTF("/serverClosed");
                            break; }

                        if (msg.startsWith("/w")) {  //Проверка персональности сообщения
                            String[] tokens = msg.split(" ",3);
                            if(tokens.length == 1) {
                                out.writeUTF("Пожалуйста, укажите nickname");
                            } else if (tokens.length == 2) {
                                out.writeUTF("Вы не ввели сообщение");
                            }
                            else {
                                server.sendPrivateMsg(ClientHandler.this, tokens[1], tokens[2]);
                            }
                        } else {

                        /*
                            Синтаксис добавления пользователя в блэклист
                            /blacklist add nickname
                            Для удаления пользователя нужно набрать
                            /blacklist delete nickname
                        */

                            if (msg.startsWith("/blacklist")) {
                                String[] tokens = msg.split(" ", 3);
                                boolean result;
                                if (tokens.length == 1 || tokens.length == 2) {
                                    out.writeUTF("Неверная команда, попробуйте еще раз");
                                } else if (tokens[1].equals("add")) {
                                    result = AuthService.addToBlackList(nickname, tokens[2]);
                                    if(result == false) {
                                        out.writeUTF("Такого ника нет, либо он уже добавлен в blacklist");
                                    } else {
                                        out.writeUTF("Ник успешно добавлен в блэк-лист");
                                    }
                                } else if (tokens[1].equals("delete")) {
                                    result = AuthService.deleteFromBlackList(nickname, tokens[2]);
                                    if(result == false) {
                                        out.writeUTF("Такого ника нет, либо он уже удален из blacklist");
                                    } else {
                                        out.writeUTF("Ник успешно удален из блэк-листа");
                                    }
                                }
                            } else {
                                server.broadcast(ClientHandler.this, nickname + ": " + msg);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(ClientHandler.this);
                    System.out.println("Client disconnected");
                }
            }
        }).start();
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authorization () throws IOException {
        while (true) {
            String string = in.readUTF();
            if (string.startsWith("/auth")) {
                String[] tokens = string.split(" ");
                String tempNickname = AuthService.getNicknameByLoginPassword(tokens[1], tokens[2]);
                if(tempNickname != null) { //Ник найден в БД
                    if(server.findClientByNickname(tempNickname) != null) { //не подключен ли уже такой ник
                        sendMsg("Пользователь с таким ником уже подключен");
                    } else {
                        sendMsg("/authOk");
                        nickname = tempNickname;
                        server.subscribe(ClientHandler.this);
                        break;
                    }
                }
                else {
                    sendMsg("Неверный логин или пароль");
                }
            }
        }
    }
}
