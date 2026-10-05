package com.dungeonanddragons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Dungeon {

    // Тип клетки — стена или пол
    public enum CellType {
        FLOOR,  // пол — можно ходить
        WALL    // стена — нельзя ходить
    }

    private String name;
    private int width;
    private int height;
    private CellType[][] grid;          // сетка клеток [x][y]
    private List<Monster> monsters;     // список всех монстров
    private List<Item> items;           // список всех предметов
    private Position entrance;          // точка входа героя
    private Position exit;              // выход из подземелья
    private boolean initialized = false; // взводится при первом реальном использовании карты

    public Dungeon(String name, int width, int height) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя подземелья не может быть пустым");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Размеры должны быть > 0");
        }

        this.name = name;
        this.width = width;
        this.height = height;
        this.grid = new CellType[width][height];
        this.monsters = new ArrayList<>();
        this.items = new ArrayList<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = CellType.FLOOR;
            }
        }

        this.entrance = new Position(0, 0);
        this.exit = new Position(width - 1, height - 1);
    }

    // ----- Геттеры -----

    public String getName()   { return name; }
    public int getWidth()     { return width; }
    public int getHeight()    { return height; }
    public Position getEntrance() { return entrance; }
    public Position getExit()     { return exit; }

    /** Возвращает неизменяемый список монстров — защита инкапсуляции. */
    public List<Monster> getMonsters() {
        return Collections.unmodifiableList(monsters);
    }

    /** Возвращает неизменяемый список предметов — защита инкапсуляции. */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    // ----- Сеттеры -----

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя подземелья не может быть пустым");
        }
        this.name = name;
    }

    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("width должен быть > 0");
        }
        if (initialized) {
            throw new IllegalStateException("Нельзя менять размеры после инициализации карты");
        }
        this.width = width;
        resizeGrid();
    }

    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("height должен быть > 0");
        }
        if (initialized) {
            throw new IllegalStateException("Нельзя менять размеры после инициализации карты");
        }
        this.height = height;
        resizeGrid();
    }

    public void setEntrance(Position entrance) {
        if (entrance == null) {
            throw new IllegalArgumentException("Точка входа не может быть null");
        }
        if (!isValidPosition(entrance)) {
            throw new IllegalArgumentException("Точка входа вне карты");
        }
        this.entrance = entrance;
    }

    public void setExit(Position exit) {
        if (exit == null) {
            throw new IllegalArgumentException("Выход не может быть null");
        }
        if (!isValidPosition(exit)) {
            throw new IllegalArgumentException("Выход вне карты");
        }
        this.exit = exit;
    }

    // Пересоздаёт сетку при изменении размеров (всё становится полом)
    private void resizeGrid() {
        grid = new CellType[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = CellType.FLOOR;
            }
        }
    }

    // ----- Работа с клетками -----

    /** Проверка, что позиция внутри карты. */
    public boolean isValidPosition(Position position) {
        if (position == null) return false;
        return position.getX() >= 0 && position.getX() < width &&
                position.getY() >= 0 && position.getY() < height;
    }

    /** Получить тип клетки. */
    public CellType getCellType(Position position) {
        if (!isValidPosition(position)) return null;
        return grid[position.getX()][position.getY()];
    }

    /** Установить тип клетки (например, сделать стену). */
    public boolean setCellType(Position position, CellType type) {
        if (!isValidPosition(position) || type == null) return false;
        grid[position.getX()][position.getY()] = type;
        initialized = true;
        return true;
    }

    /** Проходима ли клетка? (не стена и внутри карты) */
    public boolean isWalkable(Position position) {
        return isValidPosition(position) && getCellType(position) == CellType.FLOOR;
    }

    // ----- Размещение объектов -----

    /** Поставить монстра. Нельзя на стену или на другого монстра. */
    public boolean addMonster(Monster monster) {
        if (monster == null) return false;
        if (!isWalkable(monster.getPosition())) return false;
        if (getMonsterAt(monster.getPosition()) != null) return false;

        monsters.add(monster);
        initialized = true;
        return true;
    }

    /** Положить предмет. Нельзя на стену или другой предмет. */
    public boolean addItem(Item item) {
        if (item == null) return false;
        if (!isWalkable(item.getPosition())) return false;
        if (getItemAt(item.getPosition()) != null) return false;

        items.add(item);
        initialized = true;
        return true;
    }

    // ----- Поиск объектов -----

    /** Найти монстра на позиции (только живого). */
    public Monster getMonsterAt(Position position) {
        for (Monster m : monsters) {
            if (m.isAlive() && m.getPosition().equals(position)) return m;
        }
        return null;
    }

    /** Найти предмет на позиции (только неподобранный). */
    public Item getItemAt(Position position) {
        for (Item i : items) {
            if (!i.isCollected() && i.getPosition().equals(position)) return i;
        }
        return null;
    }

    /** Удалить мёртвых монстров из списка. */
    public int removeDeadMonsters() {
        int before = monsters.size();
        monsters.removeIf(m -> !m.isAlive());
        return before - monsters.size();
    }

    /** Удалить подобранные предметы из списка. */
    public int removeCollectedItems() {
        int before = items.size();
        items.removeIf(Item::isCollected);
        return before - items.size();
    }

    // ----- Вспомогательное -----

    /** Сколько живых монстров осталось. */
    public int getAliveMonsterCount() {
        int count = 0;
        for (Monster m : monsters) {
            if (m.isAlive()) count++;
        }
        return count;
    }

    /** Очистить подземелье от всех монстров и предметов. */
    public void clear() {
        monsters.clear();
        items.clear();
    }

    @Override
    public String toString() {
        return "Подземелье \"" + name + "\" " + width + "x" + height +
                " | монстров: " + getAliveMonsterCount() +
                ", предметов: " + items.size() +
                " | вход " + entrance + ", выход " + exit;
    }

    public void printMap(Character hero) {
        System.out.println("=== " + name + " ===");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Position p = new Position(x, y);
                char symbol;

                if (grid[x][y] == CellType.WALL) {
                    symbol = '#';
                } else if (hero != null && hero.getPosition().equals(p)) {
                    symbol = 'H';
                } else if (getMonsterAt(p) != null) {
                    symbol = 'M';
                } else if (getItemAt(p) != null) {
                    symbol = 'i';
                } else if (p.equals(entrance)) {
                    symbol = 'E';
                } else if (p.equals(exit)) {
                    symbol = 'X';
                } else {
                    symbol = '.';
                }

                System.out.print(symbol + " ");
            }
            System.out.println();
        }
        System.out.println("Легенда: H-герой, M-монстр, i-предмет, E-вход, X-выход, #-стена");
    }
}