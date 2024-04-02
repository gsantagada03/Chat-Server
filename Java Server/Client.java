import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;

    // costruttore
    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.username = username;
        } catch (IOException e) {
            closeEverything(writer, reader, socket);
        }
    }

    // metodo che manda il messaggio al ClientHandler
    public void sendMessage() {
        try {
            writer.write(username);
            writer.newLine();
            writer.flush();

            Scanner sc = new Scanner(System.in);
            // fin quando il client è connesso, i messaggi scritti dalla console del client
            // verranno mandati al ClientHandler
            while (socket.isConnected()) {
                String messageToSend = sc.nextLine();
                byte[] charset = messageToSend.getBytes("UTF-8");
                messageToSend = new String(charset, "UTF-8");
                writer.write(messageToSend);
                writer.newLine();
                writer.flush();

                // se il client digita exit, esce dalla chat, mandando il messaggio al
                // ClientHandler
                if (messageToSend.equalsIgnoreCase("exit")) {
                    closeEverything(writer, reader, socket);
                }
            }
        } catch (IOException e) {
            closeEverything(writer, reader, socket);
        }
    }

    // metodo che serve per ricevere i messaggi mandati dal server e stamparli nella
    // propria console
    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String messageFromGroupChat;

                while (socket.isConnected()) {
                    try {
                        messageFromGroupChat = reader.readLine();
                        byte[] charset = messageFromGroupChat.getBytes("UTF-8");
                        messageFromGroupChat = new String(charset, "UTF-8");
                        System.out.println(messageFromGroupChat);
                    } catch (IOException e) {
                        closeEverything(writer, reader, socket);
                    }
                }
            }
        }).start();
    }

    public void closeEverything(BufferedWriter writer, BufferedReader reader, Socket socket) {
        try {
            if (writer != null) {
                writer.close();
            }

            if (reader != null) {
                reader.close();
            }

            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter your username. \n Type exit to leave the chat ");
        String username = sc.nextLine();
        Socket socket = new Socket("localhost", 1234);
        Client client = new Client(socket, username);
        client.listenForMessage();
        client.sendMessage();
    }
}
