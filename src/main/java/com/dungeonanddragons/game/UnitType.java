package com.dungeonanddragons.game;

/**
 * Типы существ. Числа взяты из «Героев 3» (урон усреднён — в игре он фиксированный).
 * Первые четыре нанимает игрок в городе, остальные стоят на карте как нейтральные отряды.
 */
public enum UnitType {
    //       название          атака защита урон  hp скор  стрелок выстр. цена рост
    MILITIA ("Ополченец",        4,    5,    2,   10,  4,  false,  0,    60,  14),
    ARCHER  ("Лучник",           6,    3,    3,   10,  4,  true,  12,   100,   9),
    KNIGHT  ("Рыцарь",          10,   12,    7,   35,  5,  false,  0,   300,   4),
    PALADIN ("Паладин",         16,   16,   22,  100,  9,  false,  0,  1200,   2),
    GOBLIN  ("Гоблин",           5,    4,    2,    5,  5,  false,  0,    40,   0),
    WOLF    ("Волк",             7,    5,    3,   10,  8,  false,  0,   100,   0),
    SKELETON("Скелет",           5,    4,    2,    6,  4,  false,  0,    60,   0),
    ORC     ("Орк",              8,    4,    4,   15,  5,  false,  0,   150,   0),
    TROLL   ("Тролль",          14,    7,   12,   40,  7,  false,  0,   500,   0),
    DRAGON  ("Красный дракон",  25,   19,   45,  180, 16,  false,  0,  3500,   0);

    public final String title;
    public final int attack, defense, damage, hp, speed, shots, gold, growth;
    public final boolean ranged;

    UnitType(String title, int attack, int defense, int damage, int hp, int speed,
             boolean ranged, int shots, int gold, int growth) {
        this.title = title;
        this.attack = attack;
        this.defense = defense;
        this.damage = damage;
        this.hp = hp;
        this.speed = speed;
        this.ranged = ranged;
        this.shots = shots;
        this.gold = gold;
        this.growth = growth;
    }
}
