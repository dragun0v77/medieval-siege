package com.dungeonanddragons.world;

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

    private static final BufferedImage BANDIT = make(new String[]{
            "...nnnnnn...",
            "..nnnnnnnn..",
            "..nsssssn...",
            "..nskssksn..",
            "..nsssssn...",
            "..rrrrrrrr..",
            ".nrrrrrrrrn.",
            ".nrrrbbrrrn.",
            "..rrrrrrrr..",
            "..rrr..rrr..",
            "..ddd..ddd..",
            "..ddd..ddd.."}, "");

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

    static final BufferedImage CHEST = make(new String[]{
            "............",
            "............",
            "............",
            "............",
            "..nnnnnnnn..",
            ".nnnnnnnnnn.",
            ".nyyyyyyyyn.",
            ".nnnnyynnnn.",
            ".nnnnyynnnn.",
            ".nnnnnnnnnn.",
            ".kkkkkkkkkk.",
            "............"}, "");

    static final BufferedImage GRACE = make(new String[]{
            ".....yy.....",
            "....yyyy....",
            "...yyyyyy...",
            "...yyoyyy...",
            "....yyoy....",
            ".....yy.....",
            "....wwww....",
            "...wwwwww...",
            "..llllllll..",
            "..dddddddd..",
            "............",
            "............"}, "");

    private static final String[] NPC = {
            "....BBBB....",
            "...BBBBBB...",
            "..BBBBBBBB..",
            "...ssssss...",
            "...skssks...",
            "...wwwwww...",
            "..AAwwwwAA..",
            ".AAAAAAAAAA.",
            ".AAAAAAAAAA.",
            ".AAAAAAAAAA.",
            ".AAAAAAAAAA.",
            "..nn....nn.."};

    private static final BufferedImage ELDER = make(NPC, "A=8a4fc4,B=b4b8c4");
    private static final BufferedImage MERCHANT = make(NPC, "A=4fa84a,B=f2c230");
    private static final BufferedImage HEALER = make(NPC, "A=f0f0f0,B=c0392b");

    static BufferedImage of(Mob.Kind k) {
        return switch (k) {
            case WOLF -> WOLF;
            case BANDIT -> BANDIT;
            case GOBLIN -> GOBLIN;
            case SKELETON -> SKELETON;
            case ORC -> ORC;
            case TROLL -> TROLL;
            case DRAGON -> DRAGON;
        };
    }

    static BufferedImage of(Prop.Kind k) {
        return switch (k) {
            case CHEST -> CHEST;
            case GRACE -> GRACE;
            case ELDER -> ELDER;
            case MERCHANT -> MERCHANT;
            case HEALER -> HEALER;
        };
    }

    private Sprites() { }
}
