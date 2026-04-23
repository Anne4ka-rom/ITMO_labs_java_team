package server.file; // класс находится в папке file

import server.exceptions.InvalidDataException; // импорт пользовательского исключения для ошибок валидации данных

import java.io.*; // импорт классов для ввода/вывода (FileReader, FileOutputStream, IOException)
import java.nio.charset.StandardCharsets; // импорт для указания кодировки UTF-8
import java.time.LocalDate; // импорт для работы с датами
import java.util.Stack; // импорт коллекции Stack для хранения транспортных средств
import java.util.regex.Matcher; // импорт Matcher для работы с регулярными выражениями
import java.util.regex.Pattern; // импорт Pattern для создания регулярных выражений

import common.model.*;

/**
 * Управляет чтением и записью XML файла.
 * Использует FileReader и FileOutputStream согласно заданию.
 * 
 * @author Anni
 * @version 1.0
 */
public class FileManager { // объявляем класс для работы с файлами
    private final String filename; // приватное поле для хранения имени файла

    /**
     * Конструктор класса FileManager
     * 
     * @param filename имя XML файла для чтения/записи
     */
    public FileManager(String filename) { // конструктор класса FileManager
        this.filename = filename; // сохраняем переданное имя файла
    }

    /**
     * Сохраняет коллекцию в XML файл
     * 
     * @param collection коллекция транспортных средств для сохранения
     * @throws IOException если произошла ошибка ввода/вывода при записи файла
     */
    public void saveCollection(Stack<Vehicle> collection) throws IOException { // метод сохранения коллекции в файл
        StringBuilder xml = new StringBuilder(); // создаем StringBuilder для построения XML
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); // добавляем XML декларацию
        xml.append("<vehicles>\n"); // открывающий корневой тег

        for (Vehicle v : collection) { // проходим по всем элементам коллекции
            xml.append("  <vehicle>\n"); // открывающий тег vehicle с отступом 2 пробела
            xml.append("    <id>").append(v.getId()).append("</id>\n"); // тег id со значением
            xml.append("    <name>").append(escapeXml(v.getName())).append("</name>\n"); // тег name с экранированным значением
            xml.append("    <coordinates>\n"); // открывающий тег coordinates
            xml.append("      <x>").append(v.getCoordinates().getX()).append("</x>\n"); // тег x с координатой
            xml.append("      <y>").append(v.getCoordinates().getY()).append("</y>\n"); // тег y с координатой
            xml.append("    </coordinates>\n"); // закрывающий тег coordinates
            xml.append("    <creationDate>").append(v.getCreationDate()).append("</creationDate>\n"); // тег с датой создания
            xml.append("    <enginePower>").append(v.getEnginePower()).append("</enginePower>\n"); // тег с мощностью
            xml.append("    <capacity>").append(v.getCapacity()).append("</capacity>\n"); // тег с грузоподъемностью
            xml.append("    <type>").append(v.getType()).append("</type>\n"); // тег с типом ТС
            xml.append("    <fuelType>").append(v.getFuelType()).append("</fuelType>\n"); // тег с типом топлива
            xml.append("  </vehicle>\n"); // закрывающий тег vehicle
        }

        xml.append("</vehicles>"); // закрывающий корневой тег

