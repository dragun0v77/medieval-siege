package com.dungeonanddragons.game;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Пиксельные спрайты 12x12, нарисованные прямо в коде строками.
 * Каждая буква — цвет из палитры, точка — прозрачность. В окне спрайты увеличиваются без сглаживания.
 */
final class Sprites {

    static final int SIZE = 12;
    private static final Map<Character, Integer> PAL = new HashMap<>();

    static {
        String[] p = {"k=14121a", "w=f0f0f0", "l=b4b8c4", "d=5a5e6e", "g=4fa84a", "G=2f7a35", "r=c0392b",
                "b=3a6fd0", "y=f2c230", "o=f08a24", "n=8a5a2e", "s=f0c8a0", "e=d9c08a", "p=8a4fc4"};
        for (String s : p) PAL.put(s.charAt(0), Integer.parseInt(s.substring(2), 16));
    }

    /** Строки спрайта + необязательная замена цветов вида "g=3d7a2e,n=7a6a5a". */
    private static BufferedImage make(String[] rows, String overrides) {
        Map<Character, Integer> pal = new HashMap<>(PAL);
        if (!overrides.isEmpty()) {
            for (String s : overrides.split(",")) pal.put(s.charAt(0), Integer.parseInt(s.substring(2), 16));
        }
        if (rows.length != SIZE) throw new IllegalArgumentException("Спрайт должен быть 12 строк: " + rows.length);
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            if (rows[y].length() != SIZE) {
                throw new IllegalArgumentException("Строка " + y + " не 12 символов: «" + rows[y] + "»");
            }
            for (int x = 0; x < SIZE; x++) {
                char c = rows[y].charAt(x);
                if (c == '.') continue;
                Integer rgb = pal.get(c);
                if (rgb == null) throw new IllegalArgumentException("Нет цвета для «" + c + "»");
                img.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        return img;
    }

    static final BufferedImage HERO = make(new String[]{
            "....dddd....",
            "...dllllld..",
            "...dlkkkld..",
            "....dllld..w",
            "..bbbbbbbb.w",
            ".sbbbyybbbsw",
            ".sbbbbbbbbsw",
            "..bbbbbbbb.n",
            "..bbbddbbb..",
            "..bbb..bbb..",
            "..nnn..nnn..",
            "..nnn..nnn.."}, "");

    private static final BufferedImage GOBLIN = make(new String[]{
            "............",
            ".g.gggggg.g.",
            ".gggggggggg.",
            "..gwkggkwg..",
            "..gggrrggg..",
            "...gggggg...",
            "..nnnnnnnn..",
            ".gnnnnnnnng.",
            ".g.nnnnnn.g.",
            "...nn..nn...",
            "...gg..gg...",
            "..gg....gg.."}, "");

    private static final BufferedImage SKELETON = make(new String[]{
            "...wwwwww...",
            "..wwwwwwww..",
            "..wkkwwkkw..",
            "..wkkwwkkw..",
            "...wwwwww...",
            "....wkkw....",
            "..wwlwwlww..",
            "w.wlwwwwlw.w",
            "w..wlwwlw..w",
            "...wwwwww...",
            "...w.ww.w...",
            "..ww....ww.."}, "");

    private static final BufferedImage ORC = make(new String[]{
            "...dddddd...",
            "..dgggggggd.",
            "..ggwkggkwg.",
            "..ggggggggg.",
            "..gwggggwgg.",
            ".dddddddddd.",
            "gddrrddrrddg",
            "gddddddddddg",
            ".gddddddddg.",
            "..ddd..ddd..",
            "..ggg..ggg..",
            ".nnnn..nnnn."}, "g=3d7a2e");

    private static final BufferedImage TROLL = make(new String[]{
            "...nnnnnn...",
            "..nnnnnnnn..",
            "..nyknnkyn..",
            "..nnnnnnnn..",
            "..nwnwwnwn..",
            ".nnnnnnnnnn.",
            "nnnnnnnnnnnn",
            "nnndddddnnnn",
            "nnnnnnnnnnnn",
            ".nnn....nnn.",
            ".nnn....nnn.",
            ".kkk....kkk."}, "n=7a6a5a");

    private static final BufferedImage DRAGON = make(new String[]{
            "r..r....r..r",
            "rr.rrrrrr.rr",
            "rrrrykrykrrr",
            "rrrrrrrrrrrr",
            ".rrrrrrrrrr.",
            ".rrrooooorr.",
            "..rroooorr..",
            "..rroooorr..",
            "..rrrrrrrr..",
            "..rr....rr..",
            "..rr....rr..",
            ".rrr....rrr."}, "r=b02a2a");

    private static final BufferedImage WOLF = make(new String[]{
            "............",
            "............",
            "........d.d.",
            "........ddd.",
            ".ddddd.ddkdd",
            ".dddddddddwd",
            ".lllllllddd.",
            ".dlllllld...",
            ".dddddddd...",
            ".d.d..d.d...",
            ".d.d..d.d...",
            ".k.k..k.k..."}, "");


    static final BufferedImage TREE = make(new String[]{
            "....gggg....",
            "..ggggGggg..",
            ".ggGgggggGg.",
            ".gggggGgggg.",
            "ggGgggggggGg",
            ".ggggGggggg.",
            "..gGgggggg..",
            "....nnnn....",
            "....nnnn....",
            "....nnnn....",
            "...nnnnnn...",
            "............"}, "");

    static final BufferedImage HOUSE = make(new String[]{
            "....rrrr....",
            "..rrrrrrrr..",
            ".rrrrrrrrrr.",
            "rrrrrrrrrrrr",
            ".eeeeeeeeee.",
            ".eeyyeeeeee.",
            ".eeyyeeddee.",
            ".eeeeeeddee.",
            ".eeeeeeddee.",
            ".eeeeeeddee.",
            ".kkkkkkkkkk.",
            "............"}, "");

    static final BufferedImage GATE = make(new String[]{
            "dddddddddddd",
            "dlllllllllld",
            "dlllkkkkllld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dddddddddddd"}, "");




    private static BufferedImage recolor(BufferedImage src, int from, int to) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int p = src.getRGB(x, y);
                img.setRGB(x, y, (p & 0xFFFFFF) == from ? (0xFF000000 | to) : p);
            }
        }
        return img;
    }

    private static final BufferedImage MILITIA = make(new String[]{
            "..l.dddd....",
            "..n.dllld...",
            "..n.dkkkd...",
            "..n..ddd....",
            "..n.nnnnnn..",
            "..nsnnyynns.",
            "..n.nnnnnn..",
            "..n.nnnnnn..",
            "..n.nnddnn..",
            "..n.nn..nn..",
            "..n.dd..dd..",
            "..n.kk..kk.."}, "");

    private static final BufferedImage ARCHER = make(new String[]{
            "...gggggg...",
            "..gggggggg..",
            "..gsskksg...",
            "..gsssssg...",
            "...gggggg...",
            "..ngggggg...",
            ".nwgggggg...",
            "nw.ggnngg...",
            ".nwgggggg...",
            "..ngg..gg...",
            "...nn..nn...",
            "...kk..kk..."}, "");

    private static final BufferedImage KNIGHT = recolor(HERO, 0x3a6fd0, 0xc0392b);
    private static final BufferedImage PALADIN = recolor(HERO, 0x3a6fd0, 0xe6e6f0);

    private static final BufferedImage TOWN = make(new String[]{
            "r.d.d..d.d.r",
            "dddddddddddd",
            "dllldllldlld",
            "dllldllldlld",
            "dddddddddddd",
            "dlllllllllld",
            "dlllkkkkllld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dllkkkkkklld",
            "dddddddddddd"}, "");

    private static final BufferedImage GOLD_MINE = make(new String[]{
            "............",
            "....dddd....",
            "...dddddd...",
            "..dddlldddd.",
            ".ddddddddddd",
            ".ddkkkkkdyd.",
            "dddkkkkkdddd",
            "ddykkkkkdydd",
            "dddkkkkkdddd",
            "dddddddddddd",
            "............",
            "............"}, "");

    private static final BufferedImage ORE_PIT = recolor(GOLD_MINE, 0xf2c230, 0xc8ccd8);
    private static final BufferedImage SAWMILL = recolor(recolor(HOUSE, 0xc0392b, 0x8a5a2e), 0xd9c08a, 0xa8793f);

    private static final BufferedImage WOOD_PILE = make(new String[]{
            "............", "............", "............", "............", "............", "............",
            "...nnnnnn...",
            "..nkknkknn..",
            ".nnnnnnnnnn.",
            ".nkknkknkkn.",
            ".nnnnnnnnnn.",
            "............"}, "");

    private static final BufferedImage ORE_PILE = make(new String[]{
            "............", "............", "............", "............", "............", "............",
            "....llll....",
            "..lllllllll.",
            ".llldlllldll",
            ".lldddlldddl",
            ".kkkkkkkkkk.",
            "............"}, "");

    private static final BufferedImage GOLD_PILE = make(new String[]{
            "............", "............", "............", "............", "............", "............",
            "....yyyy....",
            "..yyyyoyyyy.",
            ".yyoyyyyyoyy",
            ".yyyyyyyyyy.",
            ".kkkkkkkkkk.",
            "............"}, "");

    static final BufferedImage CHEST = make(new String[]{
            "............", "............", "............", "............",
            "..nnnnnnnn..",
            ".nnnnnnnnnn.",
            ".nyyyyyyyyn.",
            ".nnnnyynnnn.",
            ".nnnnyynnnn.",
            ".nnnnnnnnnn.",
            ".kkkkkkkkkk.",
            "............"}, "");

    static BufferedImage of(UnitType t) {
        return switch (t) {
            case MILITIA -> MILITIA;
            case ARCHER -> ARCHER;
            case KNIGHT -> KNIGHT;
            case PALADIN -> PALADIN;
            case GOBLIN -> GOBLIN;
            case WOLF -> WOLF;
            case SKELETON -> SKELETON;
            case ORC -> ORC;
            case TROLL -> TROLL;
            case DRAGON -> DRAGON;
        };
    }

    /** Спрайт объекта карты (для отрядов — спрайт существа). */
    static BufferedImage of(GameWorld.MapObject o) {
        return switch (o.kind) {
            case TOWN -> TOWN;
            case GOLD_MINE -> GOLD_MINE;
            case SAWMILL -> SAWMILL;
            case ORE_PIT -> ORE_PIT;
            case GOLD_PILE -> GOLD_PILE;
            case WOOD_PILE -> WOOD_PILE;
            case ORE_PILE -> ORE_PILE;
            case CHEST -> CHEST;
            case STACK -> of(o.unit);
        };
    }

    /** Рисует спрайт, увеличивая без сглаживания; flip — отзеркалить по горизонтали. */
    static void draw(java.awt.Graphics2D g, BufferedImage img, int x, int y, int size, boolean flip) {
        if (flip) g.drawImage(img, x + size, y, -size, size, null);
        else g.drawImage(img, x, y, size, size, null);
    }

    private Sprites() { }
}
