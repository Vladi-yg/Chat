package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

public class Server {

    Vector<ClientHandler> clients;

    Server() {
        Socket socket = null;
        ServerSocket server = null;

        try {
            AuthService.connect();
            server = new ServerSocket(8188);
            System.out.println("Сервер запущен и ждет");
            clients = new Vector<>();

            while(true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    public void broadcast(ClientHandler sender, String msg){

        /*
            Если поместить проверку checkBlackList в цикл, то на каждой итерации будет
            отпрвляться один и тот же запрос в базу данных на leftJoin.
            Чтобы этого не было, получим весь блэк-лист отправителя сразу вне цикла.
         */

        ArrayList<String> blacklist = AuthService.getBlackList(sender.getNickname());
        for (ClientHandler c: clients) {
            if(!c.equals(sender) && !blacklist.contains(c.getNickname())){ //самому себе тоже не отправляем
                c.sendMsg(msg);
            }
        }
    }

    public void sendPrivateMsg(ClientHandler sender, String nickTo, String msg) {

        ArrayList<String> blacklist = AuthService.getBlackList(sender.getNickname());
        ClientHandler recipient = findClientByNickname(nickTo);

        if(recipient == null) {
            sender.sendMsg("Такого пользователя не существует или он не онлайн");
        } else {
            if (!blacklist.contains(nickTo) && clients.contains(recipient)) {
                recipient.sendMsg("from " + sender.getNickname() + ": " + msg);
            } else {
                sender.sendMsg("Пользователь не онлайн, либо находится в блэк-лист");
            }
        }
    }

    public ClientHandler findClientByNickname(String nickname) {
        for (ClientHandler c: clients) {
            if (c.getNickname().equals(nickname)) {
                return c;
            }
        }
        return null;
    }

    public void subscribe(ClientHandler client) {
        clients.add(client);
        broadcastClientList(clients);
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
        broadcastClientList(clients);
    }

    public void broadcastClientList(Vector<ClientHandler> clients) {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientList ");
        for (ClientHandler c: clients) {
            sb.append(c.getNickname() + " ");
        }
        String list = sb.toString();

        for (ClientHandler c: clients) {
            c.sendMsg(list);
        }
    }
}
