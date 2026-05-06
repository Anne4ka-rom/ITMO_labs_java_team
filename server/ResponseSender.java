package server; // класс находится в пакете server

import java.io.*; // импорт всех классов для работы с потоками ввода-вывода
import java.nio.ByteBuffer; // импорт для работы с буфером байтов в nio
import java.nio.channels.SocketChannel; // импорт канала для tcp-соединения

/**
 * Отправляет ответы клиентам через неблокирующий канал
 * Поддерживает автоматическую буферизацию и разбиение больших ответов на части
 * Использует формат: длина данных (4 байта) + сериализованный объект
 * 
 * @author Anni
 * @version 2.3
 */
public class ResponseSender { // объявляет класс для отправки ответов клиентам
    
    /**
     * Отправляет ответ клиенту с использованием неблокирующего ввода-вывода
     * Автоматически разбивает большие ответы на части и управляет буферизацией
     * 
     * @param handler обработчик клиента, содержащий ответ для отправки
     * @return true если ответ полностью отправлен, false если отправка не завершена
     * @throws IOException если произошла ошибка при записи в канал
     */
    public static boolean sendResponse(ClientHandler handler) throws IOException { // статический метод отправки ответа
        if (!handler.hasResponseToSend()) { // проверяем, есть ли ответ для отправки
            return true; // возвращаем true, так как ничего отправлять не нужно
        }
        
        ByteBuffer buffer = handler.getOutgoingBuffer(); // получаем текущий исходящий буфер из обработчика
        
        if (buffer == null) { // проверяем, нужно ли создать новый буфер
            Object response = handler.getResponseToSend(); // получаем объект ответа из обработчика
            byte[] data = serializeWithLength(response); // сериализуем ответ с добавлением длины в начало
            buffer = ByteBuffer.wrap(data); // оборачиваем массив байтов в буфер
            handler.setOutgoingBuffer(buffer); // сохраняем буфер в обработчике
        }
        
        SocketChannel channel = handler.getChannel(); // получаем канал клиента
        channel.write(buffer); // записываем данные из буфера в канал
        
        if (!buffer.hasRemaining()) { // проверяем, все ли данные были отправлены
            handler.setOutgoingBuffer(null); // очищаем буфер в обработчике
            handler.clearResponseToSend(); // удаляем отправленный ответ из обработчика
            return true; // возвращаем true - отправка завершена
        }
        
        return false; // возвращаем false - отправка не завершена, данных больше не поместилось
    }
    
    /**
     * Сериализует объект в массив байтов и добавляет в начало длину сериализованных данных
     * Формат: 4 байта (длина) + сериализованный объект
     * 
     * @param obj объект для сериализации
     * @return массив байтов с длиной в начале, за которой следует сериализованный объект
     * @throws IOException если произошла ошибка при сериализации объекта
     */
    private static byte[] serializeWithLength(Object obj) throws IOException { // статический метод сериализации с длиной
        byte[] serializedData; // массив для хранения сериализованных данных
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); // создаем поток для записи в массив байтов
             ObjectOutputStream oos = new ObjectOutputStream(bos)) { // создаем поток для сериализации объектов
            oos.writeObject(obj); // записываем объект в поток сериализации
            oos.flush(); // принудительно сбрасываем буфер для гарантии записи всех данных
            serializedData = bos.toByteArray(); // получаем массив сериализованных байтов
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(4 + serializedData.length); // выделяем буфер под длину (4 байта) и данные
        buffer.putInt(serializedData.length); // записываем длину данных в начало буфера
        buffer.put(serializedData); // записываем сами сериализованные данные после длины
        return buffer.array(); // возвращаем массив байтов из буфера
    }
}