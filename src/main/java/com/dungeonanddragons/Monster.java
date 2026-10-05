package com.dungeonanddragons;

public class Monster {
    public enum Type {
        GOBLIN,
        SKELETON,
        ORC,
        TROLL,
        DRAGON
    }

    private String name;
    private Type type;
    private Position position;
    private int health;
    private int maxHealth;
    private int attack;      // сила атаки
    private int defense;     // защита
    private int goldReward;  // золото за убийство
    private int expReward;   // опыт за убийство
    private boolean alive;

    public Monster(String name, Type type, Position position,
                   int health, int attack, int defense,
                   int goldReward, int expReward) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (type == null) {
            throw new IllegalArgumentException("Тип не может быть null");
        }
        if (position == null) {
            throw new IllegalArgumentException("Позиция не может быть null");
        }
        if (health <= 0) {
            throw new IllegalArgumentException("health должен быть > 0");
        }
        if (attack < 0) {
            throw new IllegalArgumentException("attack не может быть отрицательным");
        }
        if (defense < 0) {
            throw new IllegalArgumentException("defense не может быть отрицательным");
        }
        if (goldReward < 0) {
            throw new IllegalArgumentException("goldReward не может быть отрицательным");
        }
        if (expReward < 0) {
            throw new IllegalArgumentException("expReward не может быть отрицательным");
        }

        this.name = name;
        this.type = type;
        this.position = position;
        this.maxHealth = health;
        this.health = health;
        this.attack = attack;
        this.defense = defense;
        this.goldReward = goldReward;
        this.expReward = expReward;
        this.alive = true;
    }

    public String getName()        { return name; }
    public Type getType()          { return type; }
    public Position getPosition()  { return position; }
    public int getHealth()         { return health; }
    public int getMaxHealth()      { return maxHealth; }
    public int getAttack()         { return attack; }
    public int getDefense()        { return defense; }
    public int getGoldReward()     { return goldReward; }
    public int getExpReward()      { return expReward; }
    public boolean isAlive()       { return alive; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        this.name = name;
    }

    public void setType(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Тип не может быть null");
        }
        this.type = type;
    }

    public void setPosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Позиция не может быть null");
        }
        this.position = position;
    }

    public void setHealth(int health) {
        if (health < 0) health = 0;
        if (health > maxHealth) health = maxHealth;
        this.health = health;
        this.alive = health > 0;
    }

    public void setMaxHealth(int maxHealth) {
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("maxHealth должен быть > 0");
        }
        this.maxHealth = maxHealth;
        if (this.health > maxHealth) this.health = maxHealth;
    }

    public void setAttack(int attack) {
        this.attack = Math.max(0, attack);
    }

    public void setDefense(int defense) {
        this.defense = Math.max(0, defense);
    }

    public void setGoldReward(int goldReward) {
        this.goldReward = Math.max(0, goldReward);
    }

    public void setExpReward(int expReward) {
        this.expReward = Math.max(0, expReward);
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    // Получить урон с учётом защиты
    public void takeDamage(int damage) {
        if (damage <= 0) return;
        int actual = Math.max(1, damage - defense);
        health -= actual;
        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    // Атаковать персонажа
    public void attack(Character target) {
        if (!alive || target == null || !target.isAlive()) return;
        target.takeDamage(this.attack);
    }

    // Лечение
    public void heal(int amount) {
        if (amount <= 0) return;
        health = Math.min(maxHealth, health + amount);
        if (health > 0) alive = true;
    }

    // Перемещение по подземелью
    public boolean moveTo(Position newPosition, Dungeon dungeon, int moveRange) {
        if (!alive) return false;
        if (newPosition == null || dungeon == null) return false;
        if (!dungeon.isValidPosition(newPosition)) return false;

        double dist = position.distanceTo(newPosition);
        if (dist > moveRange) return false;

        this.position = newPosition;
        return true;
    }

    // Процент здоровья
    public double getHealthPercentage() {
        if (maxHealth == 0) return 0;
        return (double) health / maxHealth * 100.0;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + name +
                " [" + health + "/" + maxHealth + " HP, " +
                "АТК " + attack + ", ЗАЩ " + defense + ", " +
                "награда: " + goldReward + " зол., " + expReward + " оп.] " +
                position + (alive ? "" : " ☠");
    }
}