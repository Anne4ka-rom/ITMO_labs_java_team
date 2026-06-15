package client;

import common.model.*;

import java.io.*;
import java.util.Scanner;

/**
 * Отвечает за интерактивный ввод данных транспортного средства.
 * Читает поля из консоли или из текущего скрипта (через scriptReader).
 *
 * @author Polina
 * @version 1.1
 */
public class VehicleReader {

    private final Scanner scanner; // сканер для чтения из консоли

    // Для чтения полей из скрипта (package-private — читаются напрямую из ScriptExecutor)
    BufferedReader scriptReader = null; // ридер для текущего скрипта
    boolean isExecutingScript = false; // флаг выполнения скрипта

    public VehicleReader(Scanner scanner) {
        this.scanner = scanner;
    }

    // установить текущий ридер скрипта и флаг (вызывается из ScriptExecutor)
    public void setScriptContext(BufferedReader reader, boolean executing) {
        this.scriptReader = reader;
        this.isExecutingScript = executing;
    }

    /**
     * Читает строку из правильного источника (консоль или файл скрипта)
     */
    public String readLine() {
        if (isExecutingScript && scriptReader != null) {
            try {
                String line = scriptReader.readLine();
                if (line != null) {
                    line = line.trim();
                    System.out.println("  [из скрипта] > " + line);
                    return line;
                }
            } catch (IOException e) {
                System.out.println("Ошибка чтения из скрипта: " + e.getMessage());
            }
        }
        // Читаем из консоли
        return scanner.nextLine().trim();
    }

    // читает данные транспортного средства с консоли или из скрипта
    public Vehicle readVehicle() {
        Vehicle vehicle = new Vehicle();

        if (!isExecutingScript) {
            System.out.println("Введите данные транспортного средства:");
        }

        // ввод имени
        while (true) {
            System.out.print("  name (не пустое): ");
            String input = readLine();
            try {
                InputValidator.validateName(input);
                vehicle.setName(input);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод координаты x
        while (true) {
            System.out.print("  coordinate x (Double, <= 636): ");
            String input = readLine();
            try {
                Double x = input.isEmpty() ? null : Double.parseDouble(input);
                InputValidator.validateCoordinateX(x);

                Coordinates coords = vehicle.getCoordinates();
                if (coords == null) coords = new Coordinates();
                coords.setX(x);
                vehicle.setCoordinates(coords);
                break;
            } catch (NumberFormatException e) {
                System.out.println("  Ошибка: введите число");
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод координаты y
        while (true) {
            System.out.print("  coordinate y (int): ");
            try {
                String input = readLine();
                int y = Integer.parseInt(input);
                vehicle.getCoordinates().setY(y);
                break;
            } catch (NumberFormatException e) {
                System.out.print("  Ошибка: введите целое число: ");
            }
        }

        // ввод мощности двигателя
        while (true) {
            System.out.print("  enginePower (Double > 0): ");
            String input = readLine();
            try {
                Double power = Double.parseDouble(input);
                InputValidator.validateEnginePower(power);
                vehicle.setEnginePower(power);
                break;
            } catch (NumberFormatException e) {
                System.out.println("  Ошибка: введите число");
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод вместимости
        while (true) {
            System.out.print("  capacity (double > 0): ");
            String input = readLine();
            try {
                double capacity = Double.parseDouble(input);
                InputValidator.validateCapacity(capacity);
                vehicle.setCapacity(capacity);
                break;
            } catch (NumberFormatException e) {
                System.out.println("  Ошибка: введите число");
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод типа транспорта
        while (true) {
            System.out.print("  type (" + VehicleType.getTypes() + "): ");
            String input = readLine().toUpperCase();
            try {
                VehicleType type = InputValidator.validateAndParseVehicleType(input);
                vehicle.setType(type);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод типа топлива (может быть null)
        System.out.print("  fuelType (" + FuelType.getTypes() + ", или пустая строка для null): ");
        String fuelInput = readLine();
        if (!fuelInput.isEmpty()) {
            try {
                vehicle.setFuelType(FuelType.valueOf(fuelInput.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.out.println("  Неверный тип топлива, будет установлен null");
                vehicle.setFuelType(null);
            }
        } else {
            vehicle.setFuelType(null);
        }

        return vehicle;
    }
}