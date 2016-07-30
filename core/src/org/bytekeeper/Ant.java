package org.bytekeeper;

import com.badlogic.ashley.core.Component;

import static org.bytekeeper.State.INVALID;

/**
 * Created by dante on 23.07.16.
 */
public class Ant implements Component {
    public Player owner;
    public AntType type;
    public State state = INVALID;
    public float remaining;
    public float nextEat;
    public float distanceTravelled;
}
