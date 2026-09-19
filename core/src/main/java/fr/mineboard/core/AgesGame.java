package fr.mineboard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Simultaneous 3-age city draft for 3–7 seats: boards, coins, commerce, science and war.
 */
final class AgesGame implements Game {
    private static final int[] WAR = { 0, 1, 3, 5 };
    private static final int[] WONDER_RES = { AgesCard.G, AgesCard.P, AgesCard.O, AgesCard.S, AgesCard.C, AgesCard.W, AgesCard.T };
    private static final int[][] STAGE_COST = {
        { AgesCard.pack(AgesCard.S, 2), AgesCard.pack(AgesCard.O, 2), AgesCard.pack(AgesCard.G, 2) },
        { AgesCard.pack(AgesCard.S, 2), AgesCard.pack(AgesCard.W, 2), AgesCard.pack(AgesCard.P, 2) },
        { AgesCard.pack(AgesCard.W, 2), AgesCard.pack(AgesCard.C, 3), AgesCard.pack(AgesCard.O, 4) },
        { AgesCard.pack(AgesCard.S, 2), AgesCard.pack(AgesCard.W, 3), AgesCard.pack(AgesCard.S, 4) },
        { AgesCard.pack(AgesCard.C, 2), AgesCard.pack(AgesCard.W, 3), AgesCard.pack(AgesCard.C, 4) },
        { AgesCard.pack(AgesCard.W, 2), AgesCard.pack(AgesCard.S, 2), AgesCard.pack(AgesCard.O, 2) },
        { AgesCard.pack(AgesCard.O, 2), AgesCard.pack(AgesCard.C, 3), AgesCard.pack(AgesCard.T, 2) }
    };
    private static final int[][] STAGE_VP = { {3, 0, 7}, {3, 0, 7}, {3, 0, 7}, {3, 5, 7}, {3, 0, 7}, {3, 0, 7}, {3, 0, 7} };
    private static final int[][] STAGE_COINS = { {0, 0, 0}, {0, 9, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0} };
    private static final int[][] STAGE_MIL = { {0, 0, 0}, {0, 0, 0}, {0, 2, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0} };
    private static final int[][] STAGE_SPEC = {
        { 0, AgesCard.ANY_RAW, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 },
        { 0, AgesCard.WILD_SCI, 0 }, { 0, AgesCard.FREE_AGE, 0 }, { 0, AgesCard.DISCARD_BUILD, 0 }
    };

    private static final class Held {
        final AgesCard card;
        final int uid;
        Held(AgesCard card, int uid) { this.card = card; this.uid = uid; }
    }

    private final List<List<Held>> hands = new ArrayList<>();
    private final List<List<Held>> cities = new ArrayList<>();
    private final List<Held> discarded = new ArrayList<>();
    private int[] coins = new int[0], shields = new int[0], war = new int[0], defeats = new int[0];
    private int[] wonder = new int[0], stage = new int[0], pick = new int[0], mode = new int[0];
    private boolean[] freeUsed = new boolean[0];
    private int age, passDir, salvageSeat = -1, nextUid = 1000;
    private long roundRevision;
    private Random random;

