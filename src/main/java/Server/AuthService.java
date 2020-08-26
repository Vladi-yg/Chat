package Server;

import java.sql.*;
import java.util.ArrayList;

public class AuthService {
    private static Connection connection;
    private static Statement statement;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getNicknameByLoginPassword(String login, String pass) {
        String query = String.format("SELECT Nickname from main where " +
                "login = '%s'and password = '%s'", login, pass);
        try {
            ResultSet resultSet = statement.executeQuery(query);
            if(resultSet.next()) {
                return resultSet.getString("Nickname");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static boolean addToBlackList(String nick, String nickAdded) {

        int idUser = findIdByNickname(nick);
        int idUserAdded = findIdByNickname(nickAdded);

        //Если указанного ника не сущетсвует, то возвращаем false
        if(idUserAdded == -1) {
            return false;
        }

        String query = String.format("insert into blacklist (idUser, idToBlock) values (%d,%d)",
                idUser, idUserAdded);
        try {
            statement.executeUpdate(query);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean deleteFromBlackList(String nick, String nickDeleted) {
        int idUser = findIdByNickname(nick);
        int idUserDeleted = findIdByNickname(nickDeleted);

        //Если указанного ника не сущетсвует, то возвращаем false
        if(idUserDeleted == -1) {
            return false;
        }

        String query = String.format("delete from blacklist where (idUser = %d and idToBlock = %d)",
                idUser, idUserDeleted);
        try {
            int result = statement.executeUpdate(query);
            if(result == 0) {
                return false;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
        return true;
    }

    public static ArrayList getBlackList(String senderNick) {

        int idSender = findIdByNickname(senderNick);
        String query = String.format("select nickname from main inner join blackList on id = idToBlock " +
                "where idUser = %d", idSender);

        ResultSet resultSet = null;
        ArrayList<String> blackList = new ArrayList<>();

        try {
            resultSet = statement.executeQuery(query);
            for (int i = 0; resultSet.next() ; i++) {
                blackList.add(i, resultSet.getString("nickname"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return blackList;
    }

    public static int findIdByNickname(String nick) {
        String query = String.format("SELECT id from main where " + "nickname = '%s'", nick);
        ResultSet resultSet = null;
        try {
            resultSet = statement.executeQuery(query);
            if(resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return -1;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
