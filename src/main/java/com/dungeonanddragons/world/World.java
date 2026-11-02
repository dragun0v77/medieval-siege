package com.dungeonanddragons.world;

import com.dungeonanddragons.Character;
import com.dungeonanddragons.Pathfinder;
import com.dungeonanddragons.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Открытый мир: деревня, лес, озёра, лагеря, руины и подземелье с драконом на одной карте.
 * <p>
 * Вне боя герой ходит свободно. Когда враг замечает героя (путь до него не длиннее его
 * «зоны внимания»), начинается пошаговый бой прямо на карте: у героя {@link #HERO_SPEED} очков хода,
 * шаг стоит 1 очко, удар (шаг в сторону врага) заканчивает ход. Потом враги идут к герою
 * кратчайшим путём (обход в ширину) и бьют. Класс ничего не знает про окна.
 */
public class World {

    public enum Tile { GRASS, TREE, WATER, WALL, FLOOR, ROAD, HOUSE, GATE }

    public static final int W = 64, H = 44;
    public static final int HERO_SPEED = 5;
    private static final int DX1 = 44, DX2 = 62, DY1 = 3, DY2 = 18; // рамка подземелья
    private static final int FLASK_HEAL = 50;

    private final Tile[][] tiles = new Tile[W][H];
    private final List<Mob> mobs = new ArrayList<>();
    private final Map<Position, Prop> props = new HashMap<>();
    private final Character hero = new Character("Рыцарь", new Position(10, 33), 100, 25, 5);
    private Position respawn = new Position(10, 31);
    private int flasks = 3, maxFlasks = 3, movePoints;
    private boolean inCombat, victory, died;
    private Consumer<String> logger = s -> { };

    public World() {
        build();
        log("Добро пожаловать! WASD — ходить, E — отдохнуть у места благодати, F — фляга, Пробел — закончить ход в бою.");
    }

    // ---------- Состояние ----------

    public Character hero()            { return hero; }
    public List<Mob> mobs()            { return mobs; }
    public Map<Position, Prop> props() { return props; }
    public boolean inCombat()          { return inCombat; }
    public int movePoints()            { return movePoints; }
    public int flasks()                { return flasks; }
    public int maxFlasks()             { return maxFlasks; }
    public Tile tile(int x, int y)     { return tiles[x][y]; }
    public void setLogger(Consumer<String> l) { logger = l == null ? s -> { } : l; }

    public boolean pollDeath()   { boolean d = died; died = false; return d; }
    public boolean pollVictory() { boolean v = victory; victory = false; return v; }

    private void log(String s) { logger.accept(s); }

    public static boolean inside(Position p) {
        return p.getX() >= 0 && p.getY() >= 0 && p.getX() < W && p.getY() < H;
    }

    public boolean walkable(Position p) {
        if (!inside(p)) return false;
        Tile t = tiles[p.getX()][p.getY()];
        return t == Tile.GRASS || t == Tile.ROAD || t == Tile.FLOOR || t == Tile.GATE;
    }

    public boolean inDungeon(Position p) {
        return p.getX() > DX1 && p.getX() < DX2 && p.getY() > DY1 && p.getY() < DY2;
    }

    public String areaName(Position p) {
        if (inDungeon(p)) return "Подземелье дракона";
        if (p.getX() >= 4 && p.getX() <= 16 && p.getY() >= 27 && p.getY() <= 37) return "Деревня Дубовая Застава";
        return "Окрестности";
    }

    public Mob mobAt(Position p) {
        for (Mob m : mobs) {
            if (m.isAlive() && m.getPosition().equals(p)) return m;
        }
        return null;
    }

    private boolean free(Position p) {
        Prop pr = props.get(p);
        return walkable(p) && mobAt(p) == null && !(pr != null && pr.blocks());
    }

    /** Клетки, куда герой дойдёт в этот ход (для подсветки в бою). */
    public Set<Position> reachable() {
        return Pathfinder.search(hero.getPosition(), W, H, this::free, movePoints).getReachable();
    }

    // ---------- Действия героя ----------

    /** Шаг героя. Возвращает жителя деревни, в которого он упёрся (для разговора), или null. */
    public Prop move(int dx, int dy) {
        if (!hero.isAlive()) return null;
        Position from = hero.getPosition();
        Position to = new Position(from.getX() + dx, from.getY() + dy);
        if (!inside(to)) return null;

        Mob m = mobAt(to);
        if (m != null) { attack(m); return null; }

        Prop p = props.get(to);
        if (p != null && p.blocks()) return p;
        if (!walkable(to)) return null;

        hero.setPosition(to);
        if (inCombat) movePoints--;
        stepEffects(to, from, p);
        afterHeroAction();
        return null;
    }

    private void stepEffects(Position to, Position from, Prop p) {
        if (tiles[to.getX()][to.getY()] == Tile.GATE) {
            log(inDungeon(from) ? "Вы покидаете подземелье." : "Вы входите в подземелье. Здесь темно...");
        }
        if (p == null) return;
        if (p.getKind() == Prop.Kind.CHEST) {
            props.remove(to);
            applyLoot(p);
        } else if (p.getKind() == Prop.Kind.GRACE) {
            respawn = new Position(to.getX(), to.getY());
            log("Место благодати. Нажмите E, чтобы отдохнуть (враги тогда возродятся).");
        }
    }

    private void applyLoot(Prop p) {
        String what;
        switch (p.getLoot()) {
            case GOLD    -> { hero.addGold(p.getAmount()); what = "+" + p.getAmount() + " золота"; }
            case ATTACK  -> { hero.setAttack(hero.getAttack() + p.getAmount()); what = "+" + p.getAmount() + " к атаке"; }
            case DEFENSE -> { hero.setDefense(hero.getDefense() + p.getAmount()); what = "+" + p.getAmount() + " к защите"; }
            case FLASK   -> { maxFlasks++; flasks++; what = "+1 заряд фляги"; }
            default      -> {
                hero.setMaxHealth(hero.getMaxHealth() + p.getAmount());
                hero.heal(p.getAmount());
                what = "+" + p.getAmount() + " к максимуму здоровья";
            }
        }
        log("Найдено: «" + p.getName() + "» (" + what + ")");
    }

    private void attack(Mob m) {
        m.setEngaged(true);
        if (!inCombat) {
            inCombat = true;
            movePoints = HERO_SPEED;
            log("Бой начался!");
        }
        int d = m.takeDamage(hero.getAttack());
        log("Вы бьёте: " + m.getName() + " −" + d + " HP (" + m.getHp() + "/" + m.getMaxHp() + ")");
        if (!m.isAlive()) reward(m);
        movePoints = 0; // удар заканчивает ход
        afterHeroAction();
    }

    private void reward(Mob m) {
        int level = hero.getLevel();
        hero.addGold(m.getGold());
        hero.gainExperience(m.getExp());
        log(m.getName() + " повержен! +" + m.getGold() + " золота, +" + m.getExp() + " опыта.");
        if (hero.getLevel() > level) log("Новый уровень: " + hero.getLevel() + "! Здоровье восстановлено, характеристики выросли.");
        if (m.isBoss()) {
            victory = true;
            log("*** ДРАКОН ПОВЕРЖЕН! ***");
        }
    }

    /** Закончить ход в бою. */
    public void endTurn() {
        if (!inCombat) return;
        movePoints = 0;
        afterHeroAction();
    }

    /** Выпить флягу: лечит {@value #FLASK_HEAL} HP, в бою занимает весь ход. */
    public void drink() {
        if (flasks <= 0) { log("Фляга пуста. Отдохните у места благодати."); return; }
        if (hero.getHealth() >= hero.getMaxHealth()) { log("Здоровье полное."); return; }
        flasks--;
        int before = hero.getHealth();
        hero.heal(FLASK_HEAL);
        log("Вы пьёте из фляги: +" + (hero.getHealth() - before) + " HP (осталось зарядов: " + flasks + ")");
        if (inCombat) { movePoints = 0; afterHeroAction(); }
    }

    /** Отдых у места благодати: полное здоровье, фляга полная, враги возрождаются (кроме убитого дракона). */
    public void rest() {
        Prop p = props.get(hero.getPosition());
        if (inCombat) { log("В бою отдыхать нельзя!"); return; }
        if (p == null || p.getKind() != Prop.Kind.GRACE) { log("Здесь не отдохнуть — найдите место благодати."); return; }
        hero.heal(hero.getMaxHealth());
        flasks = maxFlasks;
        respawn = new Position(hero.getPosition().getX(), hero.getPosition().getY());
        resetMobs();
        log("Вы отдохнули у места благодати. Здоровье восстановлено, но враги вернулись.");
    }

    private void resetMobs() {
        for (Mob m : mobs) {
            if (!(m.isBoss() && !m.isAlive())) m.reset();
        }
        inCombat = false;
    }

    // ---------- Деревня ----------

    public static String[] shopItems() {
        return new String[]{"Меч: +3 к атаке (60 зол.)", "Броня: +2 к защите (50 зол.)", "Фляга: +1 заряд (40 зол.)"};
    }

    public boolean buy(int index) {
        int cost = new int[]{60, 50, 40}[index];
        if (!hero.spendGold(cost)) { log("Не хватает золота."); return false; }
        switch (index) {
            case 0 -> hero.setAttack(hero.getAttack() + 3);
            case 1 -> hero.setDefense(hero.getDefense() + 2);
            default -> { maxFlasks++; flasks++; }
        }
        log("Куплено: " + shopItems()[index]);
        return true;
    }

    public void healerHeal() {
        if (hero.getHealth() >= hero.getMaxHealth()) { log("Целительница: «Ты совершенно здоров.»"); return; }
        if (!hero.spendGold(15)) { log("Не хватает золота (нужно 15)."); return; }
        hero.heal(hero.getMaxHealth());
        log("Целительница вылечила вас за 15 золота.");
    }

    // ---------- Враги: внимание и ход ----------

    private void afterHeroAction() {
        refreshEngagement();
        if (inCombat && movePoints <= 0) enemyPhase();
    }

    /** Кто заметил героя: считаем длину пути обходом в ширину (стены учитываются). */
    private void refreshEngagement() {
        Pathfinder.Result r = Pathfinder.search(hero.getPosition(), W, H, this::walkable, 10);
        boolean was = inCombat, now = false;
        for (Mob m : mobs) {
            if (!m.isAlive()) continue;
            int d = r.distanceTo(m.getPosition());
            if (!m.isEngaged()) {
                if (d >= 0 && d <= m.getAggro()) m.setEngaged(true);
            } else if (d < 0 || d > m.getAggro() + 4) {
                m.setEngaged(false);
            }
            if (m.isEngaged()) now = true;
        }
        inCombat = now;
        if (!was && now) { movePoints = HERO_SPEED; log("Бой начался!"); }
        if (was && !now) log("Бой окончен.");
    }

    private void enemyPhase() {
        Position hp = hero.getPosition();
        List<Mob> acting = new ArrayList<>();
        for (Mob m : mobs) {
            if (m.isAlive() && m.isEngaged()) acting.add(m);
        }
        acting.sort(Comparator.comparingInt(m -> Math.abs(m.getPosition().getX() - hp.getX())
                + Math.abs(m.getPosition().getY() - hp.getY())));

        for (Mob m : acting) {
            chase(m);
            if (Pathfinder.isAdjacent(m.getPosition(), hero.getPosition())) {
                int before = hero.getHealth();
                hero.takeDamage(m.getAttack());
                log(m.getName() + " бьёт: −" + (before - hero.getHealth()) + " HP ("
                        + hero.getHealth() + "/" + hero.getMaxHealth() + ")");
                if (!hero.isAlive()) { die(); return; }
            }
        }
        movePoints = HERO_SPEED;
        refreshEngagement();
    }

    /** Враг идёт к герою по кратчайшему пути на сколько хватает скорости. */
    private void chase(Mob m) {
        Position target = hero.getPosition();
        if (Pathfinder.isAdjacent(m.getPosition(), target)) return;

        Pathfinder.Result r = Pathfinder.search(m.getPosition(), W, H,
                p -> free(p) && !p.equals(target), 16);
        Position best = null;
        for (Position n : Pathfinder.neighbours(target)) {
            if (r.isReachable(n) && (best == null || r.distanceTo(n) < r.distanceTo(best))) best = n;
        }
        if (best == null) return;
        List<Position> path = r.pathTo(best);
        int steps = Math.min(m.getSpeed(), path.size());
        if (steps > 0) m.setPosition(path.get(steps - 1));
    }

    private void die() {
        int lost = hero.getGold() / 2;
        if (lost > 0) hero.spendGold(lost);
        hero.setPosition(new Position(respawn.getX(), respawn.getY()));
        hero.heal(hero.getMaxHealth());
        flasks = maxFlasks;
        resetMobs();
        died = true;
        log("ВЫ ПОГИБЛИ. Потеряно золота: " + lost + ". Вы очнулись у места благодати.");
    }

    // ---------- Генерация карты ----------

    private void build() {
        Random rnd = new Random(7);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                boolean edge = x < 2 || y < 2 || x >= W - 2 || y >= H - 2;
                tiles[x][y] = edge ? Tile.WATER : (rnd.nextInt(100) < 14 ? Tile.TREE : Tile.GRASS);
            }
        }
        ellipse(30, 10, 6, 3);
        ellipse(14, 12, 3, 2);

        // Подземелье: рамка из стен, пол, внутренние стены с проходами, вход-арка внизу
        fill(DX1, DY1, DX2, DY2, Tile.WALL);
        fill(DX1 + 1, DY1 + 1, DX2 - 1, DY2 - 1, Tile.FLOOR);
        fill(45, 12, 61, 12, Tile.WALL);
        tiles[48][12] = Tile.FLOOR;
        tiles[58][12] = Tile.FLOOR;
        fill(53, 4, 53, 11, Tile.WALL);
        tiles[53][8] = Tile.FLOOR;
        tiles[53][DY2] = Tile.GATE;

        // Деревня
        fill(4, 27, 16, 37, Tile.GRASS);
        fill(8, 31, 12, 33, Tile.ROAD);
        // Дорога от деревни к подземелью
        fill(12, 32, 53, 32, Tile.ROAD);
        fill(53, DY2 + 1, 53, 32, Tile.ROAD);
        for (int[] h : new int[][]{{7, 29}, {13, 29}, {7, 35}, {13, 35}}) tiles[h[0]][h[1]] = Tile.HOUSE;
        prop(9, 32, Prop.of(Prop.Kind.ELDER, "Старейшина"));
        prop(11, 32, Prop.of(Prop.Kind.MERCHANT, "Торговец"));
        prop(10, 34, Prop.of(Prop.Kind.HEALER, "Целительница"));
        prop(10, 31, Prop.of(Prop.Kind.GRACE, "Место благодати"));

        // Руины с троллем
        fill(35, 23, 41, 29, Tile.GRASS);
        for (int x = 36; x <= 40; x++) {
            for (int y = 24; y <= 28; y++) {
                if (x == 36 || x == 40 || y == 24 || y == 28) tiles[x][y] = Tile.WALL;
            }
        }
        tiles[38][28] = Tile.GRASS;
        tiles[36][26] = Tile.GRASS;
        tiles[40][25] = Tile.GRASS;
        mob(Mob.Kind.TROLL, 38, 26);
        prop(39, 25, Prop.chest("Клинок руин", Prop.Loot.ATTACK, 4));
        prop(38, 30, Prop.of(Prop.Kind.GRACE, "Место благодати"));

        // Окрестности
        mob(Mob.Kind.WOLF, 24, 26); mob(Mob.Kind.WOLF, 26, 28); mob(Mob.Kind.WOLF, 25, 29);
        mob(Mob.Kind.BANDIT, 30, 38); mob(Mob.Kind.BANDIT, 32, 39); mob(Mob.Kind.BANDIT, 29, 40);
        prop(31, 40, Prop.chest("Мешок бандитов", Prop.Loot.GOLD, 80));
        mob(Mob.Kind.SKELETON, 18, 20); mob(Mob.Kind.SKELETON, 20, 22);
        prop(19, 21, Prop.chest("Старый щит", Prop.Loot.DEFENSE, 2));
        mob(Mob.Kind.GOBLIN, 43, 27); mob(Mob.Kind.GOBLIN, 45, 29);
        prop(6, 6, Prop.chest("Амулет жизни", Prop.Loot.MAX_HP, 20));
        prop(56, 36, Prop.chest("Забытый клад", Prop.Loot.GOLD, 60));

        // Подземелье
        prop(47, 16, Prop.of(Prop.Kind.GRACE, "Место благодати"));
        mob(Mob.Kind.SKELETON, 50, 15); mob(Mob.Kind.SKELETON, 57, 14);
        mob(Mob.Kind.GOBLIN, 47, 8); mob(Mob.Kind.ORC, 50, 6);
        mob(Mob.Kind.TROLL, 56, 9); mob(Mob.Kind.DRAGON, 59, 5);
        prop(46, 5, Prop.chest("Стальной меч", Prop.Loot.ATTACK, 5));
        prop(60, 16, Prop.chest("Слеза жизни", Prop.Loot.FLASK, 1));
        prop(61, 11, Prop.chest("Золото дракона", Prop.Loot.GOLD, 150));
    }

    private void fill(int x1, int y1, int x2, int y2, Tile t) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) tiles[x][y] = t;
        }
    }

    private void ellipse(int cx, int cy, int rx, int ry) {
        for (int x = cx - rx; x <= cx + rx; x++) {
            for (int y = cy - ry; y <= cy + ry; y++) {
                double dx = (x - cx) / (double) rx, dy = (y - cy) / (double) ry;
                if (dx * dx + dy * dy <= 1) tiles[x][y] = Tile.WATER;
            }
        }
    }

    /** Убирает деревья вокруг клетки, чтобы объект не оказался замурован. */
    private void open(int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (tiles[x + dx][y + dy] == Tile.TREE) tiles[x + dx][y + dy] = Tile.GRASS;
            }
        }
    }

    private void prop(int x, int y, Prop p) { open(x, y); props.put(new Position(x, y), p); }

    private void mob(Mob.Kind k, int x, int y) { open(x, y); mobs.add(Mob.of(k, x, y)); }
}
