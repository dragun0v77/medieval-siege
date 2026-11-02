package com.dungeonanddragons.game;

import com.dungeonanddragons.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Главное окно. Карта приключений: клик по клетке показывает путь (зелёный — дойдёшь сегодня,
 * красный — позже), второй клик по той же клетке — идти. Клик по отряду — напасть, по городу — зайти,
 * по шахте — захватить. WASD — шаг, Enter — конец дня, T — зайти в город рядом.
 */
public class GameWindow extends JFrame {

    private static final int T = 48, VW = 18, VH = 13;

    private final GameWorld world = new GameWorld();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MapView mapView = new MapView();
    private final BattleView battleView = new BattleView(this::battleFinished);
    private GameWorld.MapObject foe;

    public GameWindow() {
        super("Medieval Siege — Подземелья и драконы");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        root.add(mapView, "map");
        root.add(battleView, "battle");
        add(root);
        pack();
        setLocationRelativeTo(null);
        mapView.refresh();
    }

    GameWorld world() { return world; }

    // ---------- Бой ----------

    void startBattle(GameWorld.MapObject stack) {
        foe = stack;
        List<Squad> enemies = new ArrayList<>();
        enemies.add(new Squad(stack.unit, stack.amount));
        cards.show(root, "battle");
        battleView.start(new Battle(world.hero(), enemies));
    }

    private void battleFinished(Battle b) {
        b.applyResult();
        String text;
        if (b.playerWon()) {
            int exp = b.expGained();
            world.battleWon(foe, exp);
            text = "Победа! Получено опыта: " + exp + ".";
        } else {
            world.heroDefeated();
            text = "Поражение. Армия потеряна, герой вернулся в город.";
        }
        cards.show(root, "map");
        mapView.clearPath();
        mapView.refresh();
        JOptionPane.showMessageDialog(this, text, "Итог боя", JOptionPane.INFORMATION_MESSAGE);
        if (world.isWon()) {
            JOptionPane.showMessageDialog(this, "Красные драконы повержены! Вы победили на " + world.day() + " день.",
                    "Победа!", JOptionPane.INFORMATION_MESSAGE);
        }
        mapView.requestCanvasFocus();
    }

    // ---------- Город ----------

