package server; // объявление пакета server, где находится класс для чтения запросов на сервере

import common.Request; // импорт класса Request из пакета common (используется и клиентом, и сервером)

import java.io.IOException; // импорт исключения для ошибок ввода-вывода
import java.io.ObjectInputStream; // импорт класса для чтения сериализованных объектов из потока
import java.net.Socket; // импорт класса сокета для сетевого соединения

/**
 * Модуль чтения запроса от клиента
 * 
 * Отвечает за десериализацию объекта {@link Request} из потока сокета
 * Использует {@link ObjectInputStream} для чтения сериализованных объектов, переданных клиентом по сети
 * 
 * Класс следует паттерну "декоратор", оборачивая входной поток сокета в поток для чтения объектов
 * Каждый экземпляр RequestReader связан с одним конкретным клиентским сокетом
 * 
 * @author Anni
 * @version 1.0
 * @see Request
 * @see ObjectInputStream
 * @see server.ResponseSender
 */
public class RequestReader { // объявление класса с именем RequestReader
    private final ObjectInputStream ois; // финальное поле для чтения сериализованных объектов (инициализируется в конструкторе)
    
    /**
     * Конструктор - создает ObjectInputStream из сокета
     * 
     * Инициализирует внутренний поток для чтения сериализованных объектов на основе входного потока переданного сокета
     * Клиент должен первым создавать {@link java.io.ObjectOutputStream}, а сервер - {@link ObjectInputStream}, иначе может возникнуть deadlock
     * 
     * @param socket сокет подключенного клиента, из которого будут читаться запросы
     * @throws IOException если произошла ошибка при создании ObjectInputStream
     */
    public RequestReader(Socket socket) throws IOException { // конструктор класса RequestReader, принимает сокет клиента и создаёт ObjectInputStream для чтения сериализованных объектов
        this.ois = new ObjectInputStream(socket.getInputStream()); // получение входного потока из сокета и обёртка в objectinputstream
    }
    
    /**
     * Читает и десериализует запрос от клиента
     * 
     * Метод блокирует выполнение потока до тех пор, пока от клиента не поступит сериализованный объект
     * После получения выполняет десериализацию и приводит результат к типу {@link Request}
     * При десериализации проверяется, что класс Request доступен в classpath
     * Ожидается, что все поля объекта были корректно сериализованы клиентом
     * 
     * @return объект Request, полученный от клиента
     * @throws IOException если произошла ошибка при чтении из потока
     * @throws ClassNotFoundException если класс Request или его зависимости не найдены в classpath сервера
     * @see common.Request
     */
    public Request readRequest() throws IOException, ClassNotFoundException { // метод для чтения и десериализации запроса от клиента, возвращает объект типа Request
        return (Request) ois.readObject(); // чтение сериализованного объекта из потока и приведение к типу Request
    }
    
    /**
     * Закрывает поток ввода
     * 
     * Освобождает системные ресурсы, связанные с ObjectInputStream
     * После вызова этого метода дальнейшее чтение запросов станет невозможным
     * Метод безопасно обрабатывает ситуацию, когда поток уже был закрыт или не был инициализирован
     * 
     * @throws IOException если произошла ошибка при закрытии потока
     */
    public void close() throws IOException { // метод для закрытия потока ввода, освобождает системные ресурсы
        if (ois != null) { // проверка, что поток существует (не равен null)
            ois.close(); // закрытие objectinputstream (освобождает системные ресурсы)
        }
    }
}