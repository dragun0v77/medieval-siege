package com.dungeonanddragons.game;

import com.dungeonanddragons.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

/**
 * Экран боя. Мышь: клик по свободной подсвеченной клетке — идти, клик по врагу — ударить
 * (стрелок стреляет сам, если рядом нет врагов). Кнопки: заклинание, ждать, защита, отступить.
 * Хоткеи: S — заклинание, W — ждать, Пробел — защита.
 */
final class BattleView extends JPanel {

    interface Listener { void finished(Battle battle); }

    private static final int T = 48;

    private final Listener listener;
    private final Canvas canvas = new Canvas();
    private final JTextArea log = new JTextArea(16, 26);
    private final JLabel info = new JLabel();
    private final JButton arrow = new JButton();
    private final JButton waitBtn = new JButton("Ждать (W)");
    private final JButton defend = new JButton("Защита (Пробел)");
    private final JButton retreat = new JButton("Отступить");
    private final Timer aiTimer = new Timer(450, e -> aiTick());

    private Battle battle;
    private boolean spellMode;
    private Position hover;

    BattleView(Listener listener) {
        super(new BorderLayout());
        this.listener = listener;

        log.setEditable(false);
        log.setFocusable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        for (JButton b : new JButton[]{arrow, waitBtn, defend, retreat}) b.setFocusable(false);
        arrow.addActionListener(e -> toggleSpell());
        waitBtn.addActionListener(e -> doWait());
        defend.addActionListener(e -> doDefend());
        retreat.addActionListener(e -> doRetreat());

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
        buttons.add(arrow);
        buttons.add(waitBtn);
        buttons.add(defend);
        buttons.add(retreat);

        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        side.add(info, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(log);
        scroll.setFocusable(false);
        side.add(scroll, BorderLayout.CENTER);
        side.add(buttons, BorderLayout.SOUTH);
        add(canvas, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);

        canvas.setFocusable(true);
        canvas.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                char c = Character.toLowerCase(e.getKeyChar());
                if (c == 'w' || c == 'ц') doWait();
                else if (c == 's' || c == 'ы') toggleSpell();
                else if (e.getKeyCode() == KeyEvent.VK_SPACE) doDefend();
            }
        });
    }

    void start(Battle b) {
        battle = b;
        spellMode = false;
        hover = null;
        log.setText("");
        b.setLogger(this::say);
        say("Бой начался!");
        afterAction();
        canvas.requestFocusInWindow();
    }

    private void say(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    // ---------- Ход игрока ----------

    private boolean playerTurn() {
        return battle != null && !battle.isOver() && battle.active() != null
                && battle.active().player && !aiTimer.isRunning();
    }

    private void click(Position c) {
        if (!playerTurn()) return;
        Battle.Unit a = battle.active(), t = battle.unitAt(c);
        if (spellMode) {
            if (t == null || t.player) return;
            battle.castArrow(t);
            spellMode = false;
        } else if (t != null && !t.player) {
            if (battle.canShoot(a)) battle.shoot(a, t);
            else if (!battle.attack(a, t)) { say("Не дотянуться до цели."); return; }
        } else if (t == null) {
            if (!battle.move(a, c)) return;
        } else {
            return;
        }
        afterAction();
    }

    private void toggleSpell() {
        if (!playerTurn() || !battle.canCast()) return;
        spellMode = !spellMode;
        refresh();
    }

    private void doWait() {
        if (playerTurn() && battle.waitTurn(battle.active())) afterAction();
    }

    private void doDefend() {
        if (playerTurn()) { battle.defend(battle.active()); afterAction(); }
    }

    private void doRetreat() {
        if (battle == null || battle.isOver()) return;
        int r = JOptionPane.showConfirmDialog(this, "Отступить? Армия героя будет потеряна.",
                "Отступление", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        aiTimer.stop();
        battle.retreat();
        finish();
    }

    private void afterAction() {
        refresh();
        if (battle.isOver()) { finish(); return; }
        if (!battle.active().player) aiTimer.start();
    }

    private void aiTick() {
        if (battle == null || battle.isOver()) { aiTimer.stop(); return; }
        if (battle.active().player) { aiTimer.stop(); refresh(); return; }
        battle.aiAct();
        refresh();
        if (battle.isOver()) { aiTimer.stop(); finish(); }
        else if (battle.active().player) { aiTimer.stop(); refresh(); }
    }

    private void finish() {
        aiTimer.stop();
        Battle b = battle;
        battle = null;
        canvas.repaint();
        listener.finished(b);
    }

    private void refresh() {
        boolean mine = playerTurn() || (battle != null && !battle.isOver() && battle.active() != null
                && battle.active().player);
        if (battle != null && !battle.isOver() && battle.active() != null) {
            Battle.Unit a = battle.active();
            info.setText("<html><b>Раунд " + battle.round() + "</b><br>Ходит: "
                    + (a.player ? "" : "враг, ") + a.type.title + " ×" + a.count()
                    + (a.type.ranged ? "<br>Выстрелов: " + a.getShots() : "")
                    + "<br>Мана: " + battle.mana() + "</html>");
        }
        arrow.setText((spellMode ? "Выберите цель…" : "Волшебная стрела (S): " + battle.arrowDamage()
                + " урона, " + Battle.ARROW_COST + " маны"));
        arrow.setEnabled(mine && battle.canCast());
        waitBtn.setEnabled(mine && !aiTimer.isRunning());
        defend.setEnabled(mine);
        canvas.repaint();
    }

    // ---------- Рисование ----------

    private final class Canvas extends JPanel {
        Canvas() {
            setPreferredSize(new Dimension(Battle.W * T + 20, Battle.H * T + 60));
            setBackground(new Color(28, 26, 32));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    Position c = cell(e);
                    if (c != null) click(c);
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { hover = cell(e); repaint(); }
            });
        }

        private int ox() { return (getWidth() - Battle.W * T) / 2; }
        private int oy() { return 40 + Math.max(0, (getHeight() - 40 - Battle.H * T) / 2); }

        private Position cell(MouseEvent e) {
            int x = (e.getX() - ox()) / T, y = (e.getY() - oy()) / T;
            if (e.getX() < ox() || e.getY() < oy() || x >= Battle.W || y >= Battle.H) return null;
            return new Position(x, y);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (battle == null) return;
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int ox = ox(), oy = oy();

            for (int x = 0; x < Battle.W; x++) {
                for (int y = 0; y < Battle.H; y++) {
                    g.setColor((x + y) % 2 == 0 ? new Color(84, 122, 66) : new Color(76, 113, 60));
                    g.fillRect(ox + x * T, oy + y * T, T, T);
                    if (battle.rocks().contains(new Position(x, y))) {
                        g.setColor(new Color(96, 96, 110));
                        g.fillRoundRect(ox + x * T + 4, oy + y * T + 6, T - 8, T - 10, 12, 12);
                        g.setColor(new Color(64, 64, 76));
                        g.fillRoundRect(ox + x * T + 10, oy + y * T + 24, T - 20, T - 28, 8, 8);
                    }
                }
            }
            Battle.Unit active = battle.active();
            boolean mine = playerTurn();
            if (mine) {
                Set<Position> reach = battle.reachable(active);
                g.setColor(new Color(90, 170, 255, 80));
                for (Position p : reach) g.fillRect(ox + p.getX() * T, oy + p.getY() * T, T, T);
            }
            g.setColor(new Color(0, 0, 0, 50));
            for (int x = 0; x <= Battle.W; x++) g.drawLine(ox + x * T, oy, ox + x * T, oy + Battle.H * T);
            for (int y = 0; y <= Battle.H; y++) g.drawLine(ox, oy + y * T, ox + Battle.W * T, oy + y * T);

            for (Battle.Unit u : battle.units()) {
                if (!u.alive()) continue;
                int x = ox + u.getPos().getX() * T, y = oy + u.getPos().getY() * T;
                Sprites.draw(g, Sprites.of(u.type), x, y, T, !u.player);
                String n = String.valueOf(u.count());
                g.setFont(new Font("SansSerif", Font.BOLD, 13));
                int w = g.getFontMetrics().stringWidth(n) + 8;
                g.setColor(new Color(0, 0, 0, 190));
                g.fillRoundRect(x + T - w, y + T - 17, w, 16, 8, 8);
                g.setColor(u.isDefending() ? new Color(120, 200, 255) : Color.WHITE);
                g.drawString(n, x + T - w + 4, y + T - 4);
                if (u == active) {
                    g.setColor(Color.YELLOW);
                    g.setStroke(new BasicStroke(3));
                    g.drawRect(x + 1, y + 1, T - 3, T - 3);
                    g.setStroke(new BasicStroke(1));
                }
            }
            if (mine && hover != null) {
                Battle.Unit t = battle.unitAt(hover);
                Color c = (t != null && !t.player) ? (spellMode ? new Color(190, 90, 255) : new Color(255, 70, 70))
                        : (battle.reachable(active).contains(hover) && t == null ? Color.WHITE : null);
                if (c != null) {
                    g.setColor(c);
                    g.setStroke(new BasicStroke(3));
                    g.drawRect(ox + hover.getX() * T + 2, oy + hover.getY() * T + 2, T - 5, T - 5);
                    g.setStroke(new BasicStroke(1));
                }
            }
            if (active != null) {
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 15));
                g.drawString("Раунд " + battle.round() + " — ходит: " + (active.player ? "" : "враг, ")
                        + active.type.title + " ×" + active.count(), ox, 26);
            }
        }
    }
}
