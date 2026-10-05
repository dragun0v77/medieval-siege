package com.dungeonanddragons;

public class GameTest {

    private static int passed = 0; // cколько прошло успешно
    private static int failed = 0; // cколько провалилось

    public static void main(String[] args) {
        testCharacterCreation();
        testCharacterEmptyName();
        testCharacterTakeDamage();
        testCharacterLevelUp();
        testDungeonAddMonster();

        System.out.println();
        System.out.println("Пройдено: " + passed);
        System.out.println("Провалено: " + failed);
    }

    // Проверяем создание персонажа
    static void testCharacterCreation() {
        Character hero = new Character("Рыцарь", new Position(0, 0), 100, 15, 5);

        check("name", "Рыцарь", hero.getName());
        check("maxHealth", 100, hero.getMaxHealth());
        check("health", 100, hero.getHealth());
        check("attack", 15, hero.getAttack());
        check("isAlive", true, hero.isAlive());
    }

    // Проверяем, что пустое имя не принимается
    static void testCharacterEmptyName() {
        try {
            new Character("", new Position(0, 0), 100, 10, 5);
            check("пустое имя поймано", true, false);
        } catch (IllegalArgumentException e) {
            check("пустое имя поймано", true, true);
        }
    }

    // Проверяем расчёт урона с учётом защиты
    static void testCharacterTakeDamage() {
        Character hero = new Character("Тест", new Position(0, 0), 100, 10, 5);
        hero.takeDamage(20);   // 20 - 5 = 15 урона
        check("health после урона", 85, hero.getHealth());
    }

    // Проверяем повышение уровня
    static void testCharacterLevelUp() {
        Character hero = new Character("Тест", new Position(0, 0), 100, 10, 5);
        hero.gainExperience(100);
        check("level после 100 опыта", 2, hero.getLevel());
    }

    // Проверяем добавление монстра в подземелье
    static void testDungeonAddMonster() {
        Dungeon dungeon = new Dungeon("Тест", 5, 5);
        Monster goblin = new Monster("Гоблин", Monster.Type.GOBLIN,
                new Position(1, 1), 30, 8, 2, 10, 25);

        check("addMonster", true, dungeon.addMonster(goblin));
        check("aliveMonsterCount", 1, dungeon.getAliveMonsterCount());
    }

    // Вспомогательные методы для проверок

    static void check(String label, Object expected, Object actual) {
        if (expected == null && actual == null) {
            System.out.println("OK: " + label);
            passed++;
        } else if (expected != null && expected.equals(actual)) {
            System.out.println("OK: " + label);
            passed++;
        } else {
            System.out.println("ОШИБКА: " + label
                    + " (ожидалось: " + expected
                    + ", получено: " + actual + ")");
            failed++;
        }
    }

    static void check(String label, int expected, int actual) {
        check(label, (Object) expected, (Object) actual);
    }

    static void check(String label, boolean expected, boolean actual) {
        check(label, (Object) expected, (Object) actual);
    }
}