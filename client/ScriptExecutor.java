package client;

import java.io.*;
import java.util.ArrayList;

/**
 * Отвечает за выполнение команд из файлов-скриптов.
 * Защищает от рекурсии и превышения максимальной глубины вложенности.
 *
 * @author Polina
 * @version 1.1
 */
public class ScriptExecutor {

    private static final int MAX_SCRIPT_DEPTH = 5; // максимальная глубина вложенности скриптов

    private int scriptDepth = 0; // текущая глубина вложенности скриптов (для ограничения рекурсии)
    private ArrayList<String> scriptStack = new ArrayList<>(); // стек выполняемых скриптов - отслеживает имена файлов (для защиты от рекурсии)

    private final VehicleReader vehicleReader; // нужен для переключения контекста чтения (консоль ↔ скрипт)
    // ссылка на CommandSender устанавливается отдельно, чтобы избежать циклической зависимости при создании
    private CommandSender commandSender;

    public ScriptExecutor(VehicleReader vehicleReader) {
        this.vehicleReader = vehicleReader;
    }

    // устанавливает ссылку на CommandSender (вызывается после его создания)
    public void setCommandSender(CommandSender commandSender) {
        this.commandSender = commandSender;
    }

    // выполняет скрипт из файла
    public void executeScript(String filename) {
        // проверка на глубину вложенности
        if (scriptDepth >= MAX_SCRIPT_DEPTH) {
            System.out.println("Ошибка: превышена максимальная глубина вложенности скриптов (" + MAX_SCRIPT_DEPTH + ")");
            return;
        }

        // проверка на рекурсию (один и тот же скрипт вызывает сам себя)
        if (scriptStack.contains(filename)) {
            System.out.println("Ошибка: обнаружена рекурсия (скрипт " + filename + " уже выполняется)");
            return;
        }

        scriptStack.add(filename); // добавляем в стек
        scriptDepth++; // увеличиваем глубину

        // ридер и флаг
        BufferedReader oldScriptReader = vehicleReader.scriptReader;
        boolean oldIsExecutingScript = vehicleReader.isExecutingScript;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            vehicleReader.setScriptContext(reader, true); // устанавливаем текущий ридер для чтения полей
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue; // пропускаем пустые строки и комментарии

                System.out.println("[Скрипт " + filename + ":" + lineNum + "] > " + line);
                commandSender.sendCommand(line); // выполняем команду из скрипта
            }

            System.out.println("Скрипт " + filename + " выполнен успешно");

        } catch (FileNotFoundException e) {
            System.out.println("Ошибка: файл не найден - " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + e.getMessage());
        } finally {
            scriptDepth--; // уменьшаем глубину
            scriptStack.remove(scriptStack.size() - 1); // убираем из стека
            vehicleReader.setScriptContext(oldScriptReader, oldIsExecutingScript); // восстанавливаем старый ридер
        }
    }
}