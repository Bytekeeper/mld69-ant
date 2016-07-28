package org.bytekeeper;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

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
            switch (larva.morphInto) {
                case GATHERER:
                    game.spawnWorkerAnt(larva.owner, PHYSICAL.get(entity).position);
                    break;
            }
        }
    }
}
