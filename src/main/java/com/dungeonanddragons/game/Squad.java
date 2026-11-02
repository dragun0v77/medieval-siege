package com.dungeonanddragons.game;

/** Отряд в армии героя: тип существ и сколько их. */
public final class Squad {
    public final UnitType type;
    public int count;

    public Squad(UnitType type, int count) {
        this.type = type;
        this.count = count;
    }
}
