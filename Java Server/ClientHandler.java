import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import com.google.gson.Gson;

public class ClientHandler implements Runnable {

    // variabile per tenere conto il tempo passato dall'ultimo messaggio inviato
    private long lastMessageTime;

    // metodo per calcolare quanto tempo è passato dall'invio dell'ultimo messaggio
    private boolean isAllowedToSendMessage() {
        long currentTime = System.currentTimeMillis();
        // viene effetuata la sottazione tra il tempo passato dal 1970 fino ad ora in
        // millisecondi e il tempo
        // passato dall'ultimo messaggio ad ora(sempre in millisecondi),
        // se il risultato è inferiore o uguale ad 1 secondo, il messaggio non viene
        // inviato
        return (currentTime - lastMessageTime) >= 1000;
    }

    // lista di parole vietate
    private static ArrayList<String> forbiddenWords = new ArrayList<>();

    // creo un file di tipo json contenenti le parole che devono essere censurate, e
    // utilizzo Gson per leggere il file JSON
    static {
        try {
            // Oggetto per convertire oggetti java in formato JSON e viceversa
            Gson gson = new Gson();
            // converte il DictionaryJson e lo converte in oggetto java di tipo dictionary
            Dictionary dictionary = gson.fromJson(new FileReader("DictionaryJson.json"), Dictionary.class);
            forbiddenWords = dictionary.getForbiddenWords();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // arraylist in cui sono presenti tutti i client connessi al server
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    // socket utilizzato per stabilire la connessione e la comunicazione tra client
    // e server
    private Socket socket;

    // oggetto utilizzato per leggere i dati provenienti dal client
    private BufferedReader bufferedReader;

    // oggetto utilizzato per scrivere dati e mandarli al client
    private BufferedWriter bufferedWriter;

    String TXTFileName = "chatLog.txt";
    // file di testo in cui vengono riportare le conversazioni
    private FileWriter chatLogTXT;

    private String clientUsername;

    // costruttore
    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            // istanziazione del bufferedWriter
            this.bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            // istanziazione del bufferedReader
            this.bufferedReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            // attraverso il bufferedReader, si legge l'username del client
            this.clientUsername = bufferedReader.readLine();
            // aggiunta del client nell'arraylist
            clientHandlers.add(this);
            // variabile che tiene conto dei Client connessi in base all'arraylist
            int activeClients = clientHandlers.size();

            broadcastMessage("active clients : " + activeClients);
            // ottengo l'indirizzo IP pubblico dal client tramite il socket
            InetAddress clientIp = socket.getInetAddress();
            // messaggio in output quando un client si connette al server
            broadcastMessage(clientUsername + " has entered the chat from the Ip " + clientIp.getHostAddress());
            // creazione del file di testo
            chatLogTXT = new FileWriter(TXTFileName);
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    // metodo per registrare le conversazioni in un file di testo con il try with
    // resources, in modo tale che il
    // bufferedWriter venga chiuso ogni volta che il blocco try è finito
    public void addMessageToFileTXT(String messsage) {
        try (BufferedWriter writeMessageToFileTXT = new BufferedWriter(chatLogTXT);) {
            writeMessageToFileTXT.write(messsage);
            writeMessageToFileTXT.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // metodo per ricevere i dati dal client
    @Override
    public void run() {
        String messageFromClient;

        // fin quando il client è connesso
        while (socket.isConnected()) {
            try {
                // il messagio dal client viene letto dal bufferedReader
                messageFromClient = bufferedReader.readLine();
                byte[] charset = messageFromClient.getBytes("UTF-8");
                messageFromClient = new String(charset, "UTF-8");

                if (isAllowedToSendMessage()) {
                    // se il messaggio è valido, la variabile viene aggiornata da quando il
                    // messaggio viene inviato al client
                    lastMessageTime = System.currentTimeMillis();
                    // controlla se il messaggio contiene una delle parole presenti nel file JSON
                    for (String forbiddenWord : forbiddenWords) {
                        // se contiene una di quelle parole, la parola viene sostituita con "censored"
                        if (messageFromClient.contains(forbiddenWord)) {
                            messageFromClient = messageFromClient.replaceAll(forbiddenWord, "censored");
                        }
                    }
                    broadcastMessage(messageFromClient);
                    addMessageToFileTXT(messageFromClient);
                } else {
                    bufferedWriter.write("You are sending messages too quickly. Please wait a moment.");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }

                // se al ClientHandler arriva un messaggio che equivale ad "exit" rimuove il
                // client dall'arraylist di client connessi
                // e chiude le risorse
                if (messageFromClient.equalsIgnoreCase("exit")) {
                    removeClientHandler();
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }

            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    // messaggio di broadcast, che arriva a tutti gli altri client tranne al client
    // mittente
    public void broadcastMessage(String messageToSend) throws IOException {
        byte[] charset = messageToSend.getBytes("UTF-8");
        messageToSend = new String(charset, "UTF-8");
        for (ClientHandler clientHandler : clientHandlers) {
            try {

                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }

            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    // rimuove quel determinato client se non è più connesso al server
    public void removeClientHandler() throws IOException {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat");
    }

    // metodo per chiudere tutte le risore in caso di eccezione
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            // per evitare un NullPointerException
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (bufferedWriter != null) {
                bufferedWriter.close();
            }

            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
