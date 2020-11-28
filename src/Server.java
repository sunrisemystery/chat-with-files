import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

public class Server {

    private final ArrayList<ClientThread> clientThreadArrayList;
    private final SimpleDateFormat dateFormat;
    private static int uniqueId;
    private final int port;
    private boolean isOn;
    private final String notificationSign = " *** ";

    public Server(int port) {
        this.port = port;
        dateFormat = new SimpleDateFormat("HH:mm:ss");
        clientThreadArrayList = new ArrayList<>();
    }

    public void start() {
        isOn = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (isOn) {
                display("Server waiting for users on port " + port);
                Socket socket = serverSocket.accept();
                if (!isOn)
                    break;
                ClientThread clientThread = new ClientThread(socket);
                clientThreadArrayList.add(clientThread);
                clientThread.start();
            }
            try {
                serverSocket.close();
                for (ClientThread clientThread : clientThreadArrayList) {
                    try {
                        clientThread.objectInputStream.close();
                        clientThread.objectOutputStream.close();
                        clientThread.socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        } catch (IOException e) {
            String message = dateFormat.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(message);
        }
    }

    public void stop() {
        isOn = false;
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void display(String message) {
        String time = dateFormat.format(new Date()) + " " + message;
        System.out.println(time);
    }

    private synchronized boolean broadcast(String message) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(message);

            for (int i = clientThreadArrayList.size(); --i >= 0; ) {
                ClientThread clientThread = clientThreadArrayList.get(i);
                String filename = clientThread.receiveFile(message);
                clientThread.writeMessage(clientThread.textHandler.handleRequest(filename));

            }

        } catch (IllegalArgumentException e) {

            String time = dateFormat.format(new Date());
            String timeMessage = time + " " + message + "\n";
            System.out.print(timeMessage);

            for (int i = clientThreadArrayList.size(); --i >= 0; ) {
                ClientThread clientThread = clientThreadArrayList.get(i);

                if (!clientThread.writeMessage(timeMessage)) {
                    clientThreadArrayList.remove(i);
                    display("Disconnected user " + clientThread.username + " removed from list.");
                }
            }
        }

        return true;
    }

    private synchronized void disconnectClient(int id) {
        String disconnectedClient = "";
        for (int i = 0; i < clientThreadArrayList.size(); ++i) {
            ClientThread clientThread = clientThreadArrayList.get(i);
            if (clientThread.id == id) {
                disconnectedClient = clientThread.getUsername();
                clientThreadArrayList.remove(i);
                break;
            }
        }
        broadcast(notificationSign + disconnectedClient + " has left the chat." + notificationSign);
    }

    public static void main(String[] args) {
        int portNumber = 9898;

        Server server = new Server(portNumber);
        server.start();
    }

    class ClientThread extends Thread {
        private Socket socket;
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;
        private Handler textHandler = new TextHandler();
        private int id;
        private String username;
        private Message chatMessage;

        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream = new ObjectInputStream(socket.getInputStream());
                username = (String) objectInputStream.readObject();
                broadcast(notificationSign + username + " has joined the chat." + notificationSign);

            } catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
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


        private String receiveFile(String bytes) {
            String outputFileName = "image.bmp";
            FileOutputStream fileOutputStream;
            BufferedOutputStream bufferedOutputStream;
            try{
            byte[] decodedBytes = Base64.getDecoder().decode(bytes);
            InputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);
            try {
                String content = URLConnection.guessContentTypeFromStream(byteArrayInputStream);
                if (content != null) {
                    if (content.equals("audio/x-wav")) {
                        outputFileName = "song.wav";
                    }
                }
                fileOutputStream = new FileOutputStream(outputFileName);
                bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                bufferedOutputStream.write(decodedBytes, 0, decodedBytes.length);
                bufferedOutputStream.flush();

                fileOutputStream.close();
                bufferedOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }}
            catch (NullPointerException e){
                System.out.println("Choose a file");
            }
            return outputFileName;

        }

        public void run() {
            boolean isOn = true;
            while (isOn) {
                try {
                    chatMessage = (Message) objectInputStream.readObject();

                } catch (IOException e) {

                    display(username + " exception reading Streams: " + e);
                    break;

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    break;
                }

                String message = chatMessage.getMessage();
                switch (chatMessage.getType()) {

                    case Message.MESSAGE:
                        boolean confirmation = broadcast(username + ": " + message);
                        if (!confirmation) {
                            String msg = notificationSign + "Sorry. No such user exists." + notificationSign;
                            writeMessage(msg);
                        }
                        break;
                    case Message.LOGOUT:
                        display(username + " has been logged out.");
                        isOn = false;
                        break;

                    case Message.PICK:
                        display(username + " sent an audio/image file.");
                        broadcast(username + ": ");
                        String str = receiveFile(chatMessage.getMessage());
                        String bytes = sendFile(str);
                        broadcast(bytes);
                        break;
                }
            }
            disconnectClient(id);
            closeAll();
        }

        private void closeAll() {
            try {
                if (objectOutputStream != null) objectOutputStream.close();
                if (objectInputStream != null) objectInputStream.close();
                if (socket != null) socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private boolean writeMessage(String message) {
            if (!socket.isConnected()) {
                closeAll();
                return false;
            }
            try {
                objectOutputStream.writeObject(message);
            } catch (IOException e) {
                display(notificationSign + "Error sending message to " + username + notificationSign);
                display(e.toString());
            }
            return true;
        }

        public String getUsername() {
            return username;
        }
    }
}

