package net.villagerzock;

public interface BetterCloneable<T extends BetterCloneable<T>> {
    T betterClone();
}
