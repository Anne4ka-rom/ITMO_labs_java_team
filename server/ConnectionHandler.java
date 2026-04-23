package server; // объявление пакета server, где находится класс для обработки подключения клиента

import common.Request; // импорт класса Request из пакета common (содержит команду от клиента)
import common.Response; // импорт класса Response из пакета common (содержит результат обработки)
import common.ResponseStatus; // импорт перечисления статусов ответа
import server.RequestReader; // импорт класса для чтения запроса от клиента
import server.ResponseSender; // импорт класса для отправки ответа клиенту

import java.io.IOException; // импорт исключения для ошибок ввода-вывода
import java.net.Socket; // импорт класса сокета для сетевого соединения

/**
 * Модуль обработки подключения
 * 
 * Обрабатывает одного клиента: читает запрос, обрабатывает команду через {@link CommandProcessor} и отправляет ответ обратно клиенту
 * 
 * Класс реализует интерфейс {@link Runnable}, что позволяет запускать каждого клиента в отдельном потоке
 * Это обеспечивает параллельную обработку нескольких клиентов одновременно
 * 
 * Цикл обработки клиента:
 * - Создание {@link RequestReader} и {@link ResponseSender}
 * - Чтение запроса от клиента
 * - Обработка команды через CommandProcessor
 * - Отправка ответа клиенту
 * - Закрытие всех ресурсов в блоке finally
 * 
 * @author Anni
 * @version 1.0
 * @see Runnable
 * @see CommandProcessor
 * @see RequestReader
 * @see ResponseSender
 */
public class ConnectionHandler implements Runnable { // объявление класса ConnectionHandler, реализующего интерфейс Runnable
    private final Socket clientSocket; // финальное поле сокета клиента (хранит соединение с конкретным клиентом)
    private final CommandProcessor commandProcessor; // финальное поле процессора команд (обрабатывает команды из запросов)
    
    /**
     * Конструктор обработчика подключения
     * 
     * Сохраняет переданные параметры в поля класса для дальнейшего использования в методе {@link #run()}
     * 
     * @param clientSocket сокет подключенного клиента (уже установленное соединение)
     * @param commandProcessor процессор команд (общий для всех клиентов)
     */
    public ConnectionHandler(Socket clientSocket, CommandProcessor commandProcessor) { // конструктор класса, принимает сокет клиента и процессор команд
        this.clientSocket = clientSocket; // сохранение сокета клиента в поле класса
        this.commandProcessor = commandProcessor; // сохранение процессора команд в поле класса
    }
    
    /**
     * Основной метод обработки клиента
     * 
     * Вызывается автоматически при запуске потока
     * Содержит всю логику взаимодействия с одним клиентом: от чтения запроса до отправки ответа
     * 
     * При возникновении ошибки пытается отправить клиенту сообщение об ошибке
     * В блоке {@code finally} гарантированно закрывает все ресурсы (потоки ввода-вывода и сокет)
     */
    @Override // аннотация, указывающая на переопределение метода родительского интерфейса
    public void run() { // метод run() из интерфейса Runnable
        System.out.println("Обработка клиента: " + clientSocket.getInetAddress()); // вывод в консоль информации о подключившемся клиенте (его ip-адрес)
        
        RequestReader reader = null; // объявление переменной для чтения запросов (инициализация null для корректного закрытия в finally)
        ResponseSender sender = null; // объявление переменной для отправки ответов (инициализация null для корректного закрытия в finally)
        
        try { // начало блока перехвата исключений
            // 1. Создание модулей чтения и отправки
            reader = new RequestReader(clientSocket); // создание объекта для чтения запросов из сокета клиента
            sender = new ResponseSender(clientSocket); // создание объекта для отправки ответов в сокет клиента
            
            // 2. Чтение запроса от клиента
            Request request = reader.readRequest(); // чтение и десериализация запроса от клиента
            System.out.println("Получена команда: " + request.getCommand().getType()); // вывод в консоль типа полученной команды
            
            // 3. Обработка команды
            Response response = commandProcessor.process(request.getCommand()); // передача команды процессору и получение результата обработки
            
            // 4. Отправка ответа клиенту
            sender.sendResponse(response); // сериализация и отправка ответа клиенту
            System.out.println("Ответ отправлен. Статус: " + response.getStatus()); // вывод в консоль статуса отправленного ответа
            
        } catch (IOException | ClassNotFoundException e) { // обработка ошибок ввода-вывода или ошибок десериализации (класс не найден)
            System.err.println("Ошибка при обработке клиента: " + e.getMessage()); // вывод сообщения об ошибке в стандартный поток ошибок
            try { // попытка отправить клиенту сообщение об ошибке
                if (sender != null) { // проверка, что отправитель был создан (не равен null)
                    sender.sendResponse(new Response(ResponseStatus.ERROR, // создание нового ответа со статусом ошибки
                        "Ошибка сервера: " + e.getMessage())); // текст ошибки с деталями от исключения
                }
            } catch (IOException ex) { // обработка ошибки при отправке сообщения об ошибке
                System.err.println("Не удалось отправить сообщение об ошибке"); // вывод сообщения о неудачной отправке
            }
        } finally { // блок, который выполнится всегда
            // 5. Закрытие ресурсов
            try { // начало блока для закрытия всех ресурсов с обработкой ошибок
                if (reader != null) reader.close(); // закрытие потока чтения (если он был создан)
                if (sender != null) sender.close(); // закрытие потока отправки (если он был создан)
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close(); // закрытие сокета клиента (если он существует и ещё не закрыт)
            } catch (IOException e) { // обработка ошибок при закрытии ресурсов
                System.err.println("Ошибка при закрытии соединения: " + e.getMessage()); // вывод сообщения об ошибке закрытия
            }
        }
    }
}