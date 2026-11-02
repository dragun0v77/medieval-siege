package com.dungeonanddragons;

/**
 * Создаёт стартовый уровень "Логово дракона".
 * Данные такие же, как в Game.initializeGame() (консольная версия), только вынесены отдельно,
 * чтобы их мог использовать и графический интерфейс.
 */
public final class LevelFactory {

    private LevelFactory() { }

    public static Dungeon createDungeon() {
        Dungeon dungeon = new Dungeon("Логово дракона", 8, 8);

        // Стены
        for (int y = 1; y < 7; y++) {
            dungeon.setCellType(new Position(3, y), Dungeon.CellType.WALL);
        }
        dungeon.setCellType(new Position(3, 4), Dungeon.CellType.FLOOR); // проход в стене

        dungeon.setCellType(new Position(5, 2), Dungeon.CellType.WALL);
        dungeon.setCellType(new Position(5, 3), Dungeon.CellType.WALL);
        dungeon.setCellType(new Position(5, 4), Dungeon.CellType.WALL);
        dungeon.setCellType(new Position(5, 5), Dungeon.CellType.WALL);

        dungeon.setEntrance(new Position(0, 0));
        dungeon.setExit(new Position(7, 7));

        // Монстры
        dungeon.addMonster(new Monster("Гоблин", Monster.Type.GOBLIN,
                new Position(2, 2), 30, 8, 2, 10, 25));
        dungeon.addMonster(new Monster("Скелет", Monster.Type.SKELETON,
                new Position(4, 1), 40, 10, 3, 15, 35));
        dungeon.addMonster(new Monster("Орк", Monster.Type.ORC,
                new Position(6, 4), 60, 14, 5, 25, 50));
        dungeon.addMonster(new Monster("Тролль", Monster.Type.TROLL,
                new Position(1, 6), 80, 18, 7, 40, 80));
        dungeon.addMonster(new Monster("Дракон", Monster.Type.DRAGON,
                new Position(7, 7), 150, 25, 10, 100, 200));

        // Предметы
        dungeon.addItem(new Item("Стальной меч", new Position(1, 1), 50, 5, 0, 0));
        dungeon.addItem(new Item("Кожаный доспех", new Position(2, 5), 40, 0, 3, 0));
        dungeon.addItem(new Item("Зелье лечения", new Position(4, 6), 20, 0, 0, 40));
        dungeon.addItem(new Item("Большое зелье", new Position(6, 1), 40, 0, 0, 70));
        dungeon.addItem(new Item("Сундук с золотом", new Position(0, 7), 100, 0, 0, 0));

        return dungeon;
    }

    /** Герой стартует на входе в подземелье. */
    public static Character createHero(Dungeon dungeon) {
        Position entrance = dungeon.getEntrance();
        // копия позиции, чтобы герой не делил объект Position с точкой входа
        return new Character("Рыцарь", new Position(entrance.getX(), entrance.getY()), 100, 25, 5);
    }
}