    void showTown() {
        JDialog dialog = new JDialog(this, "Город Дубрава", true);
        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
            p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            p.add(new JLabel("<html><b>День " + world.day() + "</b> &nbsp; Золото: " + world.gold()
                    + " &nbsp; Дерево: " + world.wood() + " &nbsp; Руда: " + world.ore()
                    + (world.builtToday() ? " &nbsp; <i>(сегодня уже строили)</i>" : "") + "</html>"));
            for (GameWorld.Building b : GameWorld.Building.values()) p.add(townRow(b, rebuild[0]));
            dialog.setContentPane(p);
            dialog.pack();
        };
        rebuild[0].run();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        mapView.refresh();
    }

    private JPanel townRow(GameWorld.Building b, Runnable rebuild) {
        UnitType u = b.unit;
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        row.add(new JLabel(new ImageIcon(Sprites.of(u).getScaledInstance(40, 40, Image.SCALE_FAST))), BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 8));
        String stats = "атака " + u.attack + ", защита " + u.defense + ", урон " + u.damage + ", HP " + u.hp
                + ", скорость " + u.speed + (u.ranged ? ", стрелок" : "");
        if (world.isBuilt(b)) {
            row.add(new JLabel("<html><b>" + u.title + "</b> — в наличии: " + world.available(u)
                    + ", цена " + u.gold + "<br><small>" + stats + "</small></html>"), BorderLayout.CENTER);
            JButton one = new JButton("Нанять 1");
            one.setEnabled(world.maxRecruit(b) >= 1);
            one.addActionListener(e -> { world.recruit(b, 1); rebuild.run(); mapView.refresh(); });
            JButton all = new JButton("Нанять всех (" + world.maxRecruit(b) + ")");
            all.setEnabled(world.maxRecruit(b) >= 1);
            all.addActionListener(e -> { world.recruit(b, world.maxRecruit(b)); rebuild.run(); mapView.refresh(); });
            buttons.add(one);
            buttons.add(all);
        } else {
            String need = b.gold + " зол." + (b.wood > 0 ? ", " + b.wood + " дерева" : "")
                    + (b.ore > 0 ? ", " + b.ore + " руды" : "")
                    + (b == GameWorld.Building.TEMPLE ? " (нужен Рыцарский зал)" : "");
            row.add(new JLabel("<html><b>" + b.title + "</b> — даёт: " + u.title + "<br><small>Стоимость: "
                    + need + "<br>" + stats + "</small></html>"), BorderLayout.CENTER);
            JButton build = new JButton("Построить");
            build.setEnabled(world.canBuild(b));
            build.addActionListener(e -> { world.build(b); rebuild.run(); mapView.refresh(); });
            buttons.add(build);
        }
        row.add(buttons, BorderLayout.EAST);
        return row;
    }

    // ---------- Карта приключений ----------

    private final class MapView extends JPanel {
        private final Canvas canvas = new Canvas();
        private final JLabel date = new JLabel();
        private final JLabel heroInfo = new JLabel();
        private final JProgressBar move = new JProgressBar(0, Hero.MAX_MOVE);
        private final ArmyPanel army = new ArmyPanel();
        private final JTextArea log = new JTextArea(9, 26);
        private final JButton endDay = new JButton("Конец дня (Enter)");
        private final JButton town = new JButton("Войти в город (T)");
        private List<Position> path = List.of();
        private Position pathTarget;
        private Timer walker;
        private boolean faceLeft;
        private int tick;

        MapView() {
            super(new BorderLayout());
            world.setLogger(s -> { log.append(s + "\n"); log.setCaretPosition(log.getDocument().getLength()); });
            log.setEditable(false);
            log.setFocusable(false);
            log.setLineWrap(true);
            log.setWrapStyleWord(true);
            move.setStringPainted(true);
            move.setForeground(new Color(70, 150, 70));
            endDay.setFocusable(false);
            town.setFocusable(false);
            endDay.addActionListener(e -> nextDay());
            town.addActionListener(e -> enterTown());

            JPanel top = new JPanel(new GridLayout(0, 1, 0, 4));
            top.add(date);
            top.add(heroInfo);
            top.add(move);
            JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
            buttons.add(endDay);
            buttons.add(town);
            JPanel mid = new JPanel(new BorderLayout(0, 6));
            mid.add(army, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(log);
            scroll.setFocusable(false);
            mid.add(scroll, BorderLayout.CENTER);

            JPanel side = new JPanel(new BorderLayout(0, 8));
            side.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            side.add(top, BorderLayout.NORTH);
            side.add(mid, BorderLayout.CENTER);
            side.add(buttons, BorderLayout.SOUTH);
            add(canvas, BorderLayout.CENTER);
            add(side, BorderLayout.EAST);

            new Timer(400, e -> { tick++; canvas.repaint(); }).start();
        }

        void requestCanvasFocus() { canvas.requestFocusInWindow(); }

        void clearPath() { stopWalking(); path = List.of(); pathTarget = null; }

        void refresh() {
            Hero h = world.hero();
            date.setText("<html><b>День " + world.day() + "</b> (неделя " + world.week() + ")<br>Золото: "
                    + world.gold() + " &nbsp; Дерево: " + world.wood() + " &nbsp; Руда: " + world.ore() + "</html>");
            heroInfo.setText("<html><b>" + h.getName() + "</b>, ур. " + h.getLevel() + " (опыт " + h.getExp()
                    + "/" + h.expToNext() + ")<br>Атака " + h.getAttack() + "  Защита " + h.getDefense()
                    + "  Магия " + h.getSpellPower() + "  Знания " + h.getKnowledge()
                    + "<br>Мана " + h.getMana() + "/" + h.maxMana() + "</html>");
            move.setValue(h.getMovePoints());
            move.setString("Очки движения: " + h.getMovePoints() + " / " + Hero.MAX_MOVE);
            town.setEnabled(world.nextToTown());
            army.repaint();
            canvas.repaint();
        }

        // ----- Управление -----

        private void nextDay() {
            clearPath();
            world.endDay();
            refresh();
            canvas.requestFocusInWindow();
        }

        private void enterTown() {
            if (world.nextToTown()) { showTown(); canvas.requestFocusInWindow(); }
        }

        private void stopWalking() {
            if (walker != null) walker.stop();
            walker = null;
        }

        private void clicked(Position p) {
            if (walker != null) return;
            if (p.equals(pathTarget) && !path.isEmpty()) { startWalking(); return; }
            path = world.pathTo(p);
            pathTarget = path.isEmpty() ? null : p;
            if (path.isEmpty() && !p.equals(world.hero().getPosition())) log.append("Туда не пройти.\n");
            canvas.repaint();
        }

        private void startWalking() {
            List<Position> steps = new ArrayList<>(path);
            walker = new Timer(130, e -> {
                if (steps.isEmpty() || (world.hero().getMovePoints() <= 0 && !isBlockingStep(steps.get(0)))) {
                    clearPath();
                    refresh();
                    return;
                }
                Position next = steps.remove(0);
                int dx = next.getX() - world.hero().getPosition().getX();
                if (dx != 0) faceLeft = dx < 0;
                GameWorld.Event ev = world.step(next);
                path = new ArrayList<>(steps);
                refresh();
                if (ev.type() != GameWorld.EventType.NONE || steps.isEmpty()) {
                    clearPath();
                    handle(ev);
                }
            });
            walker.start();
        }

        private boolean isBlockingStep(Position p) {
            GameWorld.MapObject o = world.objectAt(p);
            return o != null && o.kind == GameWorld.Kind.TOWN;
        }

        private void handle(GameWorld.Event ev) {
            switch (ev.type()) {
                case TOWN -> { showTown(); canvas.requestFocusInWindow(); }
                case BATTLE -> startBattle(ev.object());
                default -> { }
            }
        }

        private void keyStep(int dx, int dy) {
            if (walker != null) return;
            clearPath();
            Position h = world.hero().getPosition();
            if (dx != 0) faceLeft = dx < 0;
            GameWorld.Event ev = world.step(new Position(h.getX() + dx, h.getY() + dy));
            refresh();
            handle(ev);
        }

        // ----- Рисование карты -----

        private final class Canvas extends JPanel {
            private int camX, camY;

            Canvas() {
                setPreferredSize(new Dimension(VW * T, VH * T));
                setBackground(Color.BLACK);
                setFocusable(true);
                addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) {
                        requestFocusInWindow();
                        Position p = new Position(camX + e.getX() / T, camY + e.getY() / T);
                        if (GameWorld.inside(p) && e.getX() < VW * T && e.getY() < VH * T) clicked(p);
                    }
                });
                addKeyListener(new KeyAdapter() {
                    @Override public void keyPressed(KeyEvent e) {
                        char c = Character.toLowerCase(e.getKeyChar());
                        int k = e.getKeyCode();
                        if (c == 'w' || c == 'ц' || k == KeyEvent.VK_UP) keyStep(0, -1);
                        else if (c == 's' || c == 'ы' || k == KeyEvent.VK_DOWN) keyStep(0, 1);
                        else if (c == 'a' || c == 'ф' || k == KeyEvent.VK_LEFT) keyStep(-1, 0);
                        else if (c == 'd' || c == 'в' || k == KeyEvent.VK_RIGHT) keyStep(1, 0);
                        else if (k == KeyEvent.VK_ENTER) nextDay();
                        else if (c == 't' || c == 'е') enterTown();
                    }
                });
            }

            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                Position hp = world.hero().getPosition();
                camX = Math.max(0, Math.min(GameWorld.W - VW, hp.getX() - VW / 2));
                camY = Math.max(0, Math.min(GameWorld.H - VH, hp.getY() - VH / 2));

                for (int ty = 0; ty < VH; ty++) {
                    for (int tx = 0; tx < VW; tx++) drawTile(g, camX + tx, camY + ty, tx * T, ty * T);
                }
                for (GameWorld.MapObject o : world.objects()) {
                    if (!visible(o.pos)) continue;
                    boolean big = o.boss;
                    int size = big ? T * 5 / 4 : T, off = (size - T) / 2;
                    Sprites.draw(g, Sprites.of(o), sx(o.pos) - off, sy(o.pos) - off, size, false);
                    if (o.owned) {
                        g.setColor(new Color(60, 120, 230));
                        g.fillRect(sx(o.pos) + T - 14, sy(o.pos) + 2, 11, 8);
                    }
                    if (o.kind == GameWorld.Kind.STACK) badge(g, o.pos, String.valueOf(o.amount));
                }
                drawPath(g);
                Sprites.draw(g, Sprites.HERO, sx(hp), sy(hp), T, faceLeft);
            }

            private int sx(Position p) { return (p.getX() - camX) * T; }
            private int sy(Position p) { return (p.getY() - camY) * T; }

            private boolean visible(Position p) {
                return p.getX() >= camX && p.getX() < camX + VW && p.getY() >= camY && p.getY() < camY + VH;
            }

            private void badge(Graphics2D g, Position p, String text) {
                g.setFont(new Font("SansSerif", Font.BOLD, 13));
                int w = g.getFontMetrics().stringWidth(text) + 8;
                g.setColor(new Color(0, 0, 0, 190));
                g.fillRoundRect(sx(p) + T - w, sy(p) + T - 17, w, 16, 8, 8);
                g.setColor(Color.WHITE);
                g.drawString(text, sx(p) + T - w + 4, sy(p) + T - 4);
            }

            private void drawPath(Graphics2D g) {
                int mp = world.hero().getMovePoints();
                for (int i = 0; i < path.size(); i++) {
                    Position p = path.get(i);
                    if (!visible(p)) continue;
                    boolean last = i == path.size() - 1;
                    g.setColor(i < mp ? new Color(80, 230, 80) : new Color(240, 70, 70));
                    int d = last ? 18 : 10;
                    g.fillOval(sx(p) + T / 2 - d / 2, sy(p) + T / 2 - d / 2, d, d);
                    if (last) {
                        g.setColor(Color.BLACK);
                        g.drawOval(sx(p) + T / 2 - d / 2, sy(p) + T / 2 - d / 2, d, d);
                    }
                }
            }

            private int hash(int x, int y, int k) {
                int h = x * 73856093 ^ y * 19349663 ^ k * 83492791;
                h ^= h >>> 13;
                h *= 0x5bd1e995;
                h ^= h >>> 15;
                return h & 0x7fffffff;
            }

            private void speck(Graphics2D g, Color c, int x, int y, int wx, int wy, int k, int shift) {
                int h = hash(wx, wy, k), px = T / Sprites.SIZE;
                g.setColor(c);
                g.fillRect(x + ((h % Sprites.SIZE + shift) % Sprites.SIZE) * px, y + ((h >> 8) % Sprites.SIZE) * px, px, px);
            }

            private void drawTile(Graphics2D g, int wx, int wy, int x, int y) {
                GameWorld.Tile t = world.tile(wx, wy);
                switch (t) {
                    case WATER -> {
                        g.setColor(new Color(50, 100, 170));
                        g.fillRect(x, y, T, T);
                        for (int k = 0; k < 3; k++) speck(g, new Color(100, 150, 215), x, y, wx, wy, k, tick / 2 % 3);
                    }
                    case ROAD -> {
                        g.setColor(new Color(150, 124, 84));
                        g.fillRect(x, y, T, T);
                        for (int k = 0; k < 6; k++) speck(g, k % 2 == 0 ? new Color(130, 105, 70) : new Color(168, 142, 100), x, y, wx, wy, k, 0);
                    }
                    default -> {
                        g.setColor(new Color(86, 140, 64));
                        g.fillRect(x, y, T, T);
                        for (int k = 0; k < 6; k++) speck(g, k % 2 == 0 ? new Color(110, 165, 80) : new Color(66, 112, 50), x, y, wx, wy, k, 0);
                        if (t == GameWorld.Tile.TREE) g.drawImage(Sprites.TREE, x, y, T, T, null);
                    }
                }
            }
        }

        /** Список отрядов героя: спрайт, название, количество. */
        private final class ArmyPanel extends JPanel {
            ArmyPanel() { setPreferredSize(new Dimension(270, Hero.MAX_ARMY * 30 + 4)); }

            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                int y = 2;
                for (Squad s : world.hero().getArmy()) {
                    Sprites.draw(g, Sprites.of(s.type), 2, y, 28, false);
                    g.setColor(Color.DARK_GRAY);
                    g.drawString(s.type.title, 38, y + 19);
                    g.setColor(Color.BLACK);
                    String n = "×" + s.count;
                    g.drawString(n, getWidth() - g.getFontMetrics().stringWidth(n) - 6, y + 19);
                    y += 30;
                }
                if (world.hero().getArmy().isEmpty()) {
                    g.setColor(Color.DARK_GRAY);
                    g.drawString("Армии нет — наймите войска в городе.", 6, 20);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameWindow().setVisible(true));
    }
}
