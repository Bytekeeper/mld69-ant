package org.bytekeeper;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Created by dante on 23.07.16.
 */
public interface BatchRenderer {
    void render(SpriteBatch batch, Entity entity);
}
