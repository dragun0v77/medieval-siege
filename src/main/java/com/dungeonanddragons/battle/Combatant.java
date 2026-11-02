package com.dungeonanddragons.battle;

import com.dungeonanddragons.Character;
import com.dungeonanddragons.Monster;
import com.dungeonanddragons.Position;

/**
 * Участник боя. Это обёртка над героем или монстром: здоровье и характеристики
 * берутся прямо из них (поэтому раненый герой остаётся раненым и после боя),
 * а позиция на поле боя у бойца своя — карту подземелья она не затрагивает.
 */
public final class Combatant {

    public enum Side { HERO, ENEMY }

    private final Character hero;    // ровно одно из двух полей не null
    private final Monster monster;
    private final int speed;
    private Position position;

    private Combatant(Character hero, Monster monster, Position position, int speed) {
        this.hero = hero;
        this.monster = monster;
        this.position = position;
        this.speed = speed;
    }

    public static Combatant ofHero(Character hero, Position position, int speed) {
        return new Combatant(hero, null, position, speed);
    }

    public static Combatant ofMonster(Monster monster, Position position) {
        return new Combatant(null, monster, position, speedOf(monster.getType()));
    }

    /** Скорость = сколько клеток боец проходит за ход. Заодно задаёт порядок ходов. */
    static int speedOf(Monster.Type type) {
        return switch (type) {
            case GOBLIN -> 5;
            case SKELETON -> 4;
            case ORC -> 4;
            case TROLL -> 3;
            case DRAGON -> 6;
        };
    }

    public Side getSide()                 { return hero != null ? Side.HERO : Side.ENEMY; }
    public Monster.Type getMonsterType()  { return monster == null ? null : monster.getType(); }
    public String getName()               { return hero != null ? hero.getName() : monster.getName(); }
    public int getHealth()                { return hero != null ? hero.getHealth() : monster.getHealth(); }
    public int getMaxHealth()             { return hero != null ? hero.getMaxHealth() : monster.getMaxHealth(); }
    public int getAttack()                { return hero != null ? hero.getAttack() : monster.getAttack(); }
    public int getDefense()               { return hero != null ? hero.getDefense() : monster.getDefense(); }
    public boolean isAlive()              { return hero != null ? hero.isAlive() : monster.isAlive(); }
    public int getSpeed()                 { return speed; }
    public Position getPosition()         { return position; }

    void setPosition(Position position)   { this.position = position; }

    /**
     * Удар по цели. Урон фиксированный: атака бьющего минус защита цели, но не меньше 1
     * (формула уже была в Character.takeDamage / Monster.takeDamage — никакого рандома).
     * Возвращает, сколько здоровья цель реально потеряла.
     */
    int hit(Combatant target) {
        int before = target.getHealth();
        if (target.hero != null) {
            target.hero.takeDamage(getAttack());
        } else {
            target.monster.takeDamage(getAttack());
        }
        return before - target.getHealth();
    }
}
