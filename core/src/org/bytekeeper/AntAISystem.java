package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;

import static java.lang.Float.POSITIVE_INFINITY;
import static org.bytekeeper.Components.ANT_AI;
import static org.bytekeeper.Components.FOOD;
import static org.bytekeeper.Components.POSITION;
import static org.bytekeeper.State.*;

/**
 * Created by dante on 23.07.16.
 */
public class AntAISystem extends EntitySystem {
    private static final float EPS = 0.0000001f;
    private static final float RASTER = 50;
    private static final float GATHER_AMOUNT = 10;
    private ImmutableArray<Entity> entities;
    private AntGame game;
    private BestFoodTrail bestFoodTrail = new BestFoodTrail();
    private BestHomeTrail bestHomeTrail = new BestHomeTrail();
    private Closest closet = new Closest();
    private static RandomXS128 rnd = new RandomXS128();
    private Iterable<Entity> foodStorages;
    private Iterable<Entity> food;
    public int antAmount;

    public AntAISystem(AntGame game) {
        this.game = game;
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(AntAI.class, Physical.class).get());
        foodStorages = engine.getEntitiesFor(Family.all(Base.class, Physical.class).get());
        food = engine.getEntitiesFor(Family.all(Food.class, Physical.class).get());
    }

    @Override
    public void update(float deltaTime) {
        antAmount = 0;
        for (Entity e: entities) {
            antAmount++;
            AntAI antAI = ANT_AI.get(e);
            antAI.nextEat -= deltaTime;
            if (antAI.nextEat <= 0) {
                antAI.nextEat = 5 + rnd.nextFloat() * 3;
                if (game.player.food > 1) {
                    game.player.food--;
                } else {
                    getEngine().removeEntity(e);
                }
            }
            Physical physical = POSITION.get(e);
            Pheromon pheromon = getOrCreatePheromon(physical.position.x, physical.position.y);
            switch (antAI.state) {
                case IDLE:
                case SEARCH_FOOD:
                    pheromon.homePath += deltaTime;
                    pheromon.foodPath = Math.max(0, pheromon.foodPath - deltaTime / 3);
                    break;
                case GATHER_FOOD:
                    if (physical.moveTime == 0) {
                        antAI.remaining = Math.max(antAI.remaining - deltaTime, 0);
                        if (antAI.remaining == 0) {
                            antAI.state = BRING_FOOD_HOME;
                        }
                    }
                    break;
                case BRING_FOOD_HOME:
                    pheromon.foodPath += deltaTime;
                    pheromon.homePath = Math.max(0, pheromon.homePath - deltaTime / 20);
                    break;
            }
            if (physical.moveTime > 0) {
                continue;
            }
            switch (antAI.state) {
                case IDLE:
                case SEARCH_FOOD:
                    searchFood(antAI, physical);
                    break;
                case BRING_FOOD_HOME:
                    bringFoodHome(antAI, physical);
                    break;
            }
        }
    }

    private void bringFoodHome(AntAI antAI, Physical physical) {
        for (Entity f: foodStorages) {
            Physical fpos = POSITION.get(f);
            Vector2 position = fpos.position;
            if (position.dst(physical.position) < 50) {
                antAI.state = IDLE;
                game.player.food += GATHER_AMOUNT;
                moveToward(physical, position.x, position.y, 2);
                return;
            }
        }
        bestHomeTrail.reset();
        game.grid.root.inRadius(physical.position.x, physical.position.y, 100, bestHomeTrail);
        physical.moveTime = rnd.nextFloat() + 1;
        if (bestHomeTrail.best == null ||
                Vector2.dst(bestHomeTrail.bestTarget.x,
                        bestHomeTrail.bestTarget.y,
                        physical.position.x,
                        physical.position.y) < 30) {
            physical.orientation += rnd.nextFloat() - 0.5f;
            if (physical.orientation < MathUtils.PI) {
                physical.orientation += MathUtils.PI2;
            } else if (physical.orientation > MathUtils.PI) {
                physical.orientation -= MathUtils.PI2;
            }
        } else {
            moveToward(physical, bestHomeTrail.bestTarget.x, bestHomeTrail.bestTarget.y, 30);
        }
    }

    private void moveToward(Physical physical, float x, float y, float precision) {
        float dY = y - physical.position.y + rnd.nextFloat() * precision * 2 - precision;
        float dX = x - physical.position.x + rnd.nextFloat() * precision * 2 - precision;
        physical.orientation = (float) Math.atan2(dY, dX);
    }

    private void searchFood(AntAI antAI, Physical physical) {
        for (Entity f: food) {
            Physical fpos = POSITION.get(f);
            Vector2 position = fpos.position;
            Food food = FOOD.get(f);
            if (food.amount > 0 && position.dst(physical.position) < 50) {
                food.amount = Math.max(0, food.amount - GATHER_AMOUNT);
                antAI.state = GATHER_FOOD;
                antAI.remaining = 3;
                getOrCreatePheromon(position.x, position.y).foodPath++;
                moveToward(physical, position.x, position.y, 2);
                return;
            }
        }
        bestFoodTrail.reset();
        game.grid.root.inRadius(physical.position.x, physical.position.y, 100, bestFoodTrail);
        antAI.state = SEARCH_FOOD;
        physical.moveTime = rnd.nextFloat() + 1;
        if (bestFoodTrail.best == null || rnd.nextFloat() < 0.1f ||
                Vector2.dst(bestFoodTrail.bestTarget.x,
                        bestFoodTrail.bestTarget.y,
                        physical.position.x,
                        physical.position.y) < 2) {
            physical.orientation += rnd.nextFloat();
            if (physical.orientation < MathUtils.PI) {
                physical.orientation += MathUtils.PI2;
            } else if (physical.orientation > MathUtils.PI) {
                physical.orientation -= MathUtils.PI2;
            }
        } else {
            moveToward(physical, bestFoodTrail.bestTarget.x, bestFoodTrail.bestTarget.y, 30);
        }
    }

    public Pheromon getOrCreatePheromon(float x, float y) {
        closet.reset();
        game.grid.root.inRadius(x, y, 50, closet);
        if (closet.best != null) {
            return closet.best;
        }
        Pheromon pheromon = new Pheromon();
        game.grid.root.addValue((float) Math.ceil(x / RASTER) * RASTER, (float) (Math.ceil(y / RASTER) * RASTER), pheromon);
        return pheromon;
    }

    public static class Closest implements Grid.ElementCallback<Pheromon> {
        Pheromon best;
        float bestDst = POSITIVE_INFINITY;
        final Vector2 target = new Vector2();

        public void reset() {
            best = null;
            bestDst = POSITIVE_INFINITY;
        }

        @Override
        public boolean accept(float x, float y, Pheromon e) {
            float dst = target.dst(x, y);
            if (dst < bestDst) {
                best = e;
                bestDst = dst;
            }
            return false;
        }
    }

    public static class BestFoodTrail implements Grid.ElementCallback<Pheromon> {
        Pheromon best;
        float bestScore;
        final Vector2 bestTarget = new Vector2();

        public void reset() {
            best = null;
            bestScore = Float.NEGATIVE_INFINITY;
        }

        @Override
        public boolean accept(float x, float y, Pheromon p) {
            float score = p.foodPath / (p.homePath + 1);
            if (score > 0) {
                score += rnd.nextFloat() * EPS;
            }
            if (score > 0 && (best == null || score > bestScore)) {
                best = p;
                bestTarget.set(x, y);
                bestScore = score;
            }
            return false;
        }
    }

    public static class BestHomeTrail implements Grid.ElementCallback<Pheromon> {
        Pheromon best;
        final Vector2 bestTarget = new Vector2();

        public void reset() {
            best = null;
        }

        @Override
        public boolean accept(float x, float y, Pheromon p) {
            if (p.homePath > 0 && (best == null || p.homePath > best.homePath)) {
                best = p;
            }
            return false;
        }
    }
}
