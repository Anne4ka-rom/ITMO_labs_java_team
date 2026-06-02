package server; // класс находится в пакете server

import java.io.ByteArrayOutputStream; // импорт для работы с массивом байтов как с потоком вывода
import java.nio.ByteBuffer; // импорт для работы с буфером байтов в nio
import java.nio.channels.Selector; // импорт мультиплексора каналов для неблокирующего ввода-вывода
import java.nio.channels.SocketChannel; // импорт канала для tcp-соединения

/**
 * Хранит состояние подключения одного клиента
 * 
 * @author Anni
 * @version 2.3
 */
public class ClientHandler { // объявляет класс для обработки клиентских подключений
    private final SocketChannel channel; // канал для связи с клиентом (финальное поле, инициализируется в конструкторе)
    private final Selector selector; // мультиплексор для управления этим клиентом (финальное поле)
    private final ByteArrayOutputStream pendingData; // буфер для накопления входящих данных
    private ByteBuffer outgoingBuffer; // буфер для отправки исходящих данных
    private byte[] completeRequest; // массив для хранения полностью полученного запроса
    private Object responseToSend; // объект ответа, ожидающий отправки клиенту
    private boolean isProcessing; // флаг, указывающий, обрабатывается ли запрос клиента в данный момент
    
    /**
     * Создает новый обработчик для подключенного клиента
     * 
     * @param channel канал для связи с клиентом
     * @param selector мультиплексор, управляющий этим каналом
     */
    public ClientHandler(SocketChannel channel, Selector selector) { // конструктор класса
        this.channel = channel; // сохраняем канал клиента
        this.selector = selector; // сохраняем ссылку на мультиплексор
        this.pendingData = new ByteArrayOutputStream(); // создаем новый поток для накопления данных
        this.outgoingBuffer = null; // инициализируем исходящий буфер как null (пока нет данных для отправки)
        this.completeRequest = null; // инициализируем полный запрос как null
        this.responseToSend = null; // инициализируем ответ как null
        this.isProcessing = false; // устанавливаем флаг обработки в false
    }
    
    /**
     * Возвращает мультиплексор, связанный с этим клиентом
     * 
     * @return мультиплексор селектора
     */
    public Selector getSelector() { // геттер для поля selector
        return selector; // возвращаем сохраненный мультиплексор
    }
    
    /**
     * Возвращает канал для связи с клиентом
     * 
     * @return канал сокета
     */
    public SocketChannel getChannel() { // геттер для поля channel
        return channel; // возвращаем сохраненный канал
    }
    
    /**
     * Возвращает поток накопленных входящих данных
     * 
     * @return поток байтов с ожидающими обработки данными
     */
    public ByteArrayOutputStream getPendingData() { // геттер для поля pendingData
        return pendingData; // возвращаем поток для накопления данных
    }
    
    /**
     * Устанавливает буфер для отправки исходящих данных
     * 
     * @param buffer буфер с данными для отправки
     */
    public void setOutgoingBuffer(ByteBuffer buffer) { // сеттер для поля outgoingBuffer
        this.outgoingBuffer = buffer; // сохраняем переданный буфер
    }
    
    /**
     * Возвращает текущий буфер исходящих данных
     * 
     * @return буфер для отправки или null если нет данных
     */
    public ByteBuffer getOutgoingBuffer() { // геттер для поля outgoingBuffer
        return outgoingBuffer; // возвращаем исходящий буфер
    }
    
    /**
     * Сохраняет полностью полученный запрос от клиента
     * 
     * @param data массив байтов с полным запросом
     */
    public void setCompleteRequest(byte[] data) { // сеттер для поля completeRequest
        this.completeRequest = data; // сохраняем переданный массив байтов
    }
    
    /**
     * Возвращает полностью полученный запрос клиента
     * 
     * @return массив байтов с запросом или null если запроса нет
     */
    public byte[] getCompleteRequest() { // геттер для поля completeRequest
        return completeRequest; // возвращаем массив с запросом
    }
    
    /**
     * Проверяет, имеется ли полностью полученный запрос
     * 
     * @return true если есть полный запрос, false если нет
     */
    public boolean hasCompleteRequest() { // метод проверки наличия полного запроса
        return completeRequest != null; // возвращаем true если массив не null
    }
    
    /**
     * Очищает сохраненный полный запрос
     * Вызывается после обработки запроса
     */
    public void clearCompleteRequest() { // метод очистки запроса
        this.completeRequest = null; // устанавливаем ссылку в null
    }
    
    /**
     * Устанавливает объект ответа для отправки клиенту
     * 
     * @param response объект, который будет отправлен клиенту
     */
    public void setResponseToSend(Object response) { // сеттер для поля responseToSend
        this.responseToSend = response; // сохраняем переданный объект ответа
    }
    
    /**
     * Возвращает объект ответа, ожидающий отправки
     * 
     * @return объект ответа или null если ответа нет
     */
    public Object getResponseToSend() { // геттер для поля responseToSend
        return responseToSend; // возвращаем объект ответа
    }
    
    /**
     * Проверяет, есть ли ответ для отправки клиенту
     * 
     * @return true если есть ответ, false если нет
     */
    public boolean hasResponseToSend() { // метод проверки наличия ответа
        return responseToSend != null; // возвращаем true если объект ответа не null
    }
    
    /**
     * Очищает объект ответа после отправки
     */
    public void clearResponseToSend() { // метод очистки ответа
        this.responseToSend = null; // устанавливаем ссылку в null
    }
    
    /**
     * Проверяет, обрабатывается ли в данный момент запрос клиента
     * 
     * @return true если запрос в обработке, false если нет
     */
    public boolean isProcessing() { // геттер для поля isProcessing
        return isProcessing; // возвращаем текущее состояние флага обработки
    }
    
    /**
     * Устанавливает флаг обработки запроса клиента
     * 
     * @param processing новое состояние флага обработки
     */
    public void setProcessing(boolean processing) { // сеттер для поля isProcessing
        this.isProcessing = processing; // сохраняем переданное состояние
    }
    
    /**
     * Закрывает соединение с клиентом и очищает все данные
     * Освобождает ресурсы, связанные с этим клиентом
     */
    public void close() { // метод закрытия и очистки ресурсов
        if (pendingData != null) { // проверяем, существует ли поток накопленных данных
            pendingData.reset(); // очищаем накопленные байты, но не закрываем поток
        }
        outgoingBuffer = null; // удаляем ссылку на исходящий буфер
        completeRequest = null; // удаляем ссылку на запрос
        responseToSend = null; // удаляем ссылку на ответ
        isProcessing = false; // сбрасываем флаг обработки
    }
}