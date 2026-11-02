package com.dungeonanddragons;

import java.util.function.Consumer;

/**
 * Состояние партии на карте подземелья и правила "вне боя": шаги героя,
 * подбор предметов, награда за победу, условия победы и поражения.
 * Здесь нет ничего про окна — поэтому это легко проверять тестами.
 */
public class GameSession {

    private final Dungeon dungeon;
    private final Character hero;
    private int steps;
    private Consumer<String> logger = message -> { };

    public GameSession() {
        this.dungeon = LevelFactory.createDungeon();
        this.hero = LevelFactory.createHero(dungeon);
    }

    public GameSession(Dungeon dungeon, Character hero) {
        if (dungeon == null || hero == null) {
            throw new IllegalArgumentException("dungeon и hero не могут быть null");
        }
        this.dungeon = dungeon;
        this.hero = hero;
    }

    public Dungeon getDungeon() { return dungeon; }
    public Character getHero()  { return hero; }
    public int getSteps()       { return steps; }

    /** Куда выводить игровые сообщения (в интерфейсе это журнал событий). */
    public void setLogger(Consumer<String> logger) {
        this.logger = logger == null ? message -> { } : logger;
    }

    private void log(String message) {
        logger.accept(message);
    }

    /** Можно ли ступить в клетку: внутри карты, не стена и нет живого монстра. */
    public boolean canStepTo(Position p) {
        return dungeon.isWalkable(p) && dungeon.getMonsterAt(p) == null;
    }

    /**
     * Шаг героя на соседнюю клетку. Бой здесь не начинается — если на клетке монстр,
     * шаг не делается (вызывающий код сам решает, что делать).
     * Предметы подбираются автоматически.
     */
    public boolean moveHero(Position to) {
        if (to == null || !hero.isAlive() || !canStepTo(to)) return false;

        Position from = hero.getPosition();
        if (!Pathfinder.isAdjacent(from, to)) return false;

        hero.setPosition(to);
        steps++;

        Item item = dungeon.getItemAt(to);
        if (item != null) {
            pickUp(item);
        }

        if (to.equals(dungeon.getExit()) && dungeon.getAliveMonsterCount() > 0) {
            log("Выход закрыт! Осталось монстров: " + dungeon.getAliveMonsterCount());
        }
        return true;
    }

    private void pickUp(Item item) {
        // Чистое зелье при полном здоровье не тратим — оно остаётся лежать до лучших времён
        boolean onlyPotion = item.isPotion() && item.getAttackBonus() == 0 && item.getDefenseBonus() == 0;
        if (onlyPotion && hero.getHealth() >= hero.getMaxHealth()) {
            log("Здесь лежит «" + item.getName() + "», но здоровье полное — зелье оставлено.");
            return;
        }

        log("Подобрано: " + item.getName());
        if (item.getAttackBonus() > 0) {
            hero.setAttack(hero.getAttack() + item.getAttackBonus());
            log("  +" + item.getAttackBonus() + " к атаке");
        }
        if (item.getDefenseBonus() > 0) {
            hero.setDefense(hero.getDefense() + item.getDefenseBonus());
            log("  +" + item.getDefenseBonus() + " к защите");
        }
        if (item.getHealAmount() > 0) {
            int before = hero.getHealth();
            hero.heal(item.getHealAmount());
            log("  +" + (hero.getHealth() - before) + " HP");
        }
        if (item.isTreasure()) {
            hero.addGold(item.getValue());
            log("  +" + item.getValue() + " золота");
        }
        item.collect();
        dungeon.removeCollectedItems();
    }

    /** Награда за убитого в бою монстра. */
    public void rewardForVictory(Monster monster) {
        int levelBefore = hero.getLevel();
        hero.addGold(monster.getGoldReward());
        hero.gainExperience(monster.getExpReward());

        log(monster.getName() + " повержен! Получено " + monster.getGoldReward()
                + " золота и " + monster.getExpReward() + " опыта.");
        if (hero.getLevel() > levelBefore) {
            log("Новый уровень: " + hero.getLevel() + "! Здоровье восстановлено, характеристики выросли.");
        }

        dungeon.removeDeadMonsters();
        if (dungeon.getAliveMonsterCount() == 0) {
            log("Все монстры повержены — выход открыт!");
        }
    }

    /** Победа: монстров не осталось и герой стоит на выходе. */
    public boolean isVictory() {
        return hero.isAlive()
                && dungeon.getAliveMonsterCount() == 0
                && hero.getPosition().equals(dungeon.getExit());
    }

    public boolean isDefeat() {
        return !hero.isAlive();
    }
}
