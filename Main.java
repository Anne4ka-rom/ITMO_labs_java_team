import collection.CollectionManager;
import commands.CommandExecutor;
import file.FileManager;

/**
 * Главный класс программы. Точка входа в приложение.
 * Отвечает за инициализацию всех компонентов системы и запуск основного цикла обработки команд.
 * Ожидает имя файла с данными в качестве аргумента командной строки.
 *
 * @author Polina
 * @version 1.0
 */

public class Main {
    public static void main(String[] args) {
        try {
            // Проверка наличия аргумента с именем файла
            if (args.length == 0) {
                System.out.println("Ошибка: не указано имя файла с данными");
                System.out.println("Использование: java Main <filename>");
                System.exit(1); // завершает программу с ошибкой
            }

            // Получение имени файла
            String filename = args[0]; // берет первый аргумент командной строки как имя файла
            System.out.println("Загрузка данных из файла: " + filename);

            // Создание менеджера файлов и загрузка коллекции
            FileManager fileManager = new FileManager(filename);
            CollectionManager collectionManager = new CollectionManager(fileManager);

            // Запуск обработчика команд
            CommandExecutor executor = new CommandExecutor(collectionManager);
            executor.start(); // запускает цикл обработки команд пользователя

        } catch (Exception e) { // перехватывает любые исключения
            System.err.println("Критическая ошибка: " + e.getMessage());
            System.exit(1);
        }
    }
}