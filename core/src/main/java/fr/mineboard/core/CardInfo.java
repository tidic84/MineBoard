package fr.mineboard.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Descriptions derived from rule data, so cards sharing family artwork stay identifiable. */
public final class CardInfo {
    private CardInfo() {}

    public static List<String> describe(int handId, Function<String, String> text) {
        if (!AgesCard.isHand(handId)) return List.of();
        AgesCard card = AgesCard.byId(handId - AgesCard.HAND);
        List<String> lines = new ArrayList<>();
        lines.add(text.apply("ages.name." + card.key) + " · " + text.apply("ages.age") + " " + card.age);
        List<String> cost = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            int amount = AgesCard.qty(card.cost, AgesCard.RES[i]);
            if (amount > 0) cost.add(amount + " " + text.apply("ages.resource." + i));
        }
        if (card.costCoins > 0) cost.add(card.costCoins + " " + text.apply("ages.coins"));
        lines.add(text.apply("ages.cost") + " : " + (cost.isEmpty() ? text.apply("ages.free") : String.join(", ", cost)));
        List<String> produces = new ArrayList<>();
        for (int i = 0; i < 7; i++) if ((card.prodRes & AgesCard.RES[i]) != 0) {
            produces.add(card.prodCount + " " + text.apply("ages.resource." + i));
        }
        if (!produces.isEmpty()) lines.add(text.apply("ages.produces") + " : "
            + String.join(card.prodOr ? " / " : ", ", produces));
        List<String> gains = new ArrayList<>();
        if (card.vp > 0) gains.add(card.vp + " " + text.apply("ages.points"));
        if (card.coins > 0) gains.add(card.coins + " " + text.apply("ages.coins"));
        if (card.military > 0) gains.add(card.military + " " + text.apply("ages.shields"));
        if (card.science > 0) gains.add(text.apply("ages.science." + card.science));
        if (!gains.isEmpty()) lines.add(String.join(" · ", gains));
        if (card.special > 0) lines.add(text.apply("ages.special." + card.special));
        if (card.chainFrom > 0) lines.add(text.apply("ages.chain") + " " + card.chainFrom);
        if (card.chainTo > 0) lines.add(text.apply("ages.unlock") + " " + card.chainTo);
        return lines;
    }
}