    @Override public String id() { return Games.WONDERS; }
    @Override public int seats() { return 7; }
    @Override public int minSeats() { return 3; }
    @Override public int maxSeats() { return 7; }
    @Override public String countKey() { return "coins"; }
    @Override public String hintKey() { return "lobby.hint.wonders"; }
    @Override public String controlsKey() { return "controls.wonders"; }
    @Override public String startKey() { return "start.wonders"; }
    @Override public int startingTurn() { return -1; }
    @Override public double boardSpan() { return 2.4; }
    @Override public void clear() {
        hands.clear();
        cities.clear();
        discarded.clear();
        coins = shields = war = defeats = wonder = stage = pick = mode = new int[0];
        freeUsed = new boolean[0];
        age = passDir = 0;
        salvageSeat = -1;
        nextUid = 1000;
        roundRevision = 0;
    }
    @Override public void start(TableSession session, Random random) {
        clear();
        this.random = random;
        int n = session.seatCount();
        coins = fill(n, 3);
        shields = fill(n, 0);
        war = fill(n, 0);
        defeats = fill(n, 0);
        stage = fill(n, 0);
        pick = fill(n, -1);
        mode = fill(n, 0);
        freeUsed = new boolean[n];
        wonder = new int[n];
        List<Integer> boards = new ArrayList<>();
        for (int i = 0; i < WONDER_RES.length; i++) boards.add(i);
        Collections.shuffle(boards, random);
        for (int i = 0; i < n; i++) {
            wonder[i] = boards.get(i);
            hands.add(new ArrayList<>());
            cities.add(new ArrayList<>());
        }
        dealAge(session, random, 1);
    }
    @Override public String apply(TableSession session, UUID player, String type, int target, long revision) {
        String error = session.validateTurn(player, -1);
        if (!error.isEmpty()) return error;
        int seat = session.seatOf(player);
        if (salvageSeat >= 0) {
            if (seat != salvageSeat || (!type.equals("salvage") && !type.equals("skip"))) return "not_your_turn";
            if (revision >= 0 && revision != session.revision()) return "stale";
            if (type.equals("salvage") && target < 0) return "invalid_card";
            return salvage(session, type.equals("skip") ? -1 : target);
        }
        if (!List.of("play", "discard", "wonder").contains(type)) return "invalid_action";
        if (revision >= 0 && (revision < roundRevision || revision > session.revision())) return "stale";
        if (pick[seat] >= 0) return "already_started";
        if (target < 0 || target >= hands.get(seat).size()) return "invalid_card";
        Held held = hands.get(seat).get(target);
        int action = switch (type) {
            case "discard" -> 1;
            case "wonder" -> 2;
            default -> 0;
        };
        if (action == 0 && !canPlay(seat, held.card)) return "cannot_play";
        if (action == 2 && (stage[seat] >= 3 || pay(seat, STAGE_COST[wonder[seat]][stage[seat]], 0, false) < 0)) {
            return "cannot_play";
        }
        pick[seat] = target;
        mode[seat] = action;
        if (!allPicked()) {
            session.changed("picked", seat);
            return "";
        }
        resolveRound(session);
        return "";
    }
    @Override public void populate(TableSession session, ViewBuild view, UUID recipient) {
        int n = Math.max(1, session.seatCount());
        int[] counts = new int[n];
        for (int i = 0; i < n && i < coins.length; i++) counts[i] = session.phase() == TableGame.Phase.FINISHED ? score(i, n) : coins[i];
        view.counts = counts;
        view.topCard = age;
        view.deckCount = hands.isEmpty() ? 0 : hands.get(0).size();
        int seat = recipient == null ? -1 : session.seatOf(recipient);
        List<Integer> playable = new ArrayList<>();
        List<TableGame.Move> moves = new ArrayList<>();
        List<String> buttons = new ArrayList<>();
        if (session.phase() == TableGame.Phase.PLAYING && seat >= 0 && salvageSeat == seat) {
            view.hand = discarded.stream().map(held -> held.card.handId()).toList();
            for (int i = 0; i < discarded.size(); i++) {
                if (!owns(seat, discarded.get(i).card)) {
                    playable.add(i);
                    moves.add(new TableGame.Move("salvage", i));
                    moves.add(new TableGame.Move("salvage", discarded.get(i).uid));
                }
            }
            if (!playable.isEmpty()) buttons.add("salvage");
            buttons.add("skip");
        } else {
            List<Held> hand = seat < 0 || seat >= hands.size() ? List.of() : hands.get(seat);
            view.hand = hand.stream().map(held -> held.card.handId()).toList();
            if (session.phase() == TableGame.Phase.PLAYING && seat >= 0 && pick[seat] < 0 && salvageSeat < 0) {
                for (int i = 0; i < hand.size(); i++) {
                    moves.add(new TableGame.Move("discard", i));
                    if (canPlay(seat, hand.get(i).card)) {
                        playable.add(i);
                        moves.add(new TableGame.Move("play", i));
                    }
                    if (stage[seat] < 3 && pay(seat, STAGE_COST[wonder[seat]][stage[seat]], 0, false) >= 0) {
                        moves.add(new TableGame.Move("wonder", i));
                    }
                }
                if (!playable.isEmpty()) buttons.add("play");
                buttons.add("discard");
                if (stage[seat] < 3) buttons.add("wonder");
            }
        }
        if (session.phase() == TableGame.Phase.FINISHED) buttons.add("rematch");
        view.playable = playable;
        view.moves = moves;
        view.buttons = buttons;
        view.pieces = layout(session);
    }

