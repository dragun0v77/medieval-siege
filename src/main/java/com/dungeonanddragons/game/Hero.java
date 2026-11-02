package com.dungeonanddragons.game;

import com.dungeonanddragons.Position;

import java.util.ArrayList;
import java.util.List;

/** Герой: четыре характеристики, мана, очки движения на день, армия до 7 отрядов. */
public final class Hero {

    public static final int MAX_MOVE = 14;   // шагов за день
    public static final int MAX_ARMY = 7;

    private final String name;
    private int attack = 2, defense = 2, spellPower = 1, knowledge = 1;
    private int level = 1, exp, mana, movePoints = MAX_MOVE;
    private Position position;
    private final List<Squad> army = new ArrayList<>();

    public Hero(String name, Position position) {
        this.name = name;
        this.position = position;
        this.mana = maxMana();
    }

    public String getName()        { return name; }
    public int getAttack()         { return attack; }
    public int getDefense()        { return defense; }
    public int getSpellPower()     { return spellPower; }
    public int getKnowledge()      { return knowledge; }
    public int getLevel()          { return level; }
    public int getExp()            { return exp; }
    public int getMana()           { return mana; }
    public int getMovePoints()     { return movePoints; }
    public Position getPosition()  { return position; }
    public List<Squad> getArmy()   { return army; }

    public int maxMana()           { return knowledge * 10; }
    public int expToNext()         { return level * 400; }

    public void setPosition(Position p) { this.position = p; }
    public void setMana(int mana)       { this.mana = Math.max(0, Math.min(maxMana(), mana)); }
    public void spendMove()             { movePoints--; }

    /** Новый день: очки движения и мана восстанавливаются. */
    public void newDay() {
        movePoints = MAX_MOVE;
        mana = maxMana();
    }

    /** Добавить существ: к такому же отряду или в новый слот. false — нет места. */
    public boolean addUnits(UnitType type, int count) {
        for (Squad s : army) {
            if (s.type == type) { s.count += count; return true; }
        }
        if (army.size() >= MAX_ARMY) return false;
        army.add(new Squad(type, count));
        return true;
    }

    /** Опыт за бой. Возвращает сообщения о новых уровнях. */
    public List<String> gainExp(int amount) {
        List<String> messages = new ArrayList<>();
        exp += amount;
        while (exp >= expToNext()) {
            exp -= expToNext();
            level++;
            String skill;
            switch ((level - 2) % 6) {
                case 0, 2 -> { attack++; skill = "атака"; }
                case 1, 3 -> { defense++; skill = "защита"; }
                case 4 -> { spellPower++; skill = "сила магии"; }
                default -> { knowledge++; skill = "знания"; }
            }
            messages.add("Новый уровень: " + level + "! +1 " + skill + ".");
        }
        return messages;
    }
}
