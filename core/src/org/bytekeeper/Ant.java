package org.bytekeeper;

import com.badlogic.ashley.core.Component;

import static org.bytekeeper.State.IDLE;

/**
 * Created by dante on 23.07.16.
 */
public class Ant implements Component {
    public Player owner;
    public State state = IDLE;
    public float remaining;
    public float nextEat;
    public boolean canGatherFood;
    public float distance;
}
