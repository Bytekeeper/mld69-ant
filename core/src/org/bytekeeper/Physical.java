package org.bytekeeper;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by dante on 23.07.16.
 */
public class Physical implements Component {
    public final Vector2 position = new Vector2();
    public float orientation;
    public float moveTime;
    public float speed;
}
