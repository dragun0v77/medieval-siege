package com.dungeonanddragons.world;

/** Предмет мира: сундук, место благодати или житель деревни. */
public final class Prop {

    public enum Kind { CHEST, GRACE, ELDER, MERCHANT, HEALER }

    public enum Loot { GOLD, ATTACK, DEFENSE, FLASK, MAX_HP }

    private final Kind kind;
    private final String name;
    private final Loot loot;
    private final int amount;

    private Prop(Kind kind, String name, Loot loot, int amount) {
        this.kind = kind;
        this.name = name;
        this.loot = loot;
        this.amount = amount;
    }

    public static Prop chest(String name, Loot loot, int amount) { return new Prop(Kind.CHEST, name, loot, amount); }
    public static Prop of(Kind kind, String name)                { return new Prop(kind, name, null, 0); }

    public Kind getKind()   { return kind; }
    public String getName() { return name; }
    public Loot getLoot()   { return loot; }
    public int getAmount()  { return amount; }

    /** Жители деревни стоят на месте: в них нельзя войти, с ними разговаривают. */
    public boolean blocks() {
        return kind == Kind.ELDER || kind == Kind.MERCHANT || kind == Kind.HEALER;
    }
}
