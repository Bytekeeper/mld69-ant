package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;

import static org.bytekeeper.Components.PLAYER;

/**
 * Created by dante on 28.07.16.
 */
public class WinConditionSystem extends EntitySystem {
    public static final Player DRAW = new Player();
    public static final Player LOST = new Player();
    private final AntGame game;
    public Player wonBy = null;
    private Iterable<Entity> players;

    public WinConditionSystem(AntGame game) {
        this.game = game;
    }

    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(Player.class).get());
    }

    @Override
    public void update(float deltaTime) {
        if (game.paused) {
            return;
        }
        Player currentWinner = DRAW;
        for (Entity e: players) {
            Player player = PLAYER.get(e);
            if (player.antAmount > 0) {
                if (currentWinner != DRAW) {
                    return;
                }
                currentWinner = player;
            } else if (player == game.humanPlayer) {
                currentWinner = LOST;
                wonBy = LOST;
                return;
            }
        }
        wonBy = currentWinner;
    }
}
