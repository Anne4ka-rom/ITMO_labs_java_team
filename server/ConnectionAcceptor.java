package server; // класс находится в пакете server

import java.io.IOException; // импорт исключения для ошибок ввода-вывода
import java.nio.channels.SelectionKey; // импорт ключа селектора для операций выбора
import java.nio.channels.Selector; // импорт мультиплексора каналов
import java.nio.channels.ServerSocketChannel; // импорт канала серверного сокета
import java.nio.channels.SocketChannel; // импорт канала клиентского сокета

/**
 * Модуль приёма подключений
 * Отвечает за принятие новых клиентских подключений
 * 
 * @author Anni
 * @version 1.1
 */
public class ConnectionAcceptor { // объявляет класс для принятия подключений
    
    /**
     * Принимает новое подключение от клиента
     * Настраивает канал в неблокирующий режим и создает обработчик для клиента
     * 
     * @param key ключ селектора для serversocketchannel
     * @param selector селектор для регистрации нового канала
     * @throws ioexception если произошла ошибка при приёме подключения
     */
    public static void accept(SelectionKey key, Selector selector) throws IOException { // статический метод для принятия подключений
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel(); // получаем серверный канал из ключа
        SocketChannel clientChannel = serverChannel.accept(); // принимаем новое клиентское подключение
        
        if (clientChannel != null) { // проверяем, что подключение успешно установлено
            clientChannel.configureBlocking(false); // переводим канал в неблокирующий режим
            
            ClientHandler handler = new ClientHandler(clientChannel, selector); // создаем обработчик для нового клиента
            clientChannel.register(selector, SelectionKey.OP_READ, handler); // регистрируем канал на чтение с привязкой обработчика
            
            System.out.println("Новый клиент подключён: " + clientChannel.getRemoteAddress()); // выводим адрес подключившегося клиента
        }
    }
}