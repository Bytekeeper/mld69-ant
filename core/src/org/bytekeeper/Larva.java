package org.bytekeeper;

import com.badlogic.ashley.core.Component;

/**
 * Created by dante on 25.07.16.
 */
public class Larva implements Component {
    public Buildable morphInto;
    public float buildTimeRemaining;
}
