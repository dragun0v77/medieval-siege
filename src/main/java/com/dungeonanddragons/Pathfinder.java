package com.dungeonanddragons;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Поиск пути обходом в ширину (BFS) по клеточной карте.
 * Ходить можно в 4 стороны (вверх / вправо / вниз / влево), как в консольной версии игры.
 * Один и тот же поиск используется и на карте подземелья, и на поле боя.
 */
public final class Pathfinder {

    /** Без ограничения по длине пути. */
    public static final int UNLIMITED = Integer.MAX_VALUE;

    private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private Pathfinder() { }

    /** Результат поиска: расстояния от стартовой клетки до всех достижимых клеток. */
    public static final class Result {
        private final Position start;
        private final Map<Position, Integer> distance;
        private final Map<Position, Position> parent;

        private Result(Position start, Map<Position, Integer> distance, Map<Position, Position> parent) {
            this.start = start;
            this.distance = distance;
            this.parent = parent;
        }

        public Position getStart() {
            return start;
        }

        /** Можно ли дойти до клетки (старт тоже считается достижимым). */
        public boolean isReachable(Position p) {
            return distance.containsKey(p);
        }

        /** Длина кратчайшего пути в шагах или -1, если дойти нельзя. */
        public int distanceTo(Position p) {
            Integer d = distance.get(p);
            return d == null ? -1 : d;
        }

        /** Все достижимые клетки, включая стартовую. */
        public Set<Position> getReachable() {
            return Collections.unmodifiableSet(distance.keySet());
        }

        /**
         * Кратчайший путь до клетки: список шагов БЕЗ стартовой клетки, последняя клетка — цель.
         * Если цель недостижима или совпадает со стартом — пустой список.
         */
        public List<Position> pathTo(Position target) {
            if (!isReachable(target)) return Collections.emptyList();

            List<Position> path = new ArrayList<>();
            Position current = target;
            while (!current.equals(start)) {
                path.add(current);
                current = parent.get(current);
            }
            Collections.reverse(path);
            return path;
        }
    }

    /**
     * Обход в ширину от клетки start.
     *
     * @param passable можно ли ступить в клетку (вызывается только для клеток внутри карты)
     * @param maxSteps не дальше скольких шагов искать (UNLIMITED — без ограничения)
     */
    public static Result search(Position start, int width, int height,
                                Predicate<Position> passable, int maxSteps) {
        Map<Position, Integer> distance = new HashMap<>();
        Map<Position, Position> parent = new HashMap<>();
        Queue<Position> queue = new ArrayDeque<>();

        distance.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            int d = distance.get(current);
            if (d >= maxSteps) continue;

            for (Position next : neighbours(current)) {
                if (next.getX() < 0 || next.getX() >= width
                        || next.getY() < 0 || next.getY() >= height) continue;
                if (distance.containsKey(next)) continue;
                if (!passable.test(next)) continue;

                distance.put(next, d + 1);
                parent.put(next, current);
                queue.add(next);
            }
        }
        return new Result(start, distance, parent);
    }

    /** Четыре соседние клетки (границы карты не проверяются). */
    public static List<Position> neighbours(Position p) {
        List<Position> result = new ArrayList<>(4);
        for (int[] dir : DIRECTIONS) {
            result.add(new Position(p.getX() + dir[0], p.getY() + dir[1]));
        }
        return result;
    }

    /** Стоят ли клетки рядом по стороне. */
    public static boolean isAdjacent(Position a, Position b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) == 1;
    }
}