    private void dealAge(TableSession session, Random random, int nextAge) {
        age = nextAge;
        passDir = age == 2 ? -1 : 1;
        java.util.Arrays.fill(freeUsed, false);
        salvageSeat = -1;
        int n = session.seatCount();
        List<AgesCard> deck = new ArrayList<>();
        List<AgesCard> guilds = new ArrayList<>();
        for (AgesCard card : AgesCard.ALL) {
            if (card.age != age || card.min > n) continue;
            if (card.type == AgesCard.PURPLE) guilds.add(card);
            else deck.add(card);
        }
        if (age == 3) {
            Collections.shuffle(guilds, random);
            int take = Math.min(guilds.size(), n + 2);
            deck.addAll(guilds.subList(0, take));
        }
        Collections.shuffle(deck, random);
        while (deck.size() < n * 7) {
            for (AgesCard card : AgesCard.ALL) if (card.age == 1 && card.type == AgesCard.BROWN && !card.prodOr) {
                deck.add(card);
                if (deck.size() >= n * 7) break;
            }
        }
        for (int i = 0; i < n; i++) {
            hands.get(i).clear();
            for (int c = 0; c < 7; c++) hands.get(i).add(held(deck.get(i * 7 + c)));
            pick[i] = -1;
            mode[i] = 0;
        }
        if (session.phase() == TableGame.Phase.PLAYING) session.changed("age", 0);
        roundRevision = session.revision() + (session.phase() == TableGame.Phase.LOBBY ? 1 : 0);
    }
    private Held held(AgesCard card) { return new Held(card, nextUid++); }
    private boolean allPicked() {
        for (int value : pick) if (value < 0) return false;
        return pick.length > 0;
    }
    private void resolveRound(TableSession session) {
        int n = session.seatCount();
        Held[] chosen = new Held[n];
        for (int i = 0; i < n; i++) chosen[i] = hands.get(i).remove(pick[i]);
        Payment[] payments = new Payment[n];
        for (int i = 0; i < n; i++) {
            if (mode[i] == 1) continue;
            AgesCard card = chosen[i].card;
            payments[i] = mode[i] == 2 ? payment(i, STAGE_COST[wonder[i]][stage[i]], 0)
                : chained(i, card) ? new Payment(0, 0, 0) : payment(i, card.cost, card.costCoins);
            if (payments[i] == null && mode[i] == 0 && unusedFree(i)) {
                freeUsed[i] = true;
                payments[i] = new Payment(0, 0, 0);
            }
            if (payments[i] == null) throw new IllegalStateException("Validated choice became unaffordable");
        }
        for (int i = 0; i < n; i++) if (payments[i] != null) {
            coins[i] -= payments[i].total();
            transfer(i, payments[i]);
        }
        List<Integer> salvage = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Held held = chosen[i];
            if (mode[i] == 1) {
                coins[i] += 3;
                discarded.add(held);
            } else if (mode[i] == 2) {
                int built = stage[i]++;
                coins[i] += STAGE_COINS[wonder[i]][built];
                shields[i] += STAGE_MIL[wonder[i]][built];
                if (STAGE_SPEC[wonder[i]][built] == AgesCard.DISCARD_BUILD) salvage.add(i);
            } else {
                cities.get(i).add(held);
                shields[i] += held.card.military;
            }
        }
        // Count effects after every city has received its simultaneous construction.
        for (int i = 0; i < n; i++) if (mode[i] == 0) immediate(i, chosen[i].card);
        for (int i = 0; i < n; i++) pick[i] = -1;
        if (!salvage.isEmpty() && !discarded.isEmpty()) {
            salvageSeat = salvage.get(0);
            for (int i = 1; i < salvage.size(); i++) pick[salvage.get(i)] = -2;
            session.changed("salvage", salvageSeat);
            roundRevision = session.revision();
            return;
        }
        finishRound(session);
    }
    private String salvage(TableSession session, int target) {
        int index = -1;
        if (target >= 0 && target < discarded.size()) index = target;
        else for (int i = 0; i < discarded.size(); i++) if (discarded.get(i).uid == target) index = i;
        if (index >= 0) {
            Held held = discarded.get(index);
            if (owns(salvageSeat, held.card)) return "cannot_play";
            discarded.remove(index);
            cities.get(salvageSeat).add(held);
            shields[salvageSeat] += held.card.military;
            immediate(salvageSeat, held.card);
        }
        if (target >= 0 && index < 0) return "invalid_card";
        int next = -1;
        for (int i = 0; i < pick.length; i++) if (pick[i] == -2) { next = i; break; }
        for (int i = 0; i < pick.length; i++) if (pick[i] == -2 && i == next) pick[i] = -1;
        salvageSeat = next;
        if (salvageSeat >= 0) {
            session.changed("salvage", salvageSeat);
            return "";
        }
        finishRound(session);
        return "";
    }
    private void finishRound(TableSession session) {
        int n = session.seatCount();
        salvageSeat = -1;
        for (int i = 0; i < n; i++) pick[i] = -1;
        if (hands.get(0).size() <= 1) {
            for (int i = 0; i < n; i++) {
                if (hands.get(i).isEmpty()) continue;
                Held last = hands.get(i).remove(0);
                if (hasSpecial(i, AgesCard.EXTRA) && canPlay(i, last.card)) playCard(i, last);
                else {
                    coins[i] += 3;
                    discarded.add(last);
                }
            }
            resolveWar(n);
            if (age == 3) {
                int best = Integer.MIN_VALUE, winner = -1, ties = 0;
                for (int i = 0; i < n; i++) {
                    int value = score(i, n);
                    if (value > best) { best = value; winner = i; ties = 1; }
                    else if (value == best) ties++;
                }
                session.finish(ties > 1 ? -1 : winner, ties > 1 ? "tied" : "played", 0);
                return;
            }
            dealAge(session, random, age + 1);
            return;
        }
        List<List<Held>> next = new ArrayList<>();
        for (int i = 0; i < n; i++) next.add(new ArrayList<>());
        for (int i = 0; i < n; i++) next.get((i + passDir + n) % n).addAll(hands.get(i));
        for (int i = 0; i < n; i++) {
            hands.get(i).clear();
            hands.get(i).addAll(next.get(i));
        }
        session.changed("passed", 0);
        roundRevision = session.revision();
    }
    private void playCard(int seat, Held held) {
        AgesCard card = held.card;
        boolean chained = chained(seat, card);
        int paid = chained ? 0 : pay(seat, card.cost, card.costCoins, true);
        if (paid < 0 && unusedFree(seat)) {
            paid = 0;
            freeUsed[seat] = true;
        }
        if (paid < 0) {
            coins[seat] += 3;
            discarded.add(held);
            return;
        }
        coins[seat] -= paid;
        cities.get(seat).add(held);
        shields[seat] += card.military;
        immediate(seat, card);
    }
    private void immediate(int seat, AgesCard card) {
        coins[seat] += card.coins;
        int n = coins.length;
        int left = (seat + n - 1) % n, right = (seat + 1) % n;
        if (card.special == AgesCard.VINEYARD) coins[seat] += countType(seat, AgesCard.BROWN) + countType(left, AgesCard.BROWN) + countType(right, AgesCard.BROWN);
        if (card.special == AgesCard.BAZAAR) coins[seat] += 2 * (countType(seat, AgesCard.GREY) + countType(left, AgesCard.GREY) + countType(right, AgesCard.GREY));
        if (card.special == AgesCard.HAVEN) coins[seat] += countType(seat, AgesCard.BROWN);
        if (card.special == AgesCard.LIGHTHOUSE) coins[seat] += countType(seat, AgesCard.YELLOW);
        if (card.special == AgesCard.ARENA) coins[seat] += 3 * stage[seat];
        if (card.special == AgesCard.CHAMBER) coins[seat] += 2 * countType(seat, AgesCard.GREY);
    }
    private boolean owns(int seat, AgesCard card) {
        return cities.get(seat).stream().anyMatch(owned -> owned.card.key.equals(card.key));
    }
    private boolean canPlay(int seat, AgesCard card) {
        if (owns(seat, card)) return false;
        if (chained(seat, card)) return true;
        if (pay(seat, card.cost, card.costCoins, false) >= 0) return true;
        return unusedFree(seat);
    }
    private boolean unusedFree(int seat) {
        return !freeUsed[seat] && hasSpecial(seat, AgesCard.FREE_AGE);
    }
    private boolean chained(int seat, AgesCard card) {
        if (card.chainFrom == 0) return false;
        for (Held owned : cities.get(seat)) if (owned.card.chainTo == card.chainFrom) return true;
        return false;
    }
    private record Supply(int mask, int seller) {}
    private record Payment(int total, int left, int right) {}

    private int pay(int seat, int cost, int coinCost, boolean commit) {
        Payment plan = payment(seat, cost, coinCost);
        if (plan == null) return -1;
        if (commit) transfer(seat, plan);
        return plan.total();
    }
    private void transfer(int seat, Payment plan) {
        int n = coins.length;
        coins[(seat + n - 1) % n] += plan.left();
        coins[(seat + 1) % n] += plan.right();
    }
    private Payment payment(int seat, int cost, int coinCost) {
        List<Supply> supplies = new ArrayList<>();
        supply(supplies, seat, 0);
        for (int i = 0; i < wilds(seat, true); i++) supplies.add(new Supply(15, 0));
        for (int i = 0; i < wilds(seat, false); i++) supplies.add(new Supply(112, 0));
        supply(supplies, (seat + coins.length - 1) % coins.length, -1);
        supply(supplies, (seat + 1) % coins.length, 1);
        Payment result = searchPayment(seat, supplies, 0, cost, new java.util.HashMap<>());
        if (result == null || result.total() + coinCost > coins[seat]) return null;
        return new Payment(result.total() + coinCost, result.left(), result.right());
    }
    private void supply(List<Supply> supplies, int owner, int seller) {
        int[] fixed = production(owner, seller != 0);
        for (int i = 0; i < fixed.length; i++) {
            for (int n = 0; n < fixed[i]; n++) supplies.add(new Supply(AgesCard.RES[i], seller));
        }
        for (Held held : cities.get(owner)) {
            AgesCard card = held.card;
            if (!card.prodOr || (seller != 0 && card.type != AgesCard.BROWN && card.type != AgesCard.GREY)) continue;
            for (int n = 0; n < card.prodCount; n++) supplies.add(new Supply(card.prodRes, seller));
        }
    }
    private Payment searchPayment(int seat, List<Supply> supplies, int index, int need,
                                  java.util.Map<Long, Payment> memo) {
        if (need == 0) return new Payment(0, 0, 0);
        if (index == supplies.size()) return null;
        long key = ((long) index << 32) | (need & 0xffffffffL);
        if (memo.containsKey(key)) return memo.get(key);
        Payment best = searchPayment(seat, supplies, index + 1, need, memo);
        Supply supply = supplies.get(index);
        for (int i = 0; i < 7; i++) {
            int resource = AgesCard.RES[i];
            if ((supply.mask() & resource) == 0 || AgesCard.qty(need, resource) == 0) continue;
            Payment tail = searchPayment(seat, supplies, index + 1, need - AgesCard.pack(resource, 1), memo);
            if (tail == null) continue;
            int price = supply.seller() == 0 ? 0 : unitCost(seat, i < 4, supply.seller() < 0);
            Payment candidate = new Payment(tail.total() + price,
                tail.left() + (supply.seller() < 0 ? price : 0), tail.right() + (supply.seller() > 0 ? price : 0));
            if (best == null || candidate.total() < best.total()) best = candidate;
        }
        memo.put(key, best);
        return best;
    }
    private int unitCost(int seat, boolean raw, boolean west) {
        if (raw && west && hasSpecial(seat, AgesCard.WEST)) return 1;
        if (raw && !west && hasSpecial(seat, AgesCard.EAST)) return 1;
        if (!raw && hasSpecial(seat, AgesCard.GREY_TRADE)) return 1;
        return 2;
    }
    private int[] production(int seat, boolean sellableOnly) {
        int[] have = new int[7];
        int bit = WONDER_RES[wonder[seat]];
        int index = Integer.numberOfTrailingZeros(bit);
        have[index]++;
        for (Held held : cities.get(seat)) {
            AgesCard card = held.card;
            if (sellableOnly && card.type != AgesCard.BROWN && card.type != AgesCard.GREY) continue;
            if (card.prodOr || card.prodRes == 0) continue;
            for (int i = 0; i < 7; i++) if ((card.prodRes & AgesCard.RES[i]) != 0) have[i] += card.prodCount;
        }
        return have;
    }
    private int wilds(int seat, boolean raw) {
        int count = 0;
        for (Held held : cities.get(seat)) {
            if (raw && held.card.special == AgesCard.ANY_RAW) count++;
            if (!raw && held.card.special == AgesCard.ANY_GREY) count++;
        }
        for (int i = 0; i < stage[seat]; i++) {
            int spec = STAGE_SPEC[wonder[seat]][i];
            if (raw && spec == AgesCard.ANY_RAW) count++;
            if (!raw && spec == AgesCard.ANY_GREY) count++;
        }
        return count;
    }
    private boolean hasSpecial(int seat, int special) {
        for (Held held : cities.get(seat)) if (held.card.special == special) return true;
        for (int i = 0; i < stage[seat]; i++) if (STAGE_SPEC[wonder[seat]][i] == special) return true;
        return false;
    }
    private int countType(int seat, int type) {
        int count = 0;
        for (Held held : cities.get(seat)) if (held.card.type == type) count++;
        return count;
    }
    private void resolveWar(int n) {
        int[] next = shields.clone();
        for (int i = 0; i < n; i++) {
            int left = (i + n - 1) % n, right = (i + 1) % n;
            war[i] += clash(next[i], next[left], i) + clash(next[i], next[right], i);
        }
    }
    private int clash(int self, int other, int seat) {
        if (self > other) return WAR[age];
        if (self < other) {
            defeats[seat]++;
            return -1;
        }
        return 0;
    }
    int score(int seat, int n) {
        int total = war[seat] + coins[seat] / 3;
        for (int i = 0; i < stage[seat]; i++) total += STAGE_VP[wonder[seat]][i];
        int[] sci = new int[3];
        int wild = 0;
        for (Held held : cities.get(seat)) {
            AgesCard card = held.card;
            total += card.vp;
            if (card.science == 1) sci[0]++;
            if (card.science == 2) sci[1]++;
            if (card.science == 4) sci[2]++;
            if (card.special == AgesCard.WILD_SCI) wild++;
            total += endSpecial(seat, n, card.special);
        }
        for (int i = 0; i < stage[seat]; i++) if (STAGE_SPEC[wonder[seat]][i] == AgesCard.WILD_SCI) wild++;
        total += scienceScore(sci, wild);
        return total;
    }
    private int endSpecial(int seat, int n, int special) {
        int left = (seat + n - 1) % n, right = (seat + 1) % n;
        return switch (special) {
            case AgesCard.HAVEN -> countType(seat, AgesCard.BROWN);
            case AgesCard.LIGHTHOUSE -> countType(seat, AgesCard.YELLOW);
            case AgesCard.ARENA -> stage[seat];
            case AgesCard.CHAMBER -> 2 * countType(seat, AgesCard.GREY);
            case AgesCard.GUILD_BROWN -> countType(left, AgesCard.BROWN) + countType(right, AgesCard.BROWN);
            case AgesCard.GUILD_GREY -> countType(left, AgesCard.GREY) + countType(right, AgesCard.GREY);
            case AgesCard.GUILD_BLUE -> countType(left, AgesCard.BLUE) + countType(right, AgesCard.BLUE);
            case AgesCard.GUILD_YELLOW -> countType(left, AgesCard.YELLOW) + countType(right, AgesCard.YELLOW);
            case AgesCard.GUILD_RED -> countType(left, AgesCard.RED) + countType(right, AgesCard.RED);
            case AgesCard.GUILD_GREEN -> countType(left, AgesCard.GREEN) + countType(right, AgesCard.GREEN);
            case AgesCard.GUILD_WONDER -> stage[left] + stage[right];
            case AgesCard.GUILD_LEADERS -> countType(seat, AgesCard.BROWN) + countType(seat, AgesCard.GREY) + countType(seat, AgesCard.PURPLE);
            case AgesCard.GUILD_DEFEAT -> defeats[left] + defeats[right];
            default -> 0;
        };
    }
    private int scienceScore(int[] sci, int wild) {
        if (wild == 0) return scienceOf(sci);
        return bestScience(sci, new int[3], wild, 0);
    }
    private int bestScience(int[] sci, int[] extra, int left, int index) {
        if (left == 0 || index == 3) {
            int[] copy = { sci[0] + extra[0], sci[1] + extra[1], sci[2] + extra[2] };
            if (left > 0) copy[2] += left;
            return scienceOf(copy);
        }
        int best = 0;
        for (int add = 0; add <= left; add++) {
            extra[index] = add;
            best = Math.max(best, bestScience(sci, extra, left - add, index + 1));
            extra[index] = 0;
        }
        return best;
    }
    private int scienceOf(int[] sci) {
        int min = Math.min(sci[0], Math.min(sci[1], sci[2]));
        return sci[0] * sci[0] + sci[1] * sci[1] + sci[2] * sci[2] + 7 * min;
    }
    private List<TableGame.Piece> layout(TableSession session) {
        List<TableGame.Piece> pieces = new ArrayList<>();
        int n = session.seatCount();
        if (session.phase() == TableGame.Phase.LOBBY || coins.length == 0) return pieces;
        for (int i = 0; i < discarded.size(); i++) {
            Held held = discarded.get(i);
            double x = 0.5 + (i - (discarded.size() - 1) / 2.0) * 0.04;
            boolean salvage = salvageSeat >= 0;
            pieces.add(new TableGame.Piece(held.uid, held.card.type, Math.min(0.92, Math.max(0.08, x)),
                0.16 + i * 0.002, 0.5, 0, 0.16f, "ages", salvage ? "salvage" : ""));
        }
        for (int seat = 0; seat < n; seat++) {
            double[] at = Layouts.seatCenter(seat, n);
            float yaw = Layouts.seatYaw(seat, n);
            pieces.add(new TableGame.Piece(seat, wonder[seat], at[0], 0.14, at[1], yaw, 0.42f, "wonder", "look"));
            List<Held> city = cities.get(seat);
            int cols = Math.max(1, (int) Math.ceil(Math.sqrt(Math.max(1, city.size()))));
            for (int i = 0; i < city.size(); i++) {
                int col = i % cols, row = i / cols;
                double[] pos = local(at, yaw, (col - (cols - 1) / 2.0) * 0.10, 0.24 + row * 0.15);
                pieces.add(new TableGame.Piece(city.get(i).uid, city.get(i).card.type, pos[0], 0.165, pos[1], yaw, 0.18f, "ages", ""));
            }
            List<Held> hand = hands.get(seat);
            int show = Math.min(7, hand.size());
            for (int i = 0; i < show; i++) {
                double[] pos = local(at, yaw, (i - (show - 1) / 2.0) * 0.05, -0.16);
                pieces.add(new TableGame.Piece(20_000 + seat * 10 + i, -1, pos[0], 0.155, pos[1], yaw + (i - (show - 1) / 2f) * 6, 0.14f, "seat", ""));
            }
            for (int i = 0; i < Math.min(8, Math.max(1, coins[seat] / 3)); i++) {
                double[] pos = local(at, yaw, -0.2, 0.02);
                pieces.add(new TableGame.Piece(10_000 + seat * 10 + i, 0, pos[0], 0.155 + i * 0.004, pos[1], yaw, 0.12f, "coin", ""));
            }
        }
        return pieces;
    }
    private static double[] local(double[] at, float yaw, double localX, double localZ) {
        double rad = Math.toRadians(yaw);
        return new double[] {
            at[0] + localX * Math.cos(rad) + localZ * Math.sin(rad),
            at[1] - localX * Math.sin(rad) + localZ * Math.cos(rad)
        };
    }
    private static int[] fill(int n, int value) {
        int[] array = new int[n];
        java.util.Arrays.fill(array, value);
        return array;
    }
}
