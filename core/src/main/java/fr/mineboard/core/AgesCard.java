package fr.mineboard.core;

/** Original city-age cards. Mechanical cousin of a 3-age draft, not a licensed reprint. */
final class AgesCard {
    static final int BROWN = 0, GREY = 1, YELLOW = 2, BLUE = 3, GREEN = 4, RED = 5, PURPLE = 6;
    static final int W = 1, S = 2, C = 4, O = 8, G = 16, P = 32, T = 64;
    static final int[] RES = { W, S, C, O, G, P, T };
    static final int WEST = 1, EAST = 2, GREY_TRADE = 3, VINEYARD = 4, BAZAAR = 5, HAVEN = 6,
        LIGHTHOUSE = 7, ARENA = 8, TAVERN = 9, EXTRA = 10, WILD_SCI = 11,
        GUILD_BROWN = 12, GUILD_GREY = 13, GUILD_BLUE = 14, GUILD_YELLOW = 15,
        GUILD_RED = 16, GUILD_GREEN = 17, GUILD_WONDER = 18, GUILD_LEADERS = 19,
        ANY_GREY = 20, ANY_RAW = 21, FREE_AGE = 22, DISCARD_BUILD = 23, CHAMBER = 24,
        GUILD_DEFEAT = 25;
    static final int HAND = 1000;

    final int id, age, type, min, cost, costCoins, prodRes, prodCount;
    final boolean prodOr;
    final int coins, vp, military, science, chainFrom, chainTo, special;
    final String key;

    private AgesCard(int id, int age, int type, int min, String key, int cost, int costCoins,
                     int prodRes, int prodCount, boolean prodOr, int coins, int vp, int military,
                     int science, int chainFrom, int chainTo, int special) {
        this.id = id; this.age = age; this.type = type; this.min = min; this.key = key;
        this.cost = cost; this.costCoins = costCoins; this.prodRes = prodRes;
        this.prodCount = prodCount; this.prodOr = prodOr; this.coins = coins; this.vp = vp;
        this.military = military; this.science = science; this.chainFrom = chainFrom;
        this.chainTo = chainTo; this.special = special;
    }

    int handId() { return HAND + id; }
    static boolean isHand(int value) { return value >= HAND; }
    static AgesCard byId(int id) {
        for (AgesCard card : ALL) if (card.id == id) return card;
        throw new IllegalArgumentException("ages " + id);
    }

    static int pack(int bit, int count) {
        return count << (Integer.numberOfTrailingZeros(bit) * 4);
    }
    static int qty(int packed, int bit) {
        return (packed >> (Integer.numberOfTrailingZeros(bit) * 4)) & 15;
    }

    static int nonGuild(int age, int players) {
        int count = 0;
        for (AgesCard card : ALL) {
            if (card.age != age || card.min > players || card.type == PURPLE) continue;
            count++;
        }
        return count;
    }
    static int guilds() {
        int count = 0;
        for (AgesCard card : ALL) if (card.type == PURPLE) count++;
        return count;
    }

    private static int next;
    private static AgesCard add(int age, int type, int min, String key, int cost, int costCoins,
                                int prodRes, int prodCount, boolean prodOr, int coins, int vp,
                                int military, int science, int chainFrom, int chainTo, int special) {
        return new AgesCard(next++, age, type, min, key, cost, costCoins, prodRes, prodCount,
            prodOr, coins, vp, military, science, chainFrom, chainTo, special);
    }
    private static int c(int a, int b) { return a | b; }
    private static int c(int a, int b, int d) { return a | b | d; }
    private static int c(int a, int b, int d, int e) { return a | b | d | e; }

    static final AgesCard[] ALL = {
        add(1, BROWN, 3, "timber", 0, 0, W, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 4, "timber", 0, 0, W, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 3, "stonepit", 0, 0, S, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 5, "stonepit", 0, 0, S, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 3, "claybed", 0, 0, C, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 5, "claybed", 0, 0, C, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 3, "vein", 0, 0, O, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 4, "vein", 0, 0, O, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 4, "dig", 0, 0, S | C, 1, true, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 3, "pit", 0, 0, C | O, 1, true, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 3, "yard", 0, 0, S | W, 1, true, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 5, "cave", 0, 0, W | O, 1, true, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 6, "copse", 0, 0, W | C, 1, true, 0, 0, 0, 0, 0, 0, 0),
        add(1, BROWN, 6, "mine", 0, 0, O | S, 1, true, 0, 0, 0, 0, 0, 0, 0),
        add(1, GREY, 3, "glassworks", 0, 0, G, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, GREY, 6, "glassworks", 0, 0, G, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, GREY, 3, "press", 0, 0, P, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, GREY, 6, "press", 0, 0, P, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, GREY, 3, "loom", 0, 0, T, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, GREY, 6, "loom", 0, 0, T, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(1, BLUE, 3, "altar", 0, 0, 0, 0, false, 0, 2, 0, 0, 0, 1, 0),
        add(1, BLUE, 5, "altar", 0, 0, 0, 0, false, 0, 2, 0, 0, 0, 1, 0),
        add(1, BLUE, 3, "theatre", 0, 0, 0, 0, false, 0, 2, 0, 0, 0, 2, 0),
        add(1, BLUE, 6, "theatre", 0, 0, 0, 0, false, 0, 2, 0, 0, 0, 2, 0),
        add(1, BLUE, 3, "baths", pack(S, 1), 0, 0, 0, false, 0, 3, 0, 0, 0, 3, 0),
        add(1, BLUE, 7, "baths", pack(S, 1), 0, 0, 0, false, 0, 3, 0, 0, 0, 3, 0),
        add(1, BLUE, 4, "well", 0, 0, 0, 0, false, 0, 3, 0, 0, 0, 0, 0),
        add(1, BLUE, 7, "well", 0, 0, 0, 0, false, 0, 3, 0, 0, 0, 0, 0),
        add(1, RED, 3, "stockade", pack(W, 1), 0, 0, 0, false, 0, 0, 1, 0, 0, 0, 0),
        add(1, RED, 7, "stockade", pack(W, 1), 0, 0, 0, false, 0, 0, 1, 0, 0, 0, 0),
        add(1, RED, 3, "barracks", pack(O, 1), 0, 0, 0, false, 0, 0, 1, 0, 0, 0, 0),
        add(1, RED, 5, "barracks", pack(O, 1), 0, 0, 0, false, 0, 0, 1, 0, 0, 0, 0),
        add(1, RED, 7, "barracks", pack(O, 1), 0, 0, 0, false, 0, 0, 1, 0, 0, 0, 0),
        add(1, RED, 3, "guard", pack(C, 1), 0, 0, 0, false, 0, 0, 1, 0, 0, 0, 0),
        add(1, RED, 4, "guard", pack(C, 1), 0, 0, 0, false, 0, 0, 1, 0, 0, 0, 0),
        add(1, RED, 7, "guard", pack(C, 1), 0, 0, 0, false, 0, 0, 1, 0, 0, 0, 0),
        add(1, GREEN, 3, "scriptorium", pack(P, 1), 0, 0, 0, false, 0, 0, 0, 1, 0, 5, 0),
        add(1, GREEN, 4, "scriptorium", pack(P, 1), 0, 0, 0, false, 0, 0, 0, 1, 0, 5, 0),
        add(1, GREEN, 3, "workshop", pack(G, 1), 0, 0, 0, false, 0, 0, 0, 2, 0, 6, 0),
        add(1, GREEN, 7, "workshop", pack(G, 1), 0, 0, 0, false, 0, 0, 0, 2, 0, 6, 0),
        add(1, GREEN, 3, "apothecary", pack(T, 1), 0, 0, 0, false, 0, 0, 0, 4, 0, 7, 0),
        add(1, GREEN, 5, "apothecary", pack(T, 1), 0, 0, 0, false, 0, 0, 0, 4, 0, 7, 0),
        add(1, YELLOW, 4, "tavern", 0, 0, 0, 0, false, 5, 0, 0, 0, 0, 0, TAVERN),
        add(1, YELLOW, 5, "tavern", 0, 0, 0, 0, false, 5, 0, 0, 0, 0, 0, TAVERN),
        add(1, YELLOW, 7, "tavern", 0, 0, 0, 0, false, 5, 0, 0, 0, 0, 0, TAVERN),
        add(1, YELLOW, 3, "westpost", 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 8, WEST),
        add(1, YELLOW, 3, "eastpost", 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 8, EAST),
        add(1, YELLOW, 3, "market", 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 18, GREY_TRADE),
        add(1, YELLOW, 6, "market", 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 18, GREY_TRADE),

        add(2, BROWN, 3, "sawmill", 0, 1, W, 2, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, BROWN, 4, "sawmill", 0, 1, W, 2, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, BROWN, 3, "quarry", 0, 1, S, 2, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, BROWN, 4, "quarry", 0, 1, S, 2, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, BROWN, 3, "brickyard", 0, 1, C, 2, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, BROWN, 4, "brickyard", 0, 1, C, 2, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, BROWN, 3, "foundry", 0, 1, O, 2, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, BROWN, 4, "foundry", 0, 1, O, 2, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, GREY, 3, "glassblower", 0, 1, G, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, GREY, 5, "glassblower", 0, 1, G, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, GREY, 3, "drying", 0, 1, P, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, GREY, 5, "drying", 0, 1, P, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, GREY, 3, "weaving", 0, 1, T, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, GREY, 5, "weaving", 0, 1, T, 1, false, 0, 0, 0, 0, 0, 0, 0),
        add(2, BLUE, 3, "temple", c(pack(W, 1), pack(C, 1), pack(G, 1)), 0, 0, 0, false, 0, 3, 0, 0, 1, 9, 0),
        add(2, BLUE, 6, "temple", c(pack(W, 1), pack(C, 1), pack(G, 1)), 0, 0, 0, false, 0, 3, 0, 0, 1, 9, 0),
        add(2, BLUE, 3, "statue", c(pack(W, 1), pack(O, 2)), 0, 0, 0, false, 0, 4, 0, 0, 2, 10, 0),
        add(2, BLUE, 7, "statue", c(pack(W, 1), pack(O, 2)), 0, 0, 0, false, 0, 4, 0, 0, 2, 10, 0),
        add(2, BLUE, 3, "courthouse", c(pack(C, 2), pack(T, 1)), 0, 0, 0, false, 0, 4, 0, 0, 5, 0, 0),
        add(2, BLUE, 5, "courthouse", c(pack(C, 2), pack(T, 1)), 0, 0, 0, false, 0, 4, 0, 0, 5, 0, 0),
        add(2, BLUE, 3, "aqueduct", pack(S, 3), 0, 0, 0, false, 0, 5, 0, 0, 3, 0, 0),
        add(2, BLUE, 7, "aqueduct", pack(S, 3), 0, 0, 0, false, 0, 5, 0, 0, 3, 0, 0),
        add(2, RED, 3, "walls", pack(S, 3), 0, 0, 0, false, 0, 0, 2, 0, 0, 11, 0),
        add(2, RED, 7, "walls", pack(S, 3), 0, 0, 0, false, 0, 0, 2, 0, 0, 11, 0),
        add(2, RED, 4, "drill", c(pack(W, 1), pack(O, 2)), 0, 0, 0, false, 0, 0, 2, 0, 0, 15, 0),
        add(2, RED, 6, "drill", c(pack(W, 1), pack(O, 2)), 0, 0, 0, false, 0, 0, 2, 0, 0, 15, 0),
        add(2, RED, 7, "drill", c(pack(W, 1), pack(O, 2)), 0, 0, 0, false, 0, 0, 2, 0, 0, 15, 0),
        add(2, RED, 3, "archery", c(pack(W, 2), pack(O, 1)), 0, 0, 0, false, 0, 0, 2, 0, 0, 0, 0),
        add(2, RED, 6, "archery", c(pack(W, 2), pack(O, 1)), 0, 0, 0, false, 0, 0, 2, 0, 0, 0, 0),
        add(2, RED, 3, "stables", c(pack(W, 1), pack(C, 1), pack(O, 1)), 0, 0, 0, false, 0, 0, 2, 0, 7, 0, 0),
        add(2, RED, 5, "stables", c(pack(W, 1), pack(C, 1), pack(O, 1)), 0, 0, 0, false, 0, 0, 2, 0, 7, 0, 0),
        add(2, GREEN, 3, "library", c(pack(S, 2), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 1, 5, 12, 0),
        add(2, GREEN, 6, "library", c(pack(S, 2), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 1, 5, 12, 0),
        add(2, GREEN, 3, "laboratory", c(pack(C, 2), pack(P, 1)), 0, 0, 0, false, 0, 0, 0, 2, 6, 13, 0),
        add(2, GREEN, 5, "laboratory", c(pack(C, 2), pack(P, 1)), 0, 0, 0, false, 0, 0, 0, 2, 6, 13, 0),
        add(2, GREEN, 3, "dispensary", c(pack(O, 2), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 4, 7, 14, 0),
        add(2, GREEN, 4, "dispensary", c(pack(O, 2), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 4, 7, 14, 0),
        add(2, GREEN, 3, "school", c(pack(W, 1), pack(P, 1)), 0, 0, 0, false, 0, 0, 0, 1, 0, 0, 0),
        add(2, GREEN, 7, "school", c(pack(W, 1), pack(P, 1)), 0, 0, 0, false, 0, 0, 0, 1, 0, 0, 0),
        add(2, YELLOW, 3, "forum", pack(C, 2), 0, 0, 0, false, 0, 0, 0, 0, 8, 0, ANY_GREY),
        add(2, YELLOW, 6, "forum", pack(C, 2), 0, 0, 0, false, 0, 0, 0, 0, 8, 0, ANY_GREY),
        add(2, YELLOW, 7, "forum", pack(C, 2), 0, 0, 0, false, 0, 0, 0, 0, 8, 0, ANY_GREY),
        add(2, YELLOW, 3, "caravansery", pack(W, 2), 0, 0, 0, false, 0, 0, 0, 0, 18, 0, ANY_RAW),
        add(2, YELLOW, 5, "caravansery", pack(W, 2), 0, 0, 0, false, 0, 0, 0, 0, 18, 0, ANY_RAW),
        add(2, YELLOW, 6, "caravansery", pack(W, 2), 0, 0, 0, false, 0, 0, 0, 0, 18, 0, ANY_RAW),
        add(2, YELLOW, 3, "vineyard", 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 0, VINEYARD),
        add(2, YELLOW, 6, "vineyard", 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 0, VINEYARD),
        add(2, YELLOW, 4, "bazaar", 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 0, BAZAAR),
        add(2, YELLOW, 7, "bazaar", 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 0, BAZAAR),

        add(3, BLUE, 3, "pantheon", c(c(pack(C, 1), pack(O, 1), pack(P, 1)), c(pack(T, 1), pack(G, 1))), 0, 0, 0, false, 0, 7, 0, 0, 9, 0, 0),
        add(3, BLUE, 6, "pantheon", c(c(pack(C, 1), pack(O, 1), pack(P, 1)), c(pack(T, 1), pack(G, 1))), 0, 0, 0, false, 0, 7, 0, 0, 9, 0, 0),
        add(3, BLUE, 3, "gardens", c(pack(W, 2), pack(C, 2)), 0, 0, 0, false, 0, 5, 0, 0, 10, 0, 0),
        add(3, BLUE, 4, "gardens", c(pack(W, 2), pack(C, 2)), 0, 0, 0, false, 0, 5, 0, 0, 10, 0, 0),
        add(3, BLUE, 3, "senate", c(pack(W, 2), pack(S, 1), pack(O, 1)), 0, 0, 0, false, 0, 6, 0, 0, 12, 0, 0),
        add(3, BLUE, 5, "senate", c(pack(W, 2), pack(S, 1), pack(O, 1)), 0, 0, 0, false, 0, 6, 0, 0, 12, 0, 0),
        add(3, BLUE, 3, "townhall", c(pack(S, 2), pack(O, 1), pack(G, 1)), 0, 0, 0, false, 0, 6, 0, 0, 0, 0, 0),
        add(3, BLUE, 5, "townhall", c(pack(S, 2), pack(O, 1), pack(G, 1)), 0, 0, 0, false, 0, 6, 0, 0, 0, 0, 0),
        add(3, BLUE, 6, "townhall", c(pack(S, 2), pack(O, 1), pack(G, 1)), 0, 0, 0, false, 0, 6, 0, 0, 0, 0, 0),
        add(3, BLUE, 3, "palace", c(c(pack(W, 1), pack(S, 1), pack(C, 1)), c(pack(O, 1), pack(G, 1), pack(P, 1))) | pack(T, 1), 0, 0, 0, false, 0, 8, 0, 0, 0, 0, 0),
        add(3, BLUE, 7, "palace", c(c(pack(W, 1), pack(S, 1), pack(C, 1)), c(pack(O, 1), pack(G, 1), pack(P, 1))) | pack(T, 1), 0, 0, 0, false, 0, 8, 0, 0, 0, 0, 0),
        add(3, RED, 3, "fort", c(pack(S, 3), pack(O, 1)), 0, 0, 0, false, 0, 0, 3, 0, 11, 0, 0),
        add(3, RED, 7, "fort", c(pack(S, 3), pack(O, 1)), 0, 0, 0, false, 0, 0, 3, 0, 11, 0, 0),
        add(3, RED, 4, "circus", c(pack(S, 2), pack(O, 2)), 0, 0, 0, false, 0, 0, 3, 0, 15, 0, 0),
        add(3, RED, 5, "circus", c(pack(S, 2), pack(O, 2)), 0, 0, 0, false, 0, 0, 3, 0, 15, 0, 0),
        add(3, RED, 6, "circus", c(pack(S, 2), pack(O, 2)), 0, 0, 0, false, 0, 0, 3, 0, 15, 0, 0),
        add(3, RED, 3, "arsenal", c(pack(W, 3), pack(O, 2)), 0, 0, 0, false, 0, 0, 3, 0, 0, 0, 0),
        add(3, RED, 4, "arsenal", c(pack(W, 3), pack(O, 2)), 0, 0, 0, false, 0, 0, 3, 0, 0, 0, 0),
        add(3, RED, 7, "arsenal", c(pack(W, 3), pack(O, 2)), 0, 0, 0, false, 0, 0, 3, 0, 0, 0, 0),
        add(3, RED, 3, "siege", c(pack(W, 2), pack(C, 3)), 0, 0, 0, false, 0, 0, 3, 0, 13, 0, 0),
        add(3, RED, 5, "siege", c(pack(W, 2), pack(C, 3)), 0, 0, 0, false, 0, 0, 3, 0, 13, 0, 0),
        add(3, GREEN, 3, "university", c(pack(W, 2), pack(P, 1), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 1, 12, 0, 0),
        add(3, GREEN, 4, "university", c(pack(W, 2), pack(P, 1), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 1, 12, 0, 0),
        add(3, GREEN, 3, "observatory", c(pack(O, 2), pack(G, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 2, 13, 0, 0),
        add(3, GREEN, 7, "observatory", c(pack(O, 2), pack(G, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 2, 13, 0, 0),
        add(3, GREEN, 3, "lodge", c(pack(C, 2), pack(P, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 4, 14, 0, 0),
        add(3, GREEN, 6, "lodge", c(pack(C, 2), pack(P, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 4, 14, 0, 0),
        add(3, GREEN, 3, "academy", c(pack(S, 3), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 4, 0, 0, 0),
        add(3, GREEN, 7, "academy", c(pack(S, 3), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 4, 0, 0, 0),
        add(3, GREEN, 3, "study", c(pack(W, 1), pack(P, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 2, 0, 0, 0),
        add(3, GREEN, 5, "study", c(pack(W, 1), pack(P, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 2, 0, 0, 0),
        add(3, YELLOW, 3, "haven", c(pack(W, 1), pack(O, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, HAVEN),
        add(3, YELLOW, 4, "haven", c(pack(W, 1), pack(O, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, HAVEN),
        add(3, YELLOW, 3, "lighthouse", c(pack(S, 1), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, LIGHTHOUSE),
        add(3, YELLOW, 6, "lighthouse", c(pack(S, 1), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, LIGHTHOUSE),
        add(3, YELLOW, 4, "chamber", c(pack(C, 2), pack(P, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, CHAMBER),
        add(3, YELLOW, 6, "chamber", c(pack(C, 2), pack(P, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, CHAMBER),
        add(3, YELLOW, 3, "arena", c(pack(O, 2), pack(S, 1)), 0, 0, 0, false, 0, 0, 0, 0, 14, 0, ARENA),
        add(3, YELLOW, 5, "arena", c(pack(O, 2), pack(S, 1)), 0, 0, 0, false, 0, 0, 0, 0, 14, 0, ARENA),
        add(3, YELLOW, 7, "arena", c(pack(O, 2), pack(S, 1)), 0, 0, 0, false, 0, 0, 0, 0, 14, 0, ARENA),
        add(3, PURPLE, 3, "workers", c(pack(W, 2), pack(O, 1), pack(S, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, GUILD_BROWN),
        add(3, PURPLE, 3, "craft", c(pack(O, 2), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, GUILD_GREY),
        add(3, PURPLE, 3, "traders", c(pack(W, 1), pack(P, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, GUILD_YELLOW),
        add(3, PURPLE, 3, "spies", c(pack(C, 3), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, GUILD_RED),
        add(3, PURPLE, 3, "philosophers", c(pack(T, 2), pack(P, 2)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, GUILD_GREEN),
        add(3, PURPLE, 3, "builders", c(c(pack(S, 2), pack(C, 2)), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, GUILD_WONDER),
        add(3, PURPLE, 3, "scientists", c(pack(W, 2), pack(O, 2)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, WILD_SCI),
        add(3, PURPLE, 3, "magistrates", c(pack(W, 2), pack(S, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, GUILD_BLUE),
        add(3, PURPLE, 3, "shipowners", c(pack(W, 1), pack(P, 1), pack(G, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, GUILD_LEADERS),
        add(3, PURPLE, 3, "strategists", c(pack(O, 2), pack(S, 1), pack(T, 1)), 0, 0, 0, false, 0, 0, 0, 0, 0, 0, GUILD_DEFEAT)
    };
}
