package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;

import static org.bytekeeper.AntType.GATHERER;
import static org.bytekeeper.AntType.WARRIOR;
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

        Vector2 spawnPosition = PHYSICAL.get(base).position;
        if (player.food > Math.max(GATHERER.cost, 30 + player.antAmount * 5)) {
            game.spawnLarva(player, AntType.GATHERER, spawnPosition);
        }
        if (player.food > Math.max(WARRIOR.cost, 50 + player.antAmount * 3)) {
            game.spawnLarva(player, WARRIOR, spawnPosition);
        }
    }
}
