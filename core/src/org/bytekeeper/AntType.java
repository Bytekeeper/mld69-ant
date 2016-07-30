package org.bytekeeper;

/**
 * Created by dante on 25.07.16.
 */
public enum AntType {
    GATHERER(10), WARRIOR(30);

    public final float cost;

    AntType(float cost) {
        this.cost = cost;
    }
}
