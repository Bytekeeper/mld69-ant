package org.bytekeeper;

import com.badlogic.ashley.core.Component;

import static org.bytekeeper.State.IDLE;

/**
 * Created by dante on 23.07.16.
 */
public class AntAI implements Component {
    public State state = IDLE;
    public float remaining;
    public float nextEat;
}