        try (FileOutputStream fos = new FileOutputStream(filename)) { // try-with-resources для автоматического закрытия потока
            fos.write(xml.toString().getBytes(StandardCharsets.UTF_8)); // записываем XML в файл в кодировке UTF-8
        } // поток автоматически закрывается здесь
    }

    /**
     * Загружает коллекцию из XML файла
     * 
     * @return загруженная коллекция транспортных средств (Stack<Vehicle>)
     * @throws IOException если произошла ошибка ввода/вывода при чтении файла
     * @throws InvalidDataException если данные в файле не прошли валидацию
     */
    public Stack<Vehicle> loadCollection() throws IOException, InvalidDataException { // метод загрузки коллекции из файла
        Stack<Vehicle> collection = new Stack<>(); // создаем новую пустую коллекцию Stack
        StringBuilder content = new StringBuilder(); // StringBuilder для чтения содержимого файла

        try (FileReader reader = new FileReader(filename)) { // try-with-resources для FileReader
            int ch; // переменная для хранения очередного символа
            while ((ch = reader.read()) != -1) { // читаем символы до конца файла (-1 означает конец)
                content.append((char) ch); // добавляем символ в StringBuilder (преобразуем int в char)
            }
        }

        String xml = content.toString(); // преобразуем StringBuilder в строку
        parseVehicles(xml, collection); // парсим XML и заполняем коллекцию
        return collection; // возвращаем загруженную коллекцию
    }

    /**
     * Парсит XML и заполняет коллекцию
     * 
     * @param xml строка с XML данными
     * @param collection коллекция для заполнения
     * @throws InvalidDataException если данные не прошли валидацию
     */
    private void parseVehicles(String xml, Stack<Vehicle> collection) throws InvalidDataException { // приватный метод парсинга XML
        Pattern vehiclePattern = Pattern.compile("<vehicle>(.*?)</vehicle>", Pattern.DOTALL); // регулярное выражение для поиска тегов vehicle
        Matcher vehicleMatcher = vehiclePattern.matcher(xml); // создаем Matcher для поиска по XML

        while (vehicleMatcher.find()) { // пока находим очередной тег vehicle
            String vehicleXml = vehicleMatcher.group(1); // извлекаем содержимое тега vehicle (без самих тегов)
            try { // блок try для обработки ошибок парсинга
                Vehicle vehicle = parseVehicle(vehicleXml); // парсим отдельный vehicle из его XML
                collection.push(vehicle); // добавляем vehicle в коллекцию
            } catch (Exception e) { // ловим любые исключения при парсинге
                throw new InvalidDataException("Ошибка парсинга vehicle: " + e.getMessage()); // выбрасываем исключение с сообщением
            }
        }
    }

    /**
     * Парсит один элемент Vehicle
     * 
     * @param xml строка с XML данными одного vehicle
     * @return объект Vehicle, созданный из XML
     * @throws InvalidDataException если данные не прошли валидацию
     */
    private Vehicle parseVehicle(String xml) throws InvalidDataException { // приватный метод парсинга одного Vehicle
        Vehicle vehicle = new Vehicle(); // создаем новый объект Vehicle

        // ID
        String idStr = extractTag(xml, "id"); // извлекаем содержимое тега id
        if (idStr != null) { // если тег найден
            try { // блок try для парсинга числа
                int id = Integer.parseInt(idStr); // преобразуем строку в int
                if (id <= 0) { // проверяем, что id положительный
                    throw new InvalidDataException("ID должен быть > 0"); // выбрасываем исключение, если ID не прошел валидацию (id <= 0)
                } 
                vehicle.setId(id); // устанавливаем id
            } catch (NumberFormatException e) { // если не удалось преобразовать в число
                throw new InvalidDataException("Неверный формат ID"); // выбрасываем исключение
            }
        }

        // Name
        String name = extractTag(xml, "name"); // извлекаем содержимое тега name
        if (name == null || name.trim().isEmpty()) { // проверяем, что имя не null и не пустое
            throw new InvalidDataException("Имя не может быть пустым"); // выбрасываем исключение
        }
        vehicle.setName(name); // устанавливаем имя

        // Coordinates
        String coordsXml = extractTag(xml, "coordinates"); // извлекаем содержимое тега coordinates
        if (coordsXml == null) { // если координаты отсутствуют
            throw new InvalidDataException("Отсутствуют координаты"); // выбрасываем исключение
        }
        vehicle.setCoordinates(parseCoordinates(coordsXml)); // парсим координаты и устанавливаем

        // Creation Date
        String dateStr = extractTag(xml, "creationDate"); // извлекаем содержимое тега creationDate
        if (dateStr != null) { // если тег найден
            vehicle.setCreationDate(LocalDate.parse(dateStr)); // парсим дату и устанавливаем
        }

        // Engine Power
        String powerStr = extractTag(xml, "enginePower"); // извлекаем содержимое тега enginePower
        if (powerStr != null) { // если тег найден
            try { // блок try для парсинга числа
                Double power = Double.parseDouble(powerStr); // преобразуем строку в Double
                if (power <= 0) throw new InvalidDataException("Engine power должен быть > 0"); // проверяем, что мощность положительная
                vehicle.setEnginePower(power); // устанавливаем мощность
            } catch (NumberFormatException e) { // если не удалось преобразовать в число
                throw new InvalidDataException("Неверный формат enginePower"); // выбрасываем исключение;
            }
        }

        // Capacity
        String capacityStr = extractTag(xml, "capacity"); // извлекаем содержимое тега capacity
        if (capacityStr != null) { // если тег найден
            try { // блок try для парсинга числа
                double capacity = Double.parseDouble(capacityStr); // преобразуем строку в double
                if (capacity <= 0) throw new InvalidDataException("Capacity должна быть > 0"); // проверяем, что грузоподъемность положительная
                vehicle.setCapacity(capacity); // устанавливаем грузоподъемность
            } catch (NumberFormatException e) { // если не удалось преобразовать в число
                throw new InvalidDataException("Неверный формат capacity"); // выбрасываем исключение
            }
        }

        // Type
        String typeStr = extractTag(xml, "type"); // извлекаем содержимое тега type
        if (typeStr != null) { // если тег найден
            try { // блок try для преобразования в enum
                vehicle.setType(VehicleType.valueOf(typeStr)); // преобразуем строку в VehicleType и устанавливаем
            } catch (IllegalArgumentException e) { // если нет такой константы в enum
                throw new InvalidDataException("Неверный тип vehicle"); // выбрасываем исключение
            }
        }

        // Fuel Type (может быть null)
        String fuelStr = extractTag(xml, "fuelType"); // извлекаем содержимое тега fuelType
        if (fuelStr != null && !fuelStr.equals("null")) { // если тег найден и это не строка "null"
            try { // блок try для преобразования в enum
                vehicle.setFuelType(FuelType.valueOf(fuelStr)); // преобразуем строку в FuelType и устанавливаем
            } catch (IllegalArgumentException e) { // если нет такой константы
                vehicle.setFuelType(null); // устанавливаем null (топливо не указано)
            }
        }
        return vehicle; // возвращаем созданный объект Vehicle
    }

    /**
     * Парсит координаты
     * 
     * @param xml строка с XML данными координат
     * @return объект Coordinates, созданный из XML
     * @throws InvalidDataException если данные не прошли валидацию
     */
    private Coordinates parseCoordinates(String xml) throws InvalidDataException { // приватный метод парсинга координат
        Coordinates coords = new Coordinates(); // создаем новый объект Coordinates

        String xStr = extractTag(xml, "x"); // извлекаем содержимое тега x
        if (xStr != null) { // если тег найден
            try { // блок try для парсинга числа
                Double x = Double.parseDouble(xStr); // преобразуем строку в Double
                if (x > 636) throw new InvalidDataException("X должен быть <= 636"); // проверяем ограничение
                coords.setX(x); // устанавливаем координату X
            } catch (NumberFormatException e) { // если не удалось преобразовать в число
                throw new InvalidDataException("Неверный формат X"); // выбрасываем исключение
            }
        }

        String yStr = extractTag(xml, "y"); // извлекаем содержимое тега y
        if (yStr != null) { // если тег найден
            try { // блок try для парсинга числа
                coords.setY(Integer.parseInt(yStr)); // преобразуем строку в int и устанавливаем координату Y
            } catch (NumberFormatException e) { // если не удалось преобразовать в число
                throw new InvalidDataException("Неверный формат Y"); // выбрасываем исключение
            }
        }
        return coords; // возвращаем созданный объект Coordinates
    }

    /**
     * Извлекает содержимое XML тега
     * 
     * @param xml строка с XML данными
     * @param tag имя тега для извлечения
     * @return содержимое тега или null, если тег не найден
     */
    private String extractTag(String xml, String tag) { // приватный метод для извлечения содержимого тега
        Pattern pattern = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL); // регулярное выражение для поиска тега
        Matcher matcher = pattern.matcher(xml); // создаем Matcher для поиска по XML
        return matcher.find() ? matcher.group(1).trim() : null; // если тег найден, возвращаем содержимое (обрезая пробелы), иначе null
    }

    /**
     * Экранирует специальные символы XML
     * 
     * @param text текст для экранирования
     * @return текст с замененными спецсимволами
     */
    private String escapeXml(String text) { // приватный метод для экранирования спецсимволов XML
        return text.replace("&", "&amp;") // заменяем & на &amp; (должно быть первым, чтобы не экранировать другие замены)
                .replace("<", "&lt;") // заменяем < на &lt;
                .replace(">", "&gt;") // заменяем > на &gt;
                .replace("\"", "&quot;") // заменяем " на &quot;
                .replace("'", "&apos;"); // заменяем ' на &apos;
    }
}