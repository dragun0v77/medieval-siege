package com.dungeonanddragons;

public class Resource {
    private String name;
    private int amount; // текущее кол-во ресурсов
    private int maxAmount;

    public Resource(String name, int initialAmount, int maxAmount) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (maxAmount <= 0) {
            throw new IllegalArgumentException("maxAmount должен быть > 0");
        }
        if (initialAmount < 0) {
            throw new IllegalArgumentException("initialAmount не может быть отрицательным");
        }

        this.name = name;
        this.maxAmount = maxAmount;
        this.amount = Math.min(initialAmount, maxAmount);
    }

    public String getName()     { return name; }
    public int getAmount()      { return amount; }
    public int getMaxAmount()   { return maxAmount; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        this.name = name;
    }

    public void setMaxAmount(int maxAmount) {
        if (maxAmount <= 0) {
            throw new IllegalArgumentException("maxAmount должен быть > 0");
        }
        this.maxAmount = maxAmount;
        if (this.amount > maxAmount) this.amount = maxAmount;
    }

    public boolean addAmount(int value) {
        if (value <= 0) return false;
        if (amount + value > maxAmount) return false;
        amount += value;
        return true;
    }

    public boolean consumeAmount(int value) {
        if (value <= 0) return false;
        if (amount < value) return false;
        amount -= value;
        return true;
    }

    public boolean isFull() {
        return amount >= maxAmount;
    }

    public boolean isEmpty() {
        return amount <= 0;
    }

    @Override
    public String toString() {
        return name + ": " + amount + "/" + maxAmount;
    }
}