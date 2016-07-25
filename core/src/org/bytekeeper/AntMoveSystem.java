package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector2;

import static org.bytekeeper.Components.POSITION;

/**
 * Created by dante on 23.07.16.
 */
public class AntMoveSystem extends EntitySystem {
    private Iterable<Entity> entities;
    private Vector2 v1 = new Vector2();

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(Physical.class).get());
    }

    @Override
    public void update(float deltaTime) {
        for (Entity e: entities) {
            Physical physical = POSITION.get(e);
            if (physical.moveTime > 0) {
                v1.set(Vector2.X).rotateRad(physical.orientation);
                physical.position.mulAdd(v1, 50 * Math.min(deltaTime, physical.moveTime));
                physical.moveTime = Math.max(0, physical.moveTime - deltaTime);
            }
        }
    }
}
