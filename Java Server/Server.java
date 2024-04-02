import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    // socket responsabile di ricevere le connessioni dei client e creare un socket
    // per i client
    private ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() {
        try {
            // condizione rispettata fin quando il server è aperto
            while (!serverSocket.isClosed()) {
                // il server accetta connessioni dal client
                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected");
                ClientHandler clientHandler = new ClientHandler(socket);

                // il thread serve per gestire la comunicazione con più client ad una volta,
                // senza interrompere il
                // flusso del resto del programma
                Thread thread = new Thread(clientHandler);
                // avvio del thread
                thread.start();
            }
        } catch (IOException e) {

        }
    }

    // metodo per chiudere il server
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        // come parametro del server socket metto la porta in cui il server deve
        // ascoltare le richieste del client
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
