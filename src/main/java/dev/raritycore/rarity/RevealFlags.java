package dev.raritycore.rarity;

/**
 * Defines bitmask flags for determining which hidden elements of a RarityCore item are revealed.
 */
public final class RevealFlags {

    public static final int FLAG_NONE    = 0;
    public static final int FLAG_RARITY  = 1 << 0; // 1
    public static final int FLAG_QUALITY = 1 << 1; // 2
    public static final int FLAG_TRAITS  = 1 << 2; // 4
    public static final int FLAG_AFFIXES = 1 << 3; // 8
    public static final int FLAG_ALL     = FLAG_RARITY | FLAG_QUALITY | FLAG_TRAITS | FLAG_AFFIXES; // 15

    private RevealFlags() {}

    public static boolean hasFlag(int state, int flag) {
        return (state & flag) == flag;
    }

    public static int addFlag(int state, int flag) {
        return state | flag;
    }

    public static int removeFlag(int state, int flag) {
        return state & ~flag;
    }
}
