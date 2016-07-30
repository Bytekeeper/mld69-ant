package org.bytekeeper;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;

import static org.bytekeeper.Components.LARVA;
import static org.bytekeeper.Components.PHYSICAL;

/**
 * Created by dante on 25.07.16.
 */
public class LarvaSystem extends IteratingSystem {
    private final AntGame game;

    public LarvaSystem(AntGame game) {
        super(Family.all(Larva.class, Physical.class).get());
        this.game = game;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (game.paused) {
            return;
        }
        Larva larva = LARVA.get(entity);
        larva.buildTimeRemaining -= deltaTime;
        if (larva.buildTimeRemaining <= 0) {
            getEngine().removeEntity(entity);
            Vector2 position = PHYSICAL.get(entity).position;
            switch (larva.morphInto) {
                case GATHERER:
                    game.spawnWorkerAnt(larva.owner, position);
                    break;
                case WARRIOR:
                    game.spawnWarriorAnt(larva.owner, position);
                    break;
            }
        }
    }
}
