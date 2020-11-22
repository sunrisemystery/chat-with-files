import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Scanner;

public class Client {

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private Socket socket;
    private final String server;
    private String username;
    private final int port;

    Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    public boolean start() {
        try {
            socket = new Socket(server, port);
        } catch (Exception e) {
            show("Connection to server failed:" + e);
            return false;
        }

        String message = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        show(message);

        try {
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            show("IO exception: " + e);
            return false;
        }
        new ServerListener().start();

        try {
            objectOutputStream.writeObject(username);
        } catch (IOException e) {

            show("Exception in login : " + e);
            disconnect();
            return false;
        }
        return true;
    }

    private void show(String message) {
        System.out.println(message);
    }


    private void disconnect() {
        try {
            if (objectInputStream != null) objectInputStream.close();
            if (objectOutputStream != null) objectOutputStream.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendMessage(Message message) {
        try {
            objectOutputStream.writeObject(message);
        } catch (IOException e) {
            show("Exception writing to server: " + e);
        }
    }

    private String sendFile(String path) {

        String byteToString = null;
        try {
            byte[] allBytes = Files.readAllBytes(Paths.get(path));
            byteToString = Base64.getEncoder().encodeToString(allBytes);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteToString;

    }

    private String chooseFile() {
        FileDialog dialog = new FileDialog((Frame) null, "Select File to Open");
        dialog.setMode(FileDialog.LOAD);
        dialog.setVisible(true);
        String file = dialog.getFile();
        System.out.println(file + " chosen.");
        return dialog.getDirectory() + dialog.getFile();
    }

    public static void main(String[] args) {

        int portNumber = 9898;
        String serverAddress = "localhost";
        String username;
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter the username: ");
        username = scanner.nextLine();

        Client client = new Client(serverAddress, portNumber, username);

        if (!client.start())
            return;

        System.out.println("\nHi, you are in a chat :)");
        System.out.println("1. Just type a message and press enter to send message to all active users");
        System.out.println("2. Type 'PICK' to pick a file from your computer");
        System.out.println("3. Type 'LOGOUT'  to log out from server");

        while (true) {
            System.out.print("> ");
            String message = scanner.nextLine();

            if (message.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new Message(Message.LOGOUT, ""));
                break;

            } else if (message.equalsIgnoreCase("PICK")) {

                String path = client.chooseFile();
                String bytesString = client.sendFile(path);
                client.sendMessage(new Message(Message.PICK, bytesString));

            } else {
                client.sendMessage(new Message(Message.MESSAGE, message));
            }
        }
        scanner.close();
        client.disconnect();
    }

    class ServerListener extends Thread {

        public void run() {
            while (true) {
                try {
                    String message = (String) objectInputStream.readObject();
                    System.out.println(message);
                    System.out.print("> ");
                } catch (IOException e) {
                    String notificationSign = " *** ";
                    show(notificationSign + "Server has closed the connection: " + e + notificationSign);
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

