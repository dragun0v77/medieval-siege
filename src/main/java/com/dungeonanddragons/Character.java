package com.dungeonanddragons;

public class Character {

    private String name;
    private Position position;
    private int health;
    private int maxHealth;
    private int attack;
    private int defense;
    private int level;
    private int experience;
    private int gold;
    private boolean alive;

    public Character(String name, Position position, int health, int attack, int defense) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
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

        this.name = name;
        this.position = position;
        this.maxHealth = health;
        this.health = health;
        this.attack = attack;
        this.defense = defense;
        this.level = 1;
        this.experience = 0;
        this.gold = 0;
        this.alive = true;
    }

    public String getName()        { return name; }
    public Position getPosition()  { return position; }
    public int getHealth()         { return health; }
    public int getMaxHealth()      { return maxHealth; }
    public int getAttack()         { return attack; }
    public int getDefense()        { return defense; }
    public int getLevel()          { return level; }
    public int getExperience()     { return experience; }
    public int getGold()           { return gold; }
    public boolean isAlive()       { return alive; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        this.name = name;
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

    public void setAttack(int attack)   { this.attack = Math.max(0, attack); }
    public void setDefense(int defense) { this.defense = Math.max(0, defense); }

    public void setLevel(int level) {
        if (level < 1) level = 1;
        this.level = level;
    }

    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    public void setGold(int gold)             { this.gold = Math.max(0, gold); }

    public void setAlive(boolean alive) {
        this.alive = alive;
        if (!alive) {
            this.health = 0;
        } else if (this.health == 0) {
            this.health = 1;
        }
    }

    /** Перемещение по карте (манхэттенское расстояние). */
    public boolean moveTo(Position newPosition, Dungeon dungeon, int moveRange) {
        if (!alive) return false;
        if (newPosition == null || dungeon == null) return false;
        if (!dungeon.isValidPosition(newPosition)) return false;
        if (!dungeon.isWalkable(newPosition)) return false;
        if (dungeon.getMonsterAt(newPosition) != null) return false;

        double dist = position.distanceTo(newPosition);
        if (dist > moveRange) return false;

        this.position = newPosition;
        return true;
    }

    public void takeDamage(int damage) {
        if (damage <= 0) return;
        int actual = Math.max(1, damage - defense);
        health -= actual;
        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    public void heal(int amount) {
        if (amount <= 0) return;
        health = Math.min(maxHealth, health + amount);
        if (health > 0) alive = true;
    }

    public void attack(Monster monster) {
        if (!alive || monster == null || !monster.isAlive()) return;
        monster.takeDamage(this.attack);
    }

    public void addGold(int amount) {
        if (amount <= 0) return;
        gold += amount;
    }

    public boolean spendGold(int amount) {
        if (amount <= 0) return false;
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }

    public void gainExperience(int amount) {
        if (amount <= 0) return;
        experience += amount;
        while (experience >= experienceToNextLevel()) {
            int need = experienceToNextLevel();
            experience -= need;
            levelUp();
        }
    }

    public int experienceToNextLevel() {
        return level * 100;
    }

    private void levelUp() {
        level++;
        maxHealth += 20;
        health = maxHealth;
        attack += 3;
        defense += 2;
    }

    public double getHealthPercentage() {
        if (maxHealth == 0) return 0;
        return (double) health / maxHealth * 100.0;
    }

    @Override
    public String toString() {
        return name + " [Ур." + level + ", " + health + "/" + maxHealth + " HP, " +
                "АТК " + attack + ", ЗАЩ " + defense + ", " +
                "опыт " + experience + "/" + experienceToNextLevel() + ", " +
                "золото " + gold + "] " + position +
                (alive ? "" : " ☠");
    }
}