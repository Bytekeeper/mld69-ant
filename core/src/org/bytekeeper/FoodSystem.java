package org.bytekeeper;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

import static org.bytekeeper.Components.FOOD;

/**
 * Created by dante on 25.07.16.
 */
public class FoodSystem extends IteratingSystem {
    public FoodSystem() {
        super(Family.all(Food.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (FOOD.get(entity).amount == 0) {
            getEngine().removeEntity(entity);
        }
    }
}
