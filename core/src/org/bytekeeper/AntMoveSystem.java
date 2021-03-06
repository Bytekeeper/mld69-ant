package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.FloatCounter;
import com.badlogic.gdx.math.Vector2;

import static org.bytekeeper.Components.PHYSICAL;

/**
 * Created by dante on 23.07.16.
 */
public class AntMoveSystem extends EntitySystem {
    private final AntGame antGame;
    private Iterable<Entity> entities;
    private Vector2 v1 = new Vector2();

    public AntMoveSystem(AntGame antGame) {
        this.antGame = antGame;
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(Physical.class).get());
    }

    @Override
    public void update(float deltaTime) {
        if (antGame.paused) {
            return;
        }
        for (Entity e: entities) {
            Physical physical = PHYSICAL.get(e);
            if (Float.isNaN(physical.position.x) || Float.isInfinite(physical.position.x) ||
                    Float.isNaN(physical.position.y) || Float.isInfinite(physical.position.y)) {
                throw new IllegalStateException();
            }
            if (physical.moveTime > 0) {
                v1.set(Vector2.X).rotateRad(physical.orientation);
                Vector2 position = physical.position;
                antGame.entityQueries.removeValue(position.x, position.y, e);
                position.mulAdd(v1, physical.speed * Math.min(deltaTime, physical.moveTime));
                physical.moveTime = Math.max(0, physical.moveTime - deltaTime);
                antGame.entityQueries.addValue(position.x, position.y, e);
            }
        }
    }
}
