package server.threads; // класс находится в пакете threads серверной части

import java.util.concurrent.*; // импорт всех классов для многопоточности и пулов потоков

/**
 * Управляет тремя пулами потоков для разных типов задач на сервере
 * readPool (ForkJoinPool) - для чтения данных от клиентов
 * processingPool (CachedThreadPool) - для обработки команд
 * writePool (ForkJoinPool) - для отправки ответов клиентам
 * 
 * @author Anni
 * @version 1.0
 */
public class ThreadPoolManager { // объявляет класс для управления пулами потоков
    private final ForkJoinPool readPool; // пул потоков forkjoinpool для задач чтения
    private final ExecutorService processingPool; // пул потоков cachedthreadpool для задач обработки
    private final ForkJoinPool writePool; // пул потоков forkjoinpool для задач записи

    /**
     * Создаёт и инициализирует все три пула потоков
     * Размер ForkJoinPool равен количеству доступных процессоров
     * CachedThreadPool создаёт потоки по мере необходимости
     */
    public ThreadPoolManager() { // конструктор класса, инициализирующий пулы потоков
        this.readPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors()); // создаём пул для чтения с размером = числу ядер процессора
        this.processingPool = Executors.newCachedThreadPool(); // создаём кэшируемый пул для обработки (расширяется при необходимости)
        this.writePool = new ForkJoinPool(Runtime.getRuntime().availableProcessors()); // создаём пул для записи с размером = числу ядер процессора
    }

    /**
     * Возвращает пул потоков для чтения данных от клиентов
     * 
     * @return ForkJoinPool для задач чтения
     */
    public ForkJoinPool getReadPool() { // геттер для пула чтения
        return readPool; // возвращаем пул для чтения
    }

    /**
     * Возвращает пул потоков для обработки команд
     * 
     * @return ExecutorService для задач обработки
     */
    public ExecutorService getProcessingPool() { // геттер для пула обработки
        return processingPool; // возвращаем пул для обработки
    }

    /**
     * Возвращает пул потоков для отправки ответов клиентам
     * 
     * @return ForkJoinPool для задач записи
     */
    public ForkJoinPool getWritePool() { // геттер для пула записи
        return writePool; // возвращаем пул для записи
    }

    /**
     * Корректно завершает работу всех пулов потоков
     * Сначала вызывает shutdown() для запрета новых задач
     * Затем ожидает завершения текущих задач до 5 секунд
     * Если задачи не завершились - вызывает shutdownNow() для принудительной остановки
     */
    public void shutdown() { // метод завершения работы всех пулов
        readPool.shutdown(); // завершаем пул чтения (новые задачи не принимаются)
        processingPool.shutdown(); // завершаем пул обработки (новые задачи не принимаются)
        writePool.shutdown(); // завершаем пул записи (новые задачи не принимаются)

        try { // начало блока перехвата исключений
            if (!readPool.awaitTermination(5, TimeUnit.SECONDS)) { // ожидаем завершения задач в пуле чтения до 5 секунд
                readPool.shutdownNow(); // если не завершились - принудительно останавливаем
            }
            if (!processingPool.awaitTermination(5, TimeUnit.SECONDS)) { // ожидаем завершения задач в пуле обработки до 5 секунд
                processingPool.shutdownNow(); // если не завершились - принудительно останавливаем
            }
            if (!writePool.awaitTermination(5, TimeUnit.SECONDS)) { // ожидаем завершения задач в пуле записи до 5 секунд
                writePool.shutdownNow(); // если не завершились - принудительно останавливаем
            }
        } catch (InterruptedException e) { // обрабатываем прерывание потока во время ожидания
            readPool.shutdownNow(); // принудительно останавливаем пул чтения
            processingPool.shutdownNow(); // принудительно останавливаем пул обработки
            writePool.shutdownNow(); // принудительно останавливаем пул записи
            Thread.currentThread().interrupt(); // восстанавливаем статус прерывания текущего потока
        }
    }
}