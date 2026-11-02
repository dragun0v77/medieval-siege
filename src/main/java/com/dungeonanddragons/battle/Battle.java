package com.dungeonanddragons.battle;

import com.dungeonanddragons.Character;
import com.dungeonanddragons.Monster;
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
 * Пошаговый бой в духе «Героев 3»: поле-сетка с препятствиями, очередь ходов по скорости,
 * ход = «идти» или «идти и ударить» или «пропустить».
 * <p>
 * Класс ничего не знает про окна — только правила. Интерфейс просит у него «план хода»
 * ({@link TurnPlan}), анимирует его и затем сообщает, что ход выполнен.
 * Сейчас в бою двое (герой и один монстр), но структура рассчитана на любое число бойцов.
 */
public class Battle {

    public static final int WIDTH = 11;
    public static final int HEIGHT = 9;
    public static final int HERO_SPEED = 5;

    /** Камни на поле боя: посередине стенка, по углам — одиночные валуны. */
    private static final Set<Position> ROCKS = Set.of(
            new Position(5, 3), new Position(5, 4), new Position(5, 5),
            new Position(3, 1), new Position(3, 7),
            new Position(7, 1), new Position(7, 7));

    /** Результат удара. */
    public record AttackResult(Combatant attacker, Combatant target, int damage, boolean killed) { }

    /**
     * План одного хода: куда пройти (без стартовой клетки, может быть пустым)
     * и кого после этого ударить (может быть null).
     */
    public record TurnPlan(Combatant actor, List<Position> path, Combatant target) { }

    private final List<Combatant> combatants = new ArrayList<>();
    private final Deque<Combatant> remaining = new ArrayDeque<>(); // ещё не ходили в этом раунде
    private Combatant active;
    private int round;
    private Consumer<String> logger = message -> { };

    public Battle(Character hero, Monster monster) {
        if (hero == null || monster == null) {
            throw new IllegalArgumentException("hero и monster не могут быть null");
        }
        combatants.add(Combatant.ofHero(hero, new Position(0, HEIGHT / 2), HERO_SPEED));
        combatants.add(Combatant.ofMonster(monster, new Position(WIDTH - 1, HEIGHT / 2)));
        nextActive();
    }

    public void setLogger(Consumer<String> logger) {
        this.logger = logger == null ? message -> { } : logger;
    }

    // ----- Состояние -----

    public List<Combatant> getCombatants() { return List.copyOf(combatants); }
    public Combatant getActive()           { return active; }
    public int getRound()                  { return round; }

    public Combatant getHeroUnit()  { return unitOf(Combatant.Side.HERO); }
    public Combatant getEnemyUnit() { return unitOf(Combatant.Side.ENEMY); }

    private Combatant unitOf(Combatant.Side side) {
        for (Combatant c : combatants) {
            if (c.getSide() == side) return c;
        }
        return null;
    }

    public boolean isOver() {
        return !getHeroUnit().isAlive() || !getEnemyUnit().isAlive();
    }

    /** Победившая сторона или null, если бой продолжается. */
    public Combatant.Side getWinner() {
        if (!isOver()) return null;
        return getHeroUnit().isAlive() ? Combatant.Side.HERO : Combatant.Side.ENEMY;
    }

    public boolean isInside(Position p) {
        return p.getX() >= 0 && p.getX() < WIDTH && p.getY() >= 0 && p.getY() < HEIGHT;
    }

    public boolean isRock(Position p) {
        return ROCKS.contains(p);
    }

    public Set<Position> getRocks() {
        return ROCKS;
    }

    /** Живой боец на клетке или null. */
    public Combatant combatantAt(Position p) {
        for (Combatant c : combatants) {
            if (c.isAlive() && c.getPosition().equals(p)) return c;
        }
        return null;
    }

    // ----- Очередь ходов -----

    /** Порядок ходов в раунде: быстрые первыми, при равной скорости первым ходит герой. */
    private List<Combatant> roundOrder() {
        List<Combatant> order = new ArrayList<>();
        for (Combatant c : combatants) {
            if (c.isAlive()) order.add(c);
        }
        order.sort(Comparator.comparingInt(Combatant::getSpeed).reversed()
                .thenComparingInt(c -> c.getSide() == Combatant.Side.HERO ? 0 : 1));
        return order;
    }

    private void nextActive() {
        active = null;
        if (isOver()) return;
        while (true) {
            if (remaining.isEmpty()) {
                round++;
                remaining.addAll(roundOrder());
            }
            Combatant c = remaining.poll();
            if (c != null && c.isAlive()) {
                active = c;
                return;
            }
        }
    }

    /** Ближайшие {@code count} ходов, начиная с текущего (для полосы очереди). */
    public List<Combatant> upcomingTurns(int count) {
        List<Combatant> result = new ArrayList<>();
        if (active == null) return result;
        result.add(active);
        for (Combatant c : remaining) {
            if (c.isAlive()) result.add(c);
        }
        List<Combatant> order = roundOrder();
        while (result.size() < count && !order.isEmpty()) {
            result.addAll(order);
        }
        return new ArrayList<>(result.subList(0, Math.min(count, result.size())));
    }

    // ----- Поиск пути (обход в ширину) -----

    /** Клетки, куда боец может ступить: внутри поля, не камень, не занято другим бойцом. */
    private Pathfinder.Result search(Combatant mover, int maxSteps) {
        return Pathfinder.search(mover.getPosition(), WIDTH, HEIGHT,
                p -> !isRock(p) && combatantAt(p) == null, maxSteps);
    }

    /** Все клетки, до которых боец дойдёт за ход (включая ту, где стоит). */
    public Set<Position> reachableCells(Combatant c) {
        return search(c, c.getSpeed()).getReachable();
    }

    // ----- Планирование хода -----

    public TurnPlan planSkip(Combatant actor) {
        return new TurnPlan(actor, List.of(), null);
    }

    /** Простой ход в клетку. null — если за ход не дойти. */
    public TurnPlan planMove(Combatant actor, Position to) {
        Pathfinder.Result r = search(actor, actor.getSpeed());
        if (to.equals(actor.getPosition()) || !r.isReachable(to)) return null;
        return new TurnPlan(actor, r.pathTo(to), null);
    }

    /**
     * Подойти к цели и ударить. Выбирается ближайшая достижимая клетка рядом с целью;
     * если боец уже стоит рядом — просто удар. null — если за ход не добраться.
     */
    public TurnPlan planAttack(Combatant actor, Combatant target) {
        if (target == null || !target.isAlive() || target.getSide() == actor.getSide()) return null;

        Pathfinder.Result r = search(actor, actor.getSpeed());
        Position best = nearestNeighbour(r, target);
        if (best == null) return null;
        return new TurnPlan(actor, r.pathTo(best), target);
    }

    private static Position nearestNeighbour(Pathfinder.Result r, Combatant target) {
        Position best = null;
        for (Position n : Pathfinder.neighbours(target.getPosition())) {
            if (!r.isReachable(n)) continue;
            if (best == null || r.distanceTo(n) < r.distanceTo(best)) best = n;
        }
        return best;
    }

    /**
     * Ход противника: бьёт, если дотянется; иначе идёт по кратчайшему пути
     * к герою на сколько хватает скорости.
     */
    public TurnPlan planEnemyTurn() {
        Combatant actor = active;
        Combatant target = getHeroUnit();
        if (actor == null || actor.getSide() != Combatant.Side.ENEMY || !target.isAlive()) return null;

        TurnPlan attack = planAttack(actor, target);
        if (attack != null) return attack;

        // Далеко: ищем путь без ограничения по длине и проходим только часть
        Pathfinder.Result full = search(actor, Pathfinder.UNLIMITED);
        Position goal = nearestNeighbour(full, target);
        if (goal == null) return planSkip(actor);

        List<Position> path = full.pathTo(goal);
        List<Position> part = new ArrayList<>(path.subList(0, Math.min(actor.getSpeed(), path.size())));
        return new TurnPlan(actor, part, null);
    }

    // ----- Выполнение хода -----

    /**
     * Первая половина хода: боец переходит на конечную клетку пути.
     * Интерфейс в это время рисует анимацию, а удар делает позже, в {@link #completePlan}.
     */
    public void startPlan(TurnPlan plan) {
        requireActive(plan);
        if (!plan.path().isEmpty()) {
            plan.actor().setPosition(plan.path().get(plan.path().size() - 1));
        }
    }

    /** Вторая половина хода: удар (если он в плане) и передача хода следующему. */
    public AttackResult completePlan(TurnPlan plan) {
        requireActive(plan);

        AttackResult result = null;
        Combatant target = plan.target();
        if (target != null && target.isAlive()) {
            if (!Pathfinder.isAdjacent(plan.actor().getPosition(), target.getPosition())) {
                throw new IllegalStateException("Цель не рядом — удар невозможен");
            }
            int damage = plan.actor().hit(target);
            boolean killed = !target.isAlive();
            result = new AttackResult(plan.actor(), target, damage, killed);

            logger.accept(plan.actor().getName() + " бьёт: " + target.getName() + " −" + damage
                    + " HP (" + target.getHealth() + "/" + target.getMaxHealth() + ")");
            if (killed) {
                logger.accept(target.getName() + " погибает.");
            }
        }
        nextActive();
        return result;
    }

    private void requireActive(TurnPlan plan) {
        if (plan == null || plan.actor() != active) {
            throw new IllegalStateException("Сейчас ход другого бойца");
        }
    }
}
