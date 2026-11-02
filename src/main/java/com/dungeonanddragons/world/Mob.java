package com.dungeonanddragons.world;

import com.dungeonanddragons.Position;

/** Враг в открытом мире. Урон фиксированный: атака минус защита цели, минимум 1. */
public final class Mob {

    public enum Kind { WOLF, BANDIT, GOBLIN, SKELETON, ORC, TROLL, DRAGON }

    private final Kind kind;
    private final String name;
    private final int maxHp, attack, defense, speed, aggro, gold, exp;
    private final boolean boss;
    private final Position spawn;
    private Position pos;
    private int hp;
    private boolean engaged;

    private Mob(Kind kind, String name, int hp, int attack, int defense, int speed, int aggro,
                int gold, int exp, boolean boss, int x, int y) {
        this.kind = kind;
        this.name = name;
        this.maxHp = hp;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.aggro = aggro;
        this.gold = gold;
        this.exp = exp;
        this.boss = boss;
        this.spawn = new Position(x, y);
        this.pos = new Position(x, y);
    }

    public static Mob of(Kind k, int x, int y) {
        return switch (k) {
            case WOLF     -> new Mob(k, "Волк",     25, 9,  1, 5, 5, 8,   12,  false, x, y);
            case BANDIT   -> new Mob(k, "Бандит",   35, 11, 3, 4, 5, 25,  25,  false, x, y);
            case GOBLIN   -> new Mob(k, "Гоблин",   30, 8,  2, 5, 4, 10,  25,  false, x, y);
            case SKELETON -> new Mob(k, "Скелет",   40, 10, 3, 4, 4, 15,  35,  false, x, y);
            case ORC      -> new Mob(k, "Орк",      60, 14, 5, 4, 4, 25,  50,  false, x, y);
            case TROLL    -> new Mob(k, "Тролль",   80, 18, 7, 3, 4, 40,  80,  false, x, y);
            case DRAGON   -> new Mob(k, "Дракон",  150, 25, 10, 6, 6, 100, 200, true,  x, y);
        };
    }

    /** Получить удар. Возвращает, сколько здоровья потеряно. */
    int takeDamage(int attackValue) {
        int d = Math.max(1, attackValue - defense);
        hp = Math.max(0, hp - d);
        return d;
    }

    /** Вернуть на место и вылечить (враги возрождаются, когда герой отдыхает). */
    void reset() {
        hp = maxHp;
        pos = new Position(spawn.getX(), spawn.getY());
        engaged = false;
    }

    public Kind getKind()        { return kind; }
    public String getName()      { return name; }
    public int getHp()           { return hp; }
    public int getMaxHp()        { return maxHp; }
    public int getAttack()       { return attack; }
    public int getSpeed()        { return speed; }
    public int getAggro()        { return aggro; }
    public int getGold()         { return gold; }
    public int getExp()          { return exp; }
    public boolean isBoss()      { return boss; }
    public boolean isAlive()     { return hp > 0; }
    public boolean isEngaged()   { return engaged; }
    public Position getPosition() { return pos; }

    void setPosition(Position p) { this.pos = p; }
    void setEngaged(boolean e)   { this.engaged = e; }
}
