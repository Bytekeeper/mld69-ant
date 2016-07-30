package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.RandomXS128;

import java.util.Random;

import static org.bytekeeper.Components.ANT;
import static org.bytekeeper.Components.FOOD;

/**
 * Created by dante on 25.07.16.
 */
public class FoodSystem extends EntitySystem {
    private final AntGame game;
    private Iterable<Entity> foodStores;
    private Iterable<Entity> ants;
    private RandomXS128 rnd = new RandomXS128();

    public FoodSystem(AntGame game) {
        super();
        this.game = game;
    }

    @Override
    public void addedToEngine(Engine engine) {
        foodStores = engine.getEntitiesFor(Family.all(Food.class).get());
        ants = engine.getEntitiesFor(Family.all(Ant.class).get());
    }


    @Override
    public void update(float deltaTime) {
        if (game.paused) {
            return;
        }

        for (Entity entity: foodStores) {
            if (FOOD.get(entity).amount == 0) {
                getEngine().removeEntity(entity);
            }
        }

        for (Entity entity: ants) {
            Ant ant = ANT.get(entity);
            ant.nextEat -= deltaTime;
            if (ant.nextEat <= 0) {
                ant.nextEat = 25 + rnd.nextFloat() * 5;
                if (ant.owner.food > 1) {
                    ant.owner.food--;
                } else {
                    getEngine().removeEntity(entity);
                }
            }
        }
    }
}
