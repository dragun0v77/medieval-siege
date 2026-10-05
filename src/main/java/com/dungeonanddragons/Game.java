package com.dungeonanddragons;

import java.util.Scanner;

/**
 * Главный класс игры "Подземелье и драконы".
 * Управляет игровым циклом: инициализация, ход игрока, ход монстров,
 * проверка условий победы/поражения.
 */
public class Game {

    private Dungeon dungeon;
    private Character hero;
    private Scanner scanner;
    private int turnCount;
    private boolean gameOver;
    private boolean victory;

    public Game() {
        this.scanner = new Scanner(System.in);
        this.turnCount = 0;
        this.gameOver = false;
        this.victory = false;
        initializeGame();
    }

    // ===== Инициализация =====

    private void initializeGame() {
        // Создаём подземелье 8x8
        dungeon = new Dungeon("Логово дракона", 8, 8);

        // Строим стены
        for (int y = 1; y < 7; y++) {
            dungeon.setCellType(new Position(3, y), Dungeon.CellType.WALL);
        }
        dungeon.setCellType(new Position(3, 4), Dungeon.CellType.FLOOR);

        dungeon.setCellType(new Position(5, 2), Dungeon.CellType.WALL);
        dungeon.setCellType(new Position(5, 3), Dungeon.CellType.WALL);
        dungeon.setCellType(new Position(5, 4), Dungeon.CellType.WALL);
        dungeon.setCellType(new Position(5, 5), Dungeon.CellType.WALL);

        // точки входа и выхода
        dungeon.setEntrance(new Position(0, 0));
        dungeon.setExit(new Position(7, 7));

        // 4. Герой — стартует на входе
        hero = new Character("Рыцарь", dungeon.getEntrance(), 100, 25, 5);

        // 5. Монстры
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
    }

    // ===== Игровой цикл =====

    public void start() {
        System.out.println("*** ПОДЗЕМЕЛЬЕ И ДРАКОНЫ ***");
        System.out.println("Вы — " + hero.getName() + ".");
        System.out.println("Цель: убить всех монстров и дойти до выхода (X).");
        System.out.println();

        while (!gameOver && !victory) {
            turnCount++;
            displayGameState();
            playerTurn();

            if (!hero.isAlive()) {
                gameOver = true;
                System.out.println("\nВы погибли... Игра окончена.");
                break;
            }

            if (hero.getPosition().equals(dungeon.getExit())
                    && dungeon.getAliveMonsterCount() == 0) {
                victory = true;
                System.out.println("\nПОБЕДА! Вы очистили подземелье и нашли выход!");
                break;
            }

            monstersTurn();
            dungeon.removeDeadMonsters();
            dungeon.removeCollectedItems();
        }

        printResults();
    }

    // ===== Отображение состояния =====

    private void displayGameState() {
        System.out.println("\n─── Ход " + turnCount + " ───");
        dungeon.printMap(hero);
        System.out.println(hero);
        System.out.println("Монстров осталось: " + dungeon.getAliveMonsterCount());
        System.out.println();
    }

    // ===== Ход игрока =====

    private void playerTurn() {
        System.out.println("Действия: [w]верх [s]вниз [a]влево [d]вправо");
        System.out.println("          [e]атаковать [q]подобрать [i]инфо [x]выход");
        System.out.print("Ваш выбор: ");

        String input = scanner.nextLine().trim().toLowerCase();
        if (input.isEmpty()) return;

        char action = input.charAt(0);
        Position cur = hero.getPosition();

        switch (action) {
            case 'w': tryMove(cur.getX(), cur.getY() - 1); break;
            case 's': tryMove(cur.getX(), cur.getY() + 1); break;
            case 'a': tryMove(cur.getX() - 1, cur.getY()); break;
            case 'd': tryMove(cur.getX() + 1, cur.getY()); break;
            case 'e': attackNearby(); break;
            case 'q': pickUpItem(); break;
            case 'i': System.out.println(hero); break;
            case 'x':
                gameOver = true;
                System.out.println("Вы покинули подземелье.");
                break;
            default:
                System.out.println("Неизвестное действие.");
        }
    }

    private void tryMove(int nx, int ny) {
        Position np = new Position(nx, ny);

        if (!dungeon.isValidPosition(np)) {
            System.out.println("За границей карты!");
            return;
        }
        if (!dungeon.isWalkable(np)) {
            System.out.println("Там стена!");
            return;
        }
        if (dungeon.getMonsterAt(np) != null) {
            System.out.println("Там монстр! Сначала атакуйте его.");
            return;
        }

        hero.setPosition(np);
        System.out.println("Вы переместились в " + np);

        if (np.equals(dungeon.getExit())) {
            if (dungeon.getAliveMonsterCount() == 0) {
                victory = true;
                System.out.println("ВЫ НАШЛИ ВЫХОД! ПОБЕДА!");
            } else {
                System.out.println("Выход закрыт! Осталось монстров: "
                        + dungeon.getAliveMonsterCount());
            }
        }
    }

    private void attackNearby() {
        Position hp = hero.getPosition();
        int[][] dirs = {{0,0},{0,-1},{0,1},{-1,0},{1,0}};

        for (int[] d : dirs) {
            Position p = new Position(hp.getX() + d[0], hp.getY() + d[1]);
            Monster m = dungeon.getMonsterAt(p);
            if (m != null) {
                System.out.println("Вы атакуете " + m.getName() + "!");
                hero.attack(m);

                if (!m.isAlive()) {
                    System.out.println(m.getName() + " повержен!");
                    hero.addGold(m.getGoldReward());
                    hero.gainExperience(m.getExpReward());
                    System.out.println("Получено " + m.getGoldReward()
                            + " золота и " + m.getExpReward() + " опыта.");
                } else {
                    System.out.println(m.getName() + ": " + m.getHealth()
                            + "/" + m.getMaxHealth() + " HP");
                }
                return;
            }
        }
        System.out.println("Рядом нет монстров.");
    }

    private void pickUpItem() {
        Item item = dungeon.getItemAt(hero.getPosition());
        if (item == null) {
            System.out.println("Здесь нет предметов.");
            return;
        }

        System.out.println("Вы подобрали: " + item);

        if (item.getAttackBonus() > 0) {
            hero.setAttack(hero.getAttack() + item.getAttackBonus());
            System.out.println("  +" + item.getAttackBonus() + " к атаке");
        }
        if (item.getDefenseBonus() > 0) {
            hero.setDefense(hero.getDefense() + item.getDefenseBonus());
            System.out.println("  +" + item.getDefenseBonus() + " к защите");
        }
        if (item.getHealAmount() > 0) {
            hero.heal(item.getHealAmount());
            System.out.println("  +" + item.getHealAmount() + " HP");
        }
        if (item.isTreasure()) {
            hero.addGold(item.getValue());
            System.out.println("  +" + item.getValue() + " золота");
        }
        item.collect();
    }

    // ===== Ход монстров =====

    private void monstersTurn() {
        for (Monster m : dungeon.getMonsters()) {
            if (!m.isAlive()) continue;

            double dist = m.getPosition().distanceTo(hero.getPosition());
            if (dist <= 1.0) {
                System.out.println(m.getName() + " атакует вас!");
                m.attack(hero);
                System.out.println("Ваше HP: " + hero.getHealth()
                        + "/" + hero.getMaxHealth());
                if (!hero.isAlive()) return;
            } else {
                moveMonsterTowardsHero(m);
            }
        }
    }

    private void moveMonsterTowardsHero(Monster m) {
        Position mp = m.getPosition();
        Position hp = hero.getPosition();
        int dx = Integer.compare(hp.getX(), mp.getX());
        int dy = Integer.compare(hp.getY(), mp.getY());

        Position[] tries = {
                new Position(mp.getX() + dx, mp.getY()),
                new Position(mp.getX(), mp.getY() + dy)
        };
        for (Position p : tries) {
            if (dungeon.isWalkable(p)
                    && dungeon.getMonsterAt(p) == null
                    && !p.equals(hero.getPosition())) {
                m.setPosition(p);
                return;
            }
        }
    }

    // ===== Итоги =====

    private void printResults() {
        System.out.println();
        System.out.println("*** ИТОГИ ***");
        System.out.println("Ходов: " + turnCount);
        System.out.println(hero);

        if (victory) {
            System.out.println("Результат: ПОБЕДА");
        } else {
            System.out.println("Результат: ПОРАЖЕНИЕ");
        }
    }

    // ===== Точка входа =====

    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}