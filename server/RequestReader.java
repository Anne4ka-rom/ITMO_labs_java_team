package server; // класс находится в пакете server

import common.Request; // импорт класса запроса из общей модели

import java.io.*; // импорт всех классов для работы с потоками ввода-вывода
import java.nio.ByteBuffer; // импорт для работы с буфером байтов в nio
import java.nio.channels.SocketChannel; // импорт канала для tcp-соединения

/**
 * Читает запросы от клиентов через неблокирующий канал
 * Поддерживает накопление частично полученных данных и извлечение полных сообщений
 * Использует формат: длина данных (4 байта) + сериализованный объект Request
 * 
 * @author Anni
 * @version 2.3
 */
public class RequestReader { // объявляет класс для чтения запросов от клиентов
    private static final int BUFFER_SIZE = 65536; // размер буфера для чтения данных (64 кб)
    
    /**
     * Читает данные из канала клиента и накапливает их в обработчике
     * При обнаружении полных сообщений извлекает их и сохраняет в обработчике
     * 
     * @param handler обработчик клиента, содержащий канал и буфер данных
     * @throws IOException если произошла ошибка чтения или клиент закрыл соединение
     */
    public static void readRequest(ClientHandler handler) throws IOException { // статический метод чтения запроса
        SocketChannel channel = handler.getChannel(); // получаем канал клиента из обработчика
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE); // создаем буфер фиксированного размера
        
        int bytesRead = channel.read(buffer); // читаем данные из канала в буфер
        
        if (bytesRead == -1) { // проверяем, закрыл ли клиент соединение
            throw new IOException("Клиент закрыл соединение"); // выбрасываем исключение
        }
        
        if (bytesRead > 0) { // проверяем, были ли прочитаны данные
            buffer.flip(); // переключаем буфер из режима записи в режим чтения
            byte[] data = new byte[bytesRead]; // создаем массив для прочитанных байтов
            buffer.get(data); // копируем данные из буфера в массив
            handler.getPendingData().write(data); // записываем данные в накопительный буфер обработчика
            
            extractCompleteMessages(handler); // пытаемся извлечь полные сообщения из накопленных данных
        }
    }
    
    /**
     * Извлекает полные сообщения из потока накопленных байтов
     * Формат сообщения: 4 байта длины + тело сообщения
     * При обнаружении полного сообщения сохраняет его в обработчике
     * 
     * @param handler обработчик клиента с накопленными данными
     */
    private static void extractCompleteMessages(ClientHandler handler) { // статический метод извлечения полных сообщений
        byte[] fullData = handler.getPendingData().toByteArray(); // получаем все накопленные байты в виде массива
        ByteArrayInputStream bais = new ByteArrayInputStream(fullData); // создаем поток для чтения из массива
        DataInputStream dis = new DataInputStream(bais); // оборачиваем в поток для удобного чтения примитивов
        
        try { // начало блока перехвата исключений
            bais.mark(fullData.length); // запоминаем текущую позицию для возможного отката
            
            while (true) { // бесконечный цикл обработки сообщений
                int messageLength; // переменная для длины сообщения
                try { // попытка прочитать длину сообщения
                    messageLength = dis.readInt(); // читаем 4 байта - длину сообщения
                } catch (EOFException e) { // если достигнут конец потока (не хватает данных)
                    bais.reset(); // откатываемся к сохраненной позиции
                    break; // выходим из цикла
                }
                
                if (messageLength <= 0 || messageLength > 10 * 1024 * 1024) { // проверяем корректность длины (не более 10 мб)
                    handler.getPendingData().reset(); // сбрасываем накопленные данные при ошибке
                    return; // выходим из метода
                }
                
                byte[] messageData = new byte[messageLength]; // создаем массив для тела сообщения
                try { // попытка прочитать тело сообщения
                    dis.readFully(messageData); // читаем ровно messageLength байт
                } catch (EOFException e) { // если не хватило данных для полного сообщения
                    bais.reset(); // откатываемся к сохраненной позиции
                    break; // выходим из цикла
                }
                
                handler.setCompleteRequest(messageData); // сохраняем полное сообщение в обработчике
                
                byte[] remaining = new byte[bais.available()]; // создаем массив для оставшихся данных
                bais.read(remaining); // читаем все оставшиеся байты
                handler.getPendingData().reset(); // сбрасываем накопленные данные
                handler.getPendingData().write(remaining); // записываем обратно только непрочитанные данные
                
                bais = new ByteArrayInputStream(remaining); // создаем новый поток для оставшихся данных
                dis = new DataInputStream(bais); // создаем новый дата-поток для оставшихся данных
                bais.mark(remaining.length); // сохраняем позицию для нового потока
            }
        } catch (IOException e) { // обрабатываем любые ошибки ввода-вывода
            handler.getPendingData().reset(); // сбрасываем накопленные данные при ошибке
        }
    }
    
    /**
     * Десериализует массив байтов в объект Request
     * Проверяет, что десериализованный объект имеет правильный тип
     * 
     * @param data массив байтов для десериализации
     * @return десериализованный объект Request
     * @throws IOException если произошла ошибка ввода-вывода или объект имеет неверный тип
     * @throws ClassNotFoundException если класс запроса не найден
     */
    public static Request deserializeRequest(byte[] data) throws IOException, ClassNotFoundException { // статический метод десериализации запроса
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data); // создаем поток из массива байтов
            ObjectInputStream ois = new ObjectInputStream(bis)) { // создаем поток для десериализации объектов
            Object obj = ois.readObject(); // читаем объект из потока
            if (obj instanceof Request) { // проверяем, является ли объект типом Request
                return (Request) obj; // возвращаем приведенный к типу Request объект
            }
            throw new IOException("Получен объект не типа Request"); // выбрасываем исключение о неверном типе
        }
    }
}