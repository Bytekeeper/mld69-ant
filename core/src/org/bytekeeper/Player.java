package org.bytekeeper;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;

/**
 * Created by dante on 25.07.16.
 */
public class Player implements Component {
    public Grid<Pheromon> grid = new Grid<>();
    public float food;
    public int antAmount;
    public final Color color = new Color();
}
