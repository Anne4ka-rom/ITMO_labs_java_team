package server; // объявление пакета server, где находится класс для отправки ответов клиенту

import common.Response; // импорт класса Response из пакета common (используется и клиентом, и сервером)

import java.io.IOException; // импорт исключения для ошибок ввода-вывода
import java.io.ObjectOutputStream; // импорт класса для записи сериализованных объектов в поток
import java.net.Socket; // импорт класса сокета для сетевого соединения

/**
 * Модуль отправки ответа клиенту
 * 
 * Отвечает за сериализацию объекта Response в поток сокета
 * Использует {@link ObjectOutputStream} для записи сериализованных объектов, которые будут отправлены клиенту по сети
 * 
 * Класс следует паттерну "декоратор", оборачивая выходной поток сокета в поток для записи объектов
 * Каждый экземпляр ResponseSender связан с одним конкретным клиентским сокетом
 * 
 * Клиент должен первым создавать {@link java.io.ObjectOutputStream}, а сервер - {@link java.io.ObjectInputStream} для чтения
 * ResponseSender создаётся после того, как клиент создал свои потоки
 * 
 * @author Anni
 * @version 1.0
 * @see Response
 * @see ObjectOutputStream
 * @see server.RequestReader
 */
public class ResponseSender { // объявление класса с именем ResponseSender
    private final ObjectOutputStream oos; // финальное поле для записи сериализованных объектов (инициализируется в конструкторе)
    
    /**
     * Конструктор - создает ObjectOutputStream из сокета
     * 
     * Инициализирует внутренний поток для записи сериализованных объектов на основе выходного потока переданного сокета
     * 
     * После создания ObjectOutputStream автоматически записывает заголовок в поток, в который будут отправляться ответы
     * Клиент должен прочитать этот заголовок своим ObjectInputStream
     * 
     * @param socket сокет подключенного клиента
     * @throws IOException если произошла ошибка при создании ObjectOutputStream
     */
    public ResponseSender(Socket socket) throws IOException { // конструктор класса ResponseSender, принимает сокет клиента и создаёт ObjectOutputStream для записи сериализованных объектов
        this.oos = new ObjectOutputStream(socket.getOutputStream()); // получение выходного потока из сокета и обёртка в objectoutputstream
    }
    
    /**
     * Сериализует и отправляет ответ клиенту
     * 
     * Метод преобразует объект {@link Response} в последовательность байт (сериализация) и записывает её в выходной поток сокета
     * После записи вызывает {@link ObjectOutputStream#flush()}, чтобы немедленно отправить все данные клиенту
     * 
     * Метод не блокируется надолго, так как запись в сетевой поток обычно происходит быстро, но при переполнении буфера может ждать
     * 
     * @param response объект Response для отправки клиенту (не должен быть null)
     * @throws IOException если произошла ошибка при записи в поток
     * @see Response
     * @see ObjectOutputStream#writeObject(Object)
     * @see ObjectOutputStream#flush()
     */
    public void sendResponse(Response response) throws IOException { // метод для сериализации и отправки ответа клиенту, принимает объект Response
        oos.writeObject(response); // запись сериализованного объекта response в выходной поток
        oos.flush(); // принудительная отправка всех буферизованных данных клиенту (очистка буфера)
    }
    
    /**
     * Закрывает поток вывода
     * 
     * Освобождает системные ресурсы, связанные с ObjectOutputStream
     * После вызова этого метода дальнейшая отправка ответов станет невозможной
     * 
     * Метод безопасно обрабатывает ситуацию, когда поток уже был закрыт или не был инициализирован
     * 
     * @throws IOException если произошла ошибка при закрытии потока
     */
    public void close() throws IOException { // метод для закрытия потока вывода, освобождает системные ресурсы
        if (oos != null) { // проверка, что поток существует (не равен null)
            oos.close(); // закрытие objectoutputstream (освобождает системные ресурсы)
        }
    }
}