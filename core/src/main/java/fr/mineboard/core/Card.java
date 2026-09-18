package fr.mineboard.core;

/** A compact, stable wire ID. Assets and game logic share this mapping. */
public record Card(int color, int number) {
    public Card {
        if (color < 0 || color > 3 || number < 0 || number > 9) {
            throw new IllegalArgumentException("Invalid card");
        }
    }
    public int id() { return color * 10 + number; }
    public static Card fromId(int id) {
        if (id < 0 || id >= 40) throw new IllegalArgumentException("Invalid card ID");
        return new Card(id / 10, id % 10);
    }
    public boolean matches(Card other) { return color == other.color || number == other.number; }
}
