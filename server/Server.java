package server;

import server.collection.CollectionManager;
import server.file.FileManager;
import java.net.*;
import java.io.*;

public class Server {
    public static final int PORT = 8080;
    private CollectionManager collectionManager;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Укажите название файла с данными");
            System.exit(1);
        }

        Server server = new Server();
        server.start(args[0]);
    }

    public void start(String filename) {
        // TODO: реалитзовать запуск
        System.out.println("Сервер запущен на порту " + PORT);
    }
}