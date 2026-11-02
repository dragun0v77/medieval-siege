package com.dungeonanddragons.game;

import com.dungeonanddragons.Pathfinder;
import com.dungeonanddragons.Position;

import java.util.Collection;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Карта приключений: герой ходит по дням (14 шагов в день), собирает ресурсы, захватывает шахты,
 * сражается с нейтральными отрядами и нанимает армию в городе. Цель — победить красных драконов.
 * Класс ничего не знает про окна.
 */
public class GameWorld {

    public enum Tile { GRASS, ROAD, TREE, WATER }

    public enum Kind { TOWN, GOLD_MINE, SAWMILL, ORE_PIT, GOLD_PILE, WOOD_PILE, ORE_PILE, CHEST, STACK }

    public enum EventType { NONE, TOWN, BATTLE }

    public record Event(EventType type, MapObject object) {
        static final Event NONE = new Event(EventType.NONE, null);
    }

    /** Постройки города: каждая даёт жилище существ. Строить можно одну в день. */
    public enum Building {
        BARRACKS("Казармы ополчения", UnitType.MILITIA, 0, 0, 0),
        RANGE("Стрельбище", UnitType.ARCHER, 500, 5, 0),
        HALL("Рыцарский зал", UnitType.KNIGHT, 1000, 0, 5),
        TEMPLE("Храм паладинов", UnitType.PALADIN, 3000, 5, 10);

        public final String title;
        public final UnitType unit;
        public final int gold, wood, ore;

        Building(String title, UnitType unit, int gold, int wood, int ore) {
            this.title = title;
            this.unit = unit;
            this.gold = gold;
            this.wood = wood;
            this.ore = ore;
        }
    }

    /** Объект на карте. Шахты и логова охраняются: пока охранник жив, их не захватить. */
    public static final class MapObject {
        public final Kind kind;
        public final Position pos;
        public final UnitType unit;   // для отрядов
        public final int amount;      // для отрядов — сколько существ, для ресурсов — сколько
        public boolean owned;
        public boolean boss;
        public MapObject guard;

        MapObject(Kind kind, Position pos, UnitType unit, int amount) {
            this.kind = kind;
            this.pos = pos;
            this.unit = unit;
            this.amount = amount;
        }

        public String title() {
            return switch (kind) {
                case TOWN -> "Город Дубрава";
                case GOLD_MINE -> "Золотой рудник";
                case SAWMILL -> "Лесопилка";
                case ORE_PIT -> "Каменоломня";
                case GOLD_PILE -> "Золото (" + amount + ")";
                case WOOD_PILE -> "Дерево (" + amount + ")";
                case ORE_PILE -> "Руда (" + amount + ")";
                case CHEST -> "Сундук (" + amount + " золота)";
                case STACK -> unit.title + " ×" + amount;
            };
        }
    }

    public static final int W = 40, H = 30;
    public static final int BASE_INCOME = 500, MINE_INCOME = 1000, SAWMILL_INCOME = 2, ORE_INCOME = 2;

    private final Tile[][] tiles = new Tile[W][H];
    private final Map<Position, MapObject> objects = new HashMap<>();
    private final Hero hero = new Hero("Сэр Роман", new Position(6, 25));
    private final Set<Building> built = EnumSet.of(Building.BARRACKS);
    private final Map<UnitType, Integer> available = new EnumMap<>(UnitType.class);
    private int gold = 5000, wood = 10, ore = 10, day = 1;
    private boolean builtToday, won;
    private Consumer<String> logger = s -> { };

    public GameWorld() {
        build();
        hero.addUnits(UnitType.MILITIA, 14);
        hero.addUnits(UnitType.ARCHER, 6);
        hero.addUnits(UnitType.KNIGHT, 2);
        available.put(UnitType.MILITIA, UnitType.MILITIA.growth);
    }

    // ---------- Состояние ----------

    public Hero hero()                         { return hero; }
    public int gold()                          { return gold; }
    public int wood()                          { return wood; }
    public int ore()                           { return ore; }
    public int day()                           { return day; }
    public int week()                          { return (day - 1) / 7 + 1; }
    public boolean isWon()                     { return won; }
    public boolean builtToday()                { return builtToday; }
    public boolean isBuilt(Building b)         { return built.contains(b); }
    public int available(UnitType t)           { return available.getOrDefault(t, 0); }
    public Tile tile(int x, int y)             { return tiles[x][y]; }
    public Collection<MapObject> objects()     { return objects.values(); }
    public MapObject objectAt(Position p)      { return objects.get(p); }
    public void setLogger(Consumer<String> l)  { logger = l == null ? s -> { } : l; }

    private void log(String s) { logger.accept(s); }

    public static boolean inside(Position p) {
        return p.getX() >= 0 && p.getY() >= 0 && p.getX() < W && p.getY() < H;
    }

    public boolean walkable(Position p) {
        if (!inside(p)) return false;
        Tile t = tiles[p.getX()][p.getY()];
        return t == Tile.GRASS || t == Tile.ROAD;
    }

    /** В такой объект нельзя встать: с ним взаимодействуют, подойдя вплотную. */
    private static boolean blocks(MapObject o) {
        return o.kind == Kind.TOWN || o.kind == Kind.GOLD_MINE || o.kind == Kind.SAWMILL
                || o.kind == Kind.ORE_PIT || o.kind == Kind.STACK;
    }

    public boolean guarded(MapObject o) {
        return o.guard != null && objects.get(o.guard.pos) == o.guard;
    }

    public boolean nextToTown() {
        for (Position n : Pathfinder.neighbours(hero.getPosition())) {
            MapObject o = objects.get(n);
            if (o != null && o.kind == Kind.TOWN) return true;
        }
        return false;
    }

    // ---------- Движение ----------

    /** Кратчайший путь героя (обход в ширину) до клетки; последняя клетка — цель. Пусто, если не дойти. */
    public List<Position> pathTo(Position target) {
        Pathfinder.Result r = Pathfinder.search(hero.getPosition(), W, H, p -> {
            MapObject o = objects.get(p);
            return walkable(p) && (o == null || !blocks(o) || p.equals(target));
        }, Pathfinder.UNLIMITED);
        return r.pathTo(target);
    }

    /** Один шаг на соседнюю клетку. Если там объект — взаимодействие (бой, город, шахта) вместо шага. */
    public Event step(Position to) {
        MapObject o = objects.get(to);
        if (o != null && blocks(o)) return interact(o);
        if (!walkable(to) || hero.getMovePoints() <= 0) return Event.NONE;

        hero.setPosition(to);
        hero.spendMove();
        if (o != null) collect(o);
        return Event.NONE;
    }

    private Event interact(MapObject o) {
        switch (o.kind) {
            case TOWN:
                return new Event(EventType.TOWN, o);
            case STACK:
                if (hero.getMovePoints() <= 0) { log("Нет очков движения — завершите день (Enter)."); return Event.NONE; }
                return new Event(EventType.BATTLE, o);
            default:
                if (guarded(o)) {
                    log(o.title() + " охраняется: " + o.guard.title() + ".");
                } else if (!o.owned) {
                    o.owned = true;
                    log("Захвачено: " + o.title() + ".");
                } else {
                    log(o.title() + " уже ваш.");
                }
                return Event.NONE;
        }
    }

    private void collect(MapObject o) {
        switch (o.kind) {
            case GOLD_PILE, CHEST -> gold += o.amount;
            case WOOD_PILE -> wood += o.amount;
            case ORE_PILE -> ore += o.amount;
            default -> { return; }
        }
        objects.remove(o.pos);
        log("Подобрано: " + o.title() + ".");
    }

    // ---------- День ----------

    public void endDay() {
        day++;
        hero.newDay();
        int mines = 0, saws = 0, pits = 0;
        for (MapObject o : objects.values()) {
            if (!o.owned) continue;
            if (o.kind == Kind.GOLD_MINE) mines++;
            if (o.kind == Kind.SAWMILL) saws++;
            if (o.kind == Kind.ORE_PIT) pits++;
        }
        int income = BASE_INCOME + MINE_INCOME * mines;
        gold += income;
        wood += SAWMILL_INCOME * saws;
        ore += ORE_INCOME * pits;
        builtToday = false;
        log("День " + day + ". Доход: " + income + " золота"
                + (saws > 0 ? ", " + SAWMILL_INCOME * saws + " дерева" : "")
                + (pits > 0 ? ", " + ORE_INCOME * pits + " руды" : "") + ".");
        if ((day - 1) % 7 == 0) {
            for (Building b : built) available.merge(b.unit, b.unit.growth, Integer::sum);
            log("Новая неделя! В городе прибавилось существ.");
        }
    }

    // ---------- Город ----------

    public boolean canBuild(Building b) {
        if (built.contains(b) || builtToday) return false;
        if (b == Building.TEMPLE && !built.contains(Building.HALL)) return false;
        return gold >= b.gold && wood >= b.wood && ore >= b.ore;
    }

    public void build(Building b) {
        if (!canBuild(b)) return;
        gold -= b.gold;
        wood -= b.wood;
        ore -= b.ore;
        built.add(b);
        builtToday = true;
        available.merge(b.unit, b.unit.growth, Integer::sum);
        log("Построено: " + b.title + ".");
    }

    public int maxRecruit(Building b) {
        if (!built.contains(b)) return 0;
        return Math.min(available(b.unit), gold / b.unit.gold);
    }

    public void recruit(Building b, int count) {
        int n = Math.min(count, maxRecruit(b));
        if (n <= 0) return;
        if (!hero.addUnits(b.unit, n)) { log("В армии нет места для нового отряда (максимум 7)."); return; }
        gold -= n * b.unit.gold;
        available.merge(b.unit, -n, Integer::sum);
        log("Нанято: " + b.unit.title + " ×" + n + ".");
    }

    // ---------- Итоги боя ----------

    public void battleWon(MapObject foe, int exp) {
        objects.remove(foe.pos);
        for (String m : hero.gainExp(exp)) log(m);
        if (foe.boss) won = true;
    }

    /** Армия разбита: герой возвращается в город без войск. */
    public void heroDefeated() {
        hero.getArmy().clear();
        hero.setPosition(new Position(6, 25));
        log("Герой разбит и вернулся в город. Наймите новую армию!");
    }

    // ---------- Генерация карты ----------

    private void build() {
        Random rnd = new Random(11);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                boolean edge = x < 1 || y < 1 || x >= W - 1 || y >= H - 1;
                tiles[x][y] = edge ? Tile.WATER : (rnd.nextInt(100) < 12 ? Tile.TREE : Tile.GRASS);
            }
        }
        ellipse(14, 8, 4, 3);
        ellipse(30, 16, 3, 2);
        ellipse(9, 12, 2, 2);

        // дороги
        for (int x = 6; x <= 36; x++) tiles[x][25] = Tile.ROAD;
        for (int y = 5; y <= 25; y++) tiles[20][y] = Tile.ROAD;
        for (int x = 20; x <= 34; x++) tiles[x][5] = Tile.ROAD;

        // город и герой
        put(Kind.TOWN, 6, 24, null, 0);

        // нейтральные отряды и то, что они охраняют
        put(Kind.STACK, 15, 25, UnitType.WOLF, 15);
        put(Kind.STACK, 10, 18, UnitType.GOBLIN, 25);
        MapObject mine = put(Kind.GOLD_MINE, 22, 20, null, 0);
        mine.guard = put(Kind.STACK, 22, 21, UnitType.TROLL, 8);
        MapObject saw = put(Kind.SAWMILL, 12, 15, null, 0);
        saw.guard = put(Kind.STACK, 13, 15, UnitType.WOLF, 12);
        MapObject pit = put(Kind.ORE_PIT, 30, 10, null, 0);
        pit.guard = put(Kind.STACK, 30, 11, UnitType.ORC, 18);
        put(Kind.STACK, 26, 12, UnitType.SKELETON, 30);
        put(Kind.CHEST, 26, 11, null, 1500);
        MapObject dragon = put(Kind.STACK, 35, 5, UnitType.DRAGON, 4);
        dragon.boss = true;
        put(Kind.CHEST, 36, 5, null, 5000);

        // россыпи
        put(Kind.GOLD_PILE, 9, 22, null, 700);   put(Kind.GOLD_PILE, 18, 22, null, 1000);
        put(Kind.GOLD_PILE, 33, 25, null, 800);  put(Kind.GOLD_PILE, 12, 20, null, 500);
        put(Kind.GOLD_PILE, 33, 10, null, 1000); put(Kind.CHEST, 16, 19, null, 1500);
        put(Kind.CHEST, 20, 10, null, 1500);
        put(Kind.WOOD_PILE, 8, 28, null, 8);     put(Kind.WOOD_PILE, 24, 24, null, 6);
        put(Kind.WOOD_PILE, 8, 8, null, 5);
        put(Kind.ORE_PILE, 4, 27, null, 8);      put(Kind.ORE_PILE, 28, 22, null, 6);
        put(Kind.ORE_PILE, 35, 12, null, 7);
    }

    private MapObject put(Kind kind, int x, int y, UnitType unit, int amount) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (tiles[x + dx][y + dy] == Tile.TREE) tiles[x + dx][y + dy] = Tile.GRASS;
            }
        }
        if (tiles[x][y] == Tile.WATER) tiles[x][y] = Tile.GRASS;
        MapObject o = new MapObject(kind, new Position(x, y), unit, amount);
        objects.put(o.pos, o);
        return o;
    }

    private void ellipse(int cx, int cy, int rx, int ry) {
        for (int x = cx - rx; x <= cx + rx; x++) {
            for (int y = cy - ry; y <= cy + ry; y++) {
                double dx = (x - cx) / (double) rx, dy = (y - cy) / (double) ry;
                if (dx * dx + dy * dy <= 1) tiles[x][y] = Tile.WATER;
            }
        }
    }
}
