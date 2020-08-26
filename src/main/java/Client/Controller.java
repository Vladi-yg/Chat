package Client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {
    @FXML
    public TextField msg;
    @FXML public TextArea textArea;
    @FXML public Button sendMsg;

    @FXML public TextField loginField;
    @FXML public PasswordField passwordField;

    @FXML public HBox upperPanel;
    @FXML public HBox bottomPanel;

    @FXML public VBox chatField;
    @FXML public ListView<String> clientList;

    private boolean isAuthorized;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    final String IP_ADDRESS = "localhost";
    final int PORT = 8188;

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if(!isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
            clientList.setVisible(false);
            clientList.setManaged(false);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            clientList.setVisible(true);
            clientList.setManaged(true);
        }
    }

    @FXML public void sendMsg(javafx.event.ActionEvent actionEvent) {

        try {
            out.writeUTF(msg.getText());

            //Выводим свое сообщение справа
            Label label = new Label(msg.getText() + "\n");
            VBox vbox = new VBox();
            vbox.setAlignment(Pos.TOP_RIGHT);
            vbox.getChildren().add(label);
            chatField.getChildren().add(vbox);

            msg.clear();
            msg.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect () {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String serverAnswer = in.readUTF();
                            if(serverAnswer.startsWith("/authOk")) {
                                setAuthorized(true);
                                break;
                            } else {
                                //    textArea.appendText(serverAnswer + "\n");
                            }
                        }

                        while(true) {
                            String newMsg = in.readUTF();
                            if(newMsg.equals("/serverClosed")) {
                                //     textArea.appendText(newMsg + "\n");
                                break;
                            }
                            if(newMsg.startsWith("/clientList")) {
                                final String[] tokens = newMsg.split(" ");
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        clientList.getItems().clear();
                                        for (int i = 1; i < tokens.length; i++) {
                                            clientList.getItems().add(tokens[i] + "\n");
                                        }
                                    }
                                });

                            } else {
                                //Чужие сообщения выводим слева
                                Label label = new Label(newMsg + "\n");
                                final VBox vbox = new VBox();
                                vbox.setAlignment(Pos.TOP_LEFT);
                                vbox.getChildren().add(label);

                                /*
                                Эта штука Platform.runLater каким-то чудом решает проблему потоков.
                                Модифицировать компоненты UI можно только в FX треде.
                                И если написать без этой штуки, то будет ошибка.
                                 */

                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        chatField.getChildren().add(vbox);
                                    }
                                });
                                //                              textArea.appendText(newMsg + "\n");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setAuthorized(false);
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryAuth(javafx.event.ActionEvent actionEvent) {
        if(socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
