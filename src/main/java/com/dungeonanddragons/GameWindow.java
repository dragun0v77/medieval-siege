package com.dungeonanddragons;

import com.dungeonanddragons.battle.Battle;
import com.dungeonanddragons.battle.Combatant;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;

/**
 * Окно игры (Swing). Один экран: слева поле (карта подземелья или поле боя), справа статус и журнал.
 * Карта: клик по клетке — герой идёт туда по кратчайшему пути (BFS); клик по монстру — идёт к нему и начинается бой.
 * Бой: клик по зелёной клетке — ход, клик по врагу — подойти и ударить.
 */
public class GameWindow extends JFrame {

    private static final int CELL = 64;

    private final GameSession session = new GameSession();
    private final BoardPanel board = new BoardPanel();
    private final JLabel stats = new JLabel();
    private final JTextArea log = new JTextArea(14, 26);
    private final JButton skip = new JButton("Пропустить ход");

    private Battle battle;
    private Monster enemy;
    private Timer timer;          // шаги героя по карте / ход противника
    private boolean finished;

    public GameWindow() {
        super("Medieval Siege — Подземелья и драконы");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        skip.setEnabled(false);
        skip.addActionListener(e -> heroSkip());
        session.setLogger(this::log);

        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        side.add(stats, BorderLayout.NORTH);
        side.add(new JScrollPane(log), BorderLayout.CENTER);
        side.add(skip, BorderLayout.SOUTH);

        add(board, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);

        log("Клик по клетке — идти. Клик по монстру — напасть. Найди выход (золотая клетка) после победы над всеми.");
        refresh();
    }

    // ---------- Общее ----------

    private void log(String msg) {
        log.append(msg + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void refresh() {
        Character h = session.getHero();
        stats.setText("<html><b>" + h.getName() + "</b>, ур. " + h.getLevel()
                + "<br>HP: " + h.getHealth() + "/" + h.getMaxHealth()
                + "<br>Атака: " + h.getAttack() + "  Защита: " + h.getDefense()
                + "<br>Золото: " + h.getGold()
                + "<br>Монстров: " + session.getDungeon().getAliveMonsterCount()
                + (battle != null ? "<br><b>Бой, раунд " + battle.getRound() + "</b>" : "") + "</html>");
        board.repaint();
    }

    private void stopTimer() {
        if (timer != null) timer.stop();
        timer = null;
    }

    private void end(String title, String text) {
        finished = true;
        skip.setEnabled(false);
        refresh();
        JOptionPane.showMessageDialog(this, text, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------- Карта ----------

    private void onMapClick(Position to) {
        if (timer != null) return;
        Dungeon d = session.getDungeon();
        Position from = session.getHero().getPosition();
        Monster target = d.getMonsterAt(to);

        Pathfinder.Result r = Pathfinder.search(from, d.getWidth(), d.getHeight(),
                session::canStepTo, Pathfinder.UNLIMITED);

        List<Position> path;
        if (target != null) {
            if (Pathfinder.isAdjacent(from, to)) { startBattle(target); return; }
            Position best = null;
            for (Position n : Pathfinder.neighbours(to)) {
                if (r.isReachable(n) && (best == null || r.distanceTo(n) < r.distanceTo(best))) best = n;
            }
            if (best == null) { log("К монстру не пройти."); return; }
            path = r.pathTo(best);
        } else {
            if (!r.isReachable(to) || to.equals(from)) return;
            path = r.pathTo(to);
        }
        walk(path, target);
    }

    private void walk(List<Position> path, Monster target) {
        int[] i = {0};
        timer = new Timer(120, e -> {
            if (i[0] < path.size() && session.moveHero(path.get(i[0]))) {
                i[0]++;
                refresh();
                if (session.isVictory()) { stopTimer(); end("Победа!", "Подземелье пройдено!"); return; }
            }
            if (i[0] >= path.size()) {
                stopTimer();
                if (target != null && target.isAlive()) startBattle(target);
            }
        });
        timer.start();
    }

    // ---------- Бой ----------

    private void startBattle(Monster m) {
        enemy = m;
        battle = new Battle(session.getHero(), m);
        battle.setLogger(this::log);
        log("--- Бой: " + m.getName() + " ---");
        afterTurn();
    }

    private void onBattleClick(Position p) {
        if (timer != null || battle.getActive().getSide() != Combatant.Side.HERO) return;
        Combatant hero = battle.getActive();
        Combatant c = battle.combatantAt(p);

        Battle.TurnPlan plan = (c != null) ? battle.planAttack(hero, c) : battle.planMove(hero, p);
        if (plan == null) { log("Туда не дойти за один ход."); return; }
        play(plan);
    }

    private void heroSkip() {
        if (battle != null && timer == null && battle.getActive().getSide() == Combatant.Side.HERO) {
            play(battle.planSkip(battle.getActive()));
        }
    }

    private void play(Battle.TurnPlan plan) {
        battle.startPlan(plan);
        battle.completePlan(plan);
        afterTurn();
    }

    private void afterTurn() {
        refresh();
        if (battle.isOver()) {
            boolean won = battle.getWinner() == Combatant.Side.HERO;
            battle = null;
            skip.setEnabled(false);
            if (won) {
                session.rewardForVictory(enemy);
                refresh();
                if (session.isVictory()) end("Победа!", "Подземелье пройдено!");
            } else {
                end("Поражение", "Герой пал...");
            }
            return;
        }
        boolean heroTurn = battle.getActive().getSide() == Combatant.Side.HERO;
        skip.setEnabled(heroTurn);
        if (!heroTurn) {
            timer = new Timer(500, e -> {
                stopTimer();
                Battle.TurnPlan plan = battle.planEnemyTurn();
                if (plan != null) play(plan);
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    // ---------- Рисование ----------

    private class BoardPanel extends JPanel {
        BoardPanel() {
            setPreferredSize(new Dimension(Battle.WIDTH * CELL, Battle.HEIGHT * CELL));
            setBackground(new Color(30, 28, 34));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (finished) return;
                    Position p = new Position(e.getX() / CELL, e.getY() / CELL);
                    if (battle != null) {
                        if (battle.isInside(p)) onBattleClick(p);
                    } else if (session.getDungeon().isValidPosition(p)) {
                        onMapClick(p);
                    }
                }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (battle != null) paintBattle(g); else paintMap(g);
        }

        private void cell(Graphics2D g, int x, int y, Color c) {
            g.setColor(c);
            g.fillRect(x * CELL, y * CELL, CELL, CELL);
            g.setColor(new Color(0, 0, 0, 70));
            g.drawRect(x * CELL, y * CELL, CELL, CELL);
        }

        private void unit(Graphics2D g, Position p, String letter, Color color, int hp, int max) {
            int x = p.getX() * CELL, y = p.getY() * CELL;
            g.setColor(color);
            g.fillOval(x + 8, y + 6, CELL - 16, CELL - 16);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.drawString(letter, x + CELL / 2 - 8, y + CELL / 2 + 8);
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x + 6, y + CELL - 9, CELL - 12, 6);
            g.setColor(new Color(70, 200, 90));
            g.fillRect(x + 6, y + CELL - 9, (CELL - 12) * Math.max(0, hp) / Math.max(1, max), 6);
        }

        private void paintMap(Graphics2D g) {
            Dungeon d = session.getDungeon();
            for (int x = 0; x < d.getWidth(); x++) {
                for (int y = 0; y < d.getHeight(); y++) {
                    Position p = new Position(x, y);
                    cell(g, x, y, d.isWalkable(p) ? new Color(92, 84, 70) : new Color(45, 45, 52));
                }
            }
            Position ex = d.getExit();
            g.setColor(new Color(255, 200, 40));
            g.setStroke(new BasicStroke(3));
            g.drawRect(ex.getX() * CELL + 2, ex.getY() * CELL + 2, CELL - 4, CELL - 4);
            g.setStroke(new BasicStroke(1));

            for (Item it : d.getItems()) {
                if (it.isCollected()) continue;
                g.setColor(new Color(230, 200, 60));
                g.fillRect(it.getPosition().getX() * CELL + 20, it.getPosition().getY() * CELL + 20, 24, 24);
                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.drawString(it.getName().substring(0, 1), it.getPosition().getX() * CELL + 28,
                        it.getPosition().getY() * CELL + 38);
            }
            for (Monster m : d.getMonsters()) {
                if (m.isAlive()) unit(g, m.getPosition(), m.getName().substring(0, 1),
                        new Color(180, 50, 50), m.getHealth(), m.getMaxHealth());
            }
            Character h = session.getHero();
            unit(g, h.getPosition(), "Р", new Color(50, 110, 200), h.getHealth(), h.getMaxHealth());
        }

        private void paintBattle(Graphics2D g) {
            for (int x = 0; x < Battle.WIDTH; x++) {
                for (int y = 0; y < Battle.HEIGHT; y++) {
                    cell(g, x, y, battle.isRock(new Position(x, y)) ? new Color(45, 45, 52)
                            : new Color(70, 90, 64));
                }
            }
            Combatant act = battle.getActive();
            if (act != null && act.getSide() == Combatant.Side.HERO) {
                Set<Position> reach = battle.reachableCells(act);
                for (Position p : reach) {
                    g.setColor(new Color(120, 255, 120, 70));
                    g.fillRect(p.getX() * CELL, p.getY() * CELL, CELL, CELL);
                }
            }
            for (Combatant c : battle.getCombatants()) {
                if (!c.isAlive()) continue;
                unit(g, c.getPosition(), c.getName().substring(0, 1),
                        c.getSide() == Combatant.Side.HERO ? new Color(50, 110, 200) : new Color(180, 50, 50),
                        c.getHealth(), c.getMaxHealth());
                if (c == act) {
                    g.setColor(Color.YELLOW);
                    g.setStroke(new BasicStroke(3));
                    g.drawRect(c.getPosition().getX() * CELL + 2, c.getPosition().getY() * CELL + 2,
                            CELL - 4, CELL - 4);
                    g.setStroke(new BasicStroke(1));
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameWindow().setVisible(true));
    }
}
