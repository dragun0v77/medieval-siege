package com.dungeonanddragons;

public class Item {
    private String name;
    private Position position;
    private int value;         // ценность в золоте
    private int attackBonus;   // + к атаке
    private int defenseBonus;  // + к защите
    private int healAmount;    // + к HP
    private boolean collected;

    public Item(String name, Position position, int value,
                int attackBonus, int defenseBonus, int healAmount) {
        this.name = name;
        this.position = position;
        this.value = Math.max(0, value);
        this.attackBonus = Math.max(0, attackBonus);
        this.defenseBonus = Math.max(0, defenseBonus);
        this.healAmount = Math.max(0, healAmount);
        this.collected = false;
    }

    public String getName()       { return name; }
    public Position getPosition() { return position; }
    public int getValue()         { return value; }
    public int getAttackBonus()   { return attackBonus; }
    public int getDefenseBonus()  { return defenseBonus; }
    public int getHealAmount()    { return healAmount; }
    public boolean isCollected()  { return collected; }

    public void setName(String name)           { this.name = name; }
    public void setPosition(Position position) { this.position = position; }
    public void setValue(int value)            { this.value = Math.max(0, value); }
    public void setAttackBonus(int v)          { this.attackBonus = Math.max(0, v); }
    public void setDefenseBonus(int v)         { this.defenseBonus = Math.max(0, v); }
    public void setHealAmount(int v)           { this.healAmount = Math.max(0, v); }

    public boolean isWeapon()   { return attackBonus > 0 && healAmount == 0; }
    public boolean isArmor()    { return defenseBonus > 0 && healAmount == 0; }
    public boolean isPotion()   { return healAmount > 0; }
    public boolean isTreasure() { return attackBonus == 0 && defenseBonus == 0 && healAmount == 0; } // золото

    public void collect() {
        this.collected = true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (attackBonus > 0)  sb.append(" +").append(attackBonus).append(" АТК");
        if (defenseBonus > 0) sb.append(" +").append(defenseBonus).append(" ЗАЩ");
        if (healAmount > 0)   sb.append(" +").append(healAmount).append(" HP");
        if (isTreasure())     sb.append(" (").append(value).append(" зол.)");
        sb.append(" ").append(position);
        return sb.toString();
    }
}