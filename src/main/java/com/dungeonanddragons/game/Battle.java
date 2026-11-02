package com.dungeonanddragons.game;

import com.dungeonanddragons.Pathfinder;
import com.dungeonanddragons.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Бой как в «Героях 3»: поле-сетка, отряды ходят по очереди в порядке скорости.
 * Ход отряда: идти (обход в ширину в пределах скорости), ударить, выстрелить (стрелки),
 * ждать (походит в конце раунда), встать в защиту. Защищающийся отвечает на удар один раз за раунд.
 * Герой раз в раунд может бросить «Волшебную стрелю». Весь урон фиксированный, без случайности.
 * Класс ничего не знает про окна.
 */
public class Battle {

    public static final int W = 15, H = 11, ARROW_COST = 5;
    private static final Set<Position> ROCKS = Set.of(
            new Position(5, 2), new Position(5, 8), new Position(7, 4), new Position(7, 5),
            new Position(7, 6), new Position(9, 2), new Position(9, 8));

    /** Отряд на поле боя. Здоровье хранится одним числом: сколько осталось в сумме по всем существам. */
    public static final class Unit {
        public final UnitType type;
        public final boolean player;
        private final Squad source;
        private final int startHp;
        private int totalHp, shots;
        private Position pos;
        private boolean retaliated, defending, waited;

        private Unit(UnitType type, int count, boolean player, Squad source, Position pos) {
            this.type = type;
            this.player = player;
            this.source = source;
            this.startHp = count * type.hp;
            this.totalHp = startHp;
            this.shots = type.shots;
            this.pos = pos;
        }

        public int count()          { return (totalHp + type.hp - 1) / type.hp; }
        public boolean alive()      { return totalHp > 0; }
        public int getShots()       { return shots; }
        public Position getPos()    { return pos; }
        public boolean isDefending() { return defending; }
    }

    private final Hero hero;
    private final List<Unit> units = new ArrayList<>();
    private final Deque<Unit> queue = new ArrayDeque<>();
    private final List<Unit> waiters = new ArrayList<>();
    private Unit active;
    private int round, mana, atkBonus, defBonus, spellPower;
    private boolean spellUsed;
    private Consumer<String> logger = s -> { };

    public Battle(Hero hero, List<Squad> enemies) {
        this.hero = hero;
        this.mana = hero.getMana();
        this.atkBonus = hero.getAttack();
        this.defBonus = hero.getDefense();
        this.spellPower = hero.getSpellPower();
        place(hero.getArmy(), true, 0);
        place(enemies, false, W - 1);
        nextTurn();
    }

    private void place(List<Squad> squads, boolean player, int x) {
        int n = squads.size();
        for (int i = 0; i < n; i++) {
            Squad s = squads.get(i);
            units.add(new Unit(s.type, s.count, player, s, new Position(x, (int) ((i + 0.5) * H / n))));
        }
    }

    public void setLogger(Consumer<String> l) { logger = l == null ? s -> { } : l; }

    // ---------- Состояние ----------

    public List<Unit> units()       { return List.copyOf(units); }
    public Unit active()            { return active; }
    public int round()              { return round; }
    public int mana()               { return mana; }
    public Set<Position> rocks()    { return ROCKS; }
    public int arrowDamage()        { return 10 * spellPower + 10; }

    public Unit unitAt(Position p) {
        for (Unit u : units) {
            if (u.alive() && u.pos.equals(p)) return u;
        }
        return null;
    }

    private boolean free(Position p) {
        return !ROCKS.contains(p) && unitAt(p) == null;
    }

    private List<Unit> alive(boolean player) {
        List<Unit> list = new ArrayList<>();
        for (Unit u : units) {
            if (u.alive() && u.player == player) list.add(u);
        }
        return list;
    }

    public boolean isOver()     { return alive(true).isEmpty() || alive(false).isEmpty(); }
    public boolean playerWon()  { return isOver() && !alive(true).isEmpty(); }

    /** Клетки, куда отряд дойдёт за ход (включая ту, где стоит). */
    public Set<Position> reachable(Unit u) {
        return reach(u).getReachable();
    }

    private Pathfinder.Result reach(Unit u) {
        return Pathfinder.search(u.pos, W, H, this::free, u.type.speed);
    }

    public boolean canShoot(Unit u) {
        if (!u.type.ranged || u.shots <= 0) return false;
        for (Unit f : alive(!u.player)) {
            if (Pathfinder.isAdjacent(u.pos, f.pos)) return false; // вплотную стрелять нельзя
        }
        return true;
    }

    public boolean canCast() {
        return active != null && active.player && !spellUsed && mana >= ARROW_COST;
    }

    // ---------- Очередь ходов ----------

    private void nextTurn() {
        active = null;
        if (isOver()) return;
        while (true) {
            if (queue.isEmpty()) {
                if (!waiters.isEmpty()) {
                    waiters.sort(Comparator.comparingInt(u -> u.type.speed)); // ждавшие ходят от медленных к быстрым
                    queue.addAll(waiters);
                    waiters.clear();
                } else {
                    round++;
                    spellUsed = false;
                    List<Unit> order = new ArrayList<>();
                    for (Unit u : units) {
                        if (!u.alive()) continue;
                        u.retaliated = false;
                        u.waited = false;
                        order.add(u);
                    }
                    order.sort(Comparator.comparingInt((Unit u) -> u.type.speed).reversed()
                            .thenComparingInt(u -> u.player ? 0 : 1));
                    queue.addAll(order);
                }
            }
            Unit u = queue.poll();
            if (u != null && u.alive()) {
                u.defending = false;
                active = u;
                return;
            }
        }
    }

    // ---------- Действия ----------

    public boolean move(Unit a, Position cell) {
        if (a != active || cell.equals(a.pos) || !reach(a).isReachable(cell)) return false;
        a.pos = cell;
        nextTurn();
        return true;
    }

    /** Подойти и ударить в ближнем бою. false — за ход не добраться. */
    public boolean attack(Unit a, Unit t) {
        if (a != active || t == null || !t.alive() || t.player == a.player) return false;
        Pathfinder.Result r = reach(a);
        Position best = null;
        for (Position n : Pathfinder.neighbours(t.pos)) {
            if (r.isReachable(n) && (best == null || r.distanceTo(n) < r.distanceTo(best))) best = n;
        }
        if (best == null) return false;

        a.pos = best;
        strike(a, t, true);
        if (t.alive() && !t.retaliated) {
            t.retaliated = true;
            strike(t, a, true);
        }
        nextTurn();
        return true;
    }

    public boolean shoot(Unit a, Unit t) {
        if (a != active || !canShoot(a) || t == null || !t.alive() || t.player == a.player) return false;
        a.shots--;
        strike(a, t, false);
        nextTurn();
        return true;
    }

    public void defend(Unit a) {
        if (a != active) return;
        a.defending = true;
        log(a.type.title + " встаёт в защиту.");
        nextTurn();
    }

    public boolean waitTurn(Unit a) {
        if (a != active || a.waited) return false;
        a.waited = true;
        waiters.add(a);
        nextTurn();
        return true;
    }

    /** «Волшебная стрела»: фиксированный урон 10×сила магии+10, не тратит ход отряда. */
    public boolean castArrow(Unit t) {
        if (!canCast() || t == null || !t.alive() || t.player) return false;
        mana -= ARROW_COST;
        spellUsed = true;
        int before = t.count();
        t.totalHp = Math.max(0, t.totalHp - arrowDamage());
        log("Герой бросает Волшебную стрелу: " + t.type.title + " −" + arrowDamage()
                + " HP, погибло: " + (before - t.count()) + " (осталось " + t.count() + ")");
        if (isOver()) active = null;
        return true;
    }

    /** Отступление: армия героя теряется. */
    public void retreat() {
        for (Unit u : alive(true)) u.totalHp = 0;
        log("Армия отступила и разбита.");
        active = null;
    }

    private int damage(Unit a, Unit t, boolean melee) {
        int atk = a.type.attack + (a.player ? atkBonus : 0);
        int def = t.type.defense + (t.player ? defBonus : 0);
        double mod = atk >= def ? 1 + 0.05 * Math.min(atk - def, 60) : 1 - 0.025 * Math.min(def - atk, 28);
        if (t.defending) mod *= 0.8;
        if (melee && a.type.ranged) mod *= 0.5; // стрелок в рукопашной слабее
        return Math.max(1, (int) Math.round(a.count() * a.type.damage * mod));
    }

    private void strike(Unit a, Unit t, boolean melee) {
        int dmg = damage(a, t, melee);
        int before = t.count();
        t.totalHp = Math.max(0, t.totalHp - dmg);
        log(a.type.title + " ×" + a.count() + (melee ? " бьёт " : " стреляет в ") + t.type.title
                + ": −" + dmg + " HP, погибло: " + (before - t.count()) + " (осталось " + t.count() + ")");
    }

    private void log(String s) { logger.accept(s); }

    // ---------- Итоги ----------

    /** Опыт = сколько здоровья врагов уничтожено. */
    public int expGained() {
        int exp = 0;
        for (Unit u : units) {
            if (!u.player) exp += u.startHp - u.totalHp;
        }
        return exp;
    }

    /** Перенести потери и ману обратно в героя. */
    public void applyResult() {
        for (Unit u : units) {
            if (u.player) u.source.count = u.count();
        }
        hero.getArmy().removeIf(s -> s.count <= 0);
        hero.setMana(mana);
    }

    // ---------- Противник (и автобой) ----------

    private static int value(Unit u) { return u.count() * u.type.gold; }

    /** Ход активного отряда по простому правилу: стреляет, бьёт самого ценного из досягаемых или идёт к ближайшему. */
    public void aiAct() {
        Unit a = active;
        if (a == null) return;
        List<Unit> foes = alive(!a.player);
        if (foes.isEmpty()) return;

        if (canShoot(a)) {
            Unit target = foes.get(0);
            for (Unit f : foes) if (value(f) > value(target)) target = f;
            shoot(a, target);
            return;
        }
        Pathfinder.Result r = reach(a);
        Unit best = null;
        for (Unit f : foes) {
            for (Position n : Pathfinder.neighbours(f.pos)) {
                if (r.isReachable(n)) {
                    if (best == null || value(f) > value(best)) best = f;
                    break;
                }
            }
        }
        if (best != null) { attack(a, best); return; }

        Pathfinder.Result full = Pathfinder.search(a.pos, W, H, this::free, Pathfinder.UNLIMITED);
        List<Position> bestPath = null;
        for (Unit f : foes) {
            for (Position n : Pathfinder.neighbours(f.pos)) {
                if (!full.isReachable(n)) continue;
                List<Position> p = full.pathTo(n);
                if (!p.isEmpty() && (bestPath == null || p.size() < bestPath.size())) bestPath = p;
            }
        }
        if (bestPath == null) { defend(a); return; }
        move(a, bestPath.get(Math.min(a.type.speed, bestPath.size()) - 1));
    }
}
