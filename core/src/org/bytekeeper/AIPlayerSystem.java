package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

import static org.bytekeeper.Components.*;

/**
 * Created by dante on 28.07.16.
 */
public class AIPlayerSystem extends IteratingSystem {
    private final AntGame game;
    private Iterable<Entity> bases;

    public AIPlayerSystem(AntGame game) {
        super(Family.all(Player.class, AIState.class).get());
        this.game = game;
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        bases = engine.getEntitiesFor(Family.all(Base.class, Physical.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (game.paused) {
            return;
        }
        Player player = PLAYER.get(entity);
        AIState aiState = AI_STATE.get(entity);

        Entity base = null;
        for (Entity e: bases) {
            if (BASE.get(e).owner.equals(player)) {
                base = e;
                break;
            }
        }

        if (player.food > 110 + player.antAmount * 10) {
            game.spawnLarva(player, Buildable.GATHERER, PHYSICAL.get(base).position);
        }
    }
}
