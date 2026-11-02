package com.dungeonanddragons.world;

import com.dungeonanddragons.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Окно игры. Управление: WASD / стрелки (на русской раскладке ц ф ы в тоже работают),
 * Пробел — закончить ход в бою, F — фляга, E — отдохнуть у места благодати.
 * Шаг в сторону врага = удар, шаг в жителя деревни = разговор.
 */
public class WorldWindow extends JFrame {

    private static final int PX = 4, T = PX * Sprites.SIZE, VW = 19, VH = 13;

    private final World world = new World();
    private final GamePanel panel = new GamePanel();
    private final JLabel info = new JLabel();
    private final JProgressBar hpBar = new JProgressBar();
    private final JTextArea log = new JTextArea(14, 24);
    private int tick;
    private boolean faceLeft;

    public WorldWindow() {
        super("Medieval Siege — Подземелья и драконы");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        log.setEditable(false);
        log.setFocusable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        world.setLogger(s -> {
            log.append(s + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
        hpBar.setStringPainted(true);
        hpBar.setForeground(new Color(190, 50, 50));

        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(hpBar, BorderLayout.NORTH);
        top.add(info, BorderLayout.CENTER);
        side.add(top, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(log);
        scroll.setFocusable(false);
        side.add(scroll, BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);

        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKey(e); }
        });
        new Timer(350, e -> { tick++; panel.repaint(); }).start();
        refresh();
    }

    World world() { return world; }

    // ---------- Управление ----------

    private void onKey(KeyEvent e) {
        int dx = 0, dy = 0;
        char c = java.lang.Character.toLowerCase(e.getKeyChar());
        int code = e.getKeyCode();

        if (c == 'w' || c == 'ц' || code == KeyEvent.VK_UP) dy = -1;
        else if (c == 's' || c == 'ы' || code == KeyEvent.VK_DOWN) dy = 1;
        else if (c == 'a' || c == 'ф' || code == KeyEvent.VK_LEFT) { dx = -1; faceLeft = true; }
        else if (c == 'd' || c == 'в' || code == KeyEvent.VK_RIGHT) { dx = 1; faceLeft = false; }
        else if (code == KeyEvent.VK_SPACE) { world.endTurn(); refresh(); return; }
        else if (c == 'f' || c == 'а') { world.drink(); refresh(); return; }
        else if (c == 'e' || c == 'у') { world.rest(); refresh(); return; }
        else return;

        Prop talk = world.move(dx, dy);
        refresh();
        if (talk != null) talk(talk);
    }

    private void talk(Prop p) {
        switch (p.getKind()) {
            case ELDER -> JOptionPane.showMessageDialog(this,
                    "<html><body style='width:300px'>Дракон засел в подземелье на востоке, за дорогой. "
                            + "Но сначала окрепни: в лесу волки, на юге бандиты, а в руинах живёт тролль.<br><br>"
                            + "Ищи места благодати — у них можно отдохнуть (клавиша E). "
                            + "Только помни: враги после этого возрождаются.</body></html>",
                    "Старейшина", JOptionPane.INFORMATION_MESSAGE);
            case MERCHANT -> shop();
            case HEALER -> {
                int r = JOptionPane.showConfirmDialog(this, "Исцеление за 15 золота?", "Целительница",
                        JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) world.healerHeal();
            }
            default -> { }
        }
        refresh();
    }

    private void shop() {
        String[] items = World.shopItems();
        String[] options = new String[items.length + 1];
        System.arraycopy(items, 0, options, 0, items.length);
        options[items.length] = "Уйти";
        while (true) {
            int c = JOptionPane.showOptionDialog(this, "Золото: " + world.hero().getGold(), "Торговец",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[items.length]);
            if (c < 0 || c >= items.length) break;
            world.buy(c);
            refresh();
        }
    }

    private void refresh() {
        var h = world.hero();
        hpBar.setMaximum(h.getMaxHealth());
        hpBar.setValue(h.getHealth());
        hpBar.setString("HP " + h.getHealth() + " / " + h.getMaxHealth());
        info.setText("<html><b>" + world.areaName(h.getPosition()) + "</b>"
                + "<br>" + h.getName() + ", ур. " + h.getLevel()
                + " (опыт " + h.getExperience() + "/" + h.experienceToNextLevel() + ")"
                + "<br>Атака: " + h.getAttack() + "  Защита: " + h.getDefense()
                + "<br>Золото: " + h.getGold() + "  Фляга: " + world.flasks() + "/" + world.maxFlasks()
                + (world.inCombat() ? "<br><b>БОЙ</b>, очков хода: " + world.movePoints() : "")
                + "</html>");
        panel.repaint();

        if (world.pollDeath()) {
            JOptionPane.showMessageDialog(this, "ВЫ ПОГИБЛИ", "Конец пути?", JOptionPane.PLAIN_MESSAGE);
        }
        if (world.pollVictory()) {
            JOptionPane.showMessageDialog(this, "Дракон повержен! Вы победили.", "Победа!",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        panel.requestFocusInWindow();
    }

    // ---------- Рисование ----------

    private class GamePanel extends JPanel {
        private int camX, camY;

        GamePanel() {
            setPreferredSize(new Dimension(VW * T, VH * T));
            setBackground(Color.BLACK);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            Position hp = world.hero().getPosition();
            camX = Math.max(0, Math.min(World.W - VW, hp.getX() - VW / 2));
            camY = Math.max(0, Math.min(World.H - VH, hp.getY() - VH / 2));

            for (int ty = 0; ty < VH; ty++) {
                for (int tx = 0; tx < VW; tx++) drawTile(g, camX + tx, camY + ty, tx * T, ty * T);
            }
            if (world.inCombat()) {
                g.setColor(new Color(90, 170, 255, 70));
                for (Position p : world.reachable()) {
                    if (visible(p)) g.fillRect(sx(p), sy(p), T, T);
                }
            }
            for (Map.Entry<Position, Prop> e : world.props().entrySet()) {
                Position p = e.getKey();
                if (!visible(p)) continue;
                if (e.getValue().getKind() == Prop.Kind.GRACE) {
                    g.setColor(new Color(255, 220, 100, 50 + (tick % 2) * 40));
                    g.fillOval(sx(p) - T / 3, sy(p) - T / 3, T * 5 / 3, T * 5 / 3);
                }
                g.drawImage(Sprites.of(e.getValue().getKind()), sx(p), sy(p), T, T, null);
            }
            for (Mob m : world.mobs()) {
                Position p = m.getPosition();
                if (!m.isAlive() || !visible(p)) continue;
                int size = m.isBoss() ? T * 5 / 4 : T;
                int off = (size - T) / 2;
                g.drawImage(Sprites.of(m.getKind()), sx(p) - off, sy(p) - off, size, size, null);
                if (m.isEngaged()) {
                    g.setColor(new Color(255, 60, 60));
                    g.drawRect(sx(p) + 1, sy(p) + 1, T - 3, T - 3);
                }
                if (m.isEngaged() || m.getHp() < m.getMaxHp()) bar(g, p, m.getHp(), m.getMaxHp());
            }
            var h = world.hero();
            BufferedImage hero = Sprites.HERO;
            if (faceLeft) g.drawImage(hero, sx(hp) + T, sy(hp), -T, T, null);
            else g.drawImage(hero, sx(hp), sy(hp), T, T, null);
            if (world.inCombat()) bar(g, hp, h.getHealth(), h.getMaxHealth());

            if (world.inDungeon(hp)) darkness(g, hp);
            if (world.inCombat()) banner(g);
        }

        private int sx(Position p) { return (p.getX() - camX) * T; }
        private int sy(Position p) { return (p.getY() - camY) * T; }

        private boolean visible(Position p) {
            return p.getX() >= camX && p.getX() < camX + VW && p.getY() >= camY && p.getY() < camY + VH;
        }

        private void bar(Graphics2D g, Position p, int hp, int max) {
            g.setColor(Color.BLACK);
            g.fillRect(sx(p) + 4, sy(p) - 2, T - 8, 6);
            g.setColor(new Color(80, 210, 100));
            g.fillRect(sx(p) + 5, sy(p) - 1, (T - 10) * hp / Math.max(1, max), 4);
        }

        private void darkness(Graphics2D g, Position hp) {
            Point2D c = new Point2D.Float(sx(hp) + T / 2f, sy(hp) + T / 2f);
            g.setPaint(new RadialGradientPaint(c, T * 6f, new float[]{0.45f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 235)}));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        private void banner(Graphics2D g) {
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < World.HERO_SPEED; i++) dots.append(i < world.movePoints() ? "● " : "○ ");
            g.setColor(new Color(0, 0, 0, 170));
            g.fillRect(0, 0, getWidth(), 28);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("БОЙ   ход: " + dots + "  |  Пробел — закончить ход   F — фляга", 12, 19);
        }

        // ----- Тайлы -----

        private int hash(int x, int y, int k) {
            int h = x * 73856093 ^ y * 19349663 ^ k * 83492791;
            h ^= h >>> 13;
            h *= 0x5bd1e995;
            h ^= h >>> 15;
            return h & 0x7fffffff;
        }

        private void speck(Graphics2D g, Color c, int x, int y, int wx, int wy, int k, int shift) {
            int h = hash(wx, wy, k);
            g.setColor(c);
            g.fillRect(x + ((h % Sprites.SIZE + shift) % Sprites.SIZE) * PX,
                    y + ((h >> 8) % Sprites.SIZE) * PX, PX, PX);
        }

        private void fillTile(Graphics2D g, Color base, Color a, Color b, int x, int y, int wx, int wy, int n) {
            g.setColor(base);
            g.fillRect(x, y, T, T);
            for (int k = 0; k < n; k++) speck(g, k % 2 == 0 ? a : b, x, y, wx, wy, k, 0);
        }

        private void drawTile(Graphics2D g, int wx, int wy, int x, int y) {
            World.Tile t = world.tile(wx, wy);
            switch (t) {
                case GRASS, TREE, HOUSE -> {
                    fillTile(g, new Color(86, 140, 64), new Color(110, 165, 80), new Color(66, 112, 50), x, y, wx, wy, 6);
                    if (t == World.Tile.TREE) g.drawImage(Sprites.TREE, x, y, T, T, null);
                    if (t == World.Tile.HOUSE) g.drawImage(Sprites.HOUSE, x, y, T, T, null);
                }
                case WATER -> {
                    g.setColor(new Color(50, 100, 170));
                    g.fillRect(x, y, T, T);
                    for (int k = 0; k < 3; k++) speck(g, new Color(100, 150, 215), x, y, wx, wy, k, tick / 2 % 3);
                }
                case ROAD -> fillTile(g, new Color(150, 124, 84), new Color(130, 105, 70), new Color(168, 142, 100), x, y, wx, wy, 6);
                case FLOOR -> fillTile(g, new Color(46, 42, 52), new Color(62, 58, 70), new Color(36, 33, 42), x, y, wx, wy, 5);
                case WALL -> {
                    g.setColor(new Color(78, 78, 92));
                    g.fillRect(x, y, T, T);
                    g.setColor(new Color(46, 46, 56));
                    for (int r = 0; r < 3; r++) {
                        g.fillRect(x, y + r * 4 * PX, T, PX);
                        g.fillRect(x + (((wx + r) & 1) == 0 ? 3 : 9) * PX, y + r * 4 * PX, PX, 4 * PX);
                    }
                }
                case GATE -> {
                    g.setColor(new Color(46, 42, 52));
                    g.fillRect(x, y, T, T);
                    g.drawImage(Sprites.GATE, x, y, T, T, null);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WorldWindow().setVisible(true));
    }
}
