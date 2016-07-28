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
import static org.bytekeeper.Components.*;
import static org.bytekeeper.State.*;

/**
 * Created by dante on 23.07.16.
 */
public class AntSystem extends EntitySystem {
    private static final float EPS = 0.0000001f;
    private static final float RASTER = 20;
    private static final float GATHER_AMOUNT = 10;
    private ImmutableArray<Entity> entities;
    private AntGame game;
    private BestFoodTrail bestFoodTrail = new BestFoodTrail();
    private BestHomeTrail bestHomeTrail = new BestHomeTrail();
    private Closest closest = new Closest();
    private static RandomXS128 rnd = new RandomXS128();
    private Iterable<Entity> foodStorages;
    private Iterable<Entity> food;
    private Iterable<Entity> players;

    public AntSystem(AntGame game) {
        this.game = game;
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(Ant.class, Physical.class).get());
        foodStorages = engine.getEntitiesFor(Family.all(Base.class, Physical.class).get());
        food = engine.getEntitiesFor(Family.all(Food.class, Physical.class).get());
        players = engine.getEntitiesFor(Family.all(Player.class).get());
    }

    @Override
    public void update(float deltaTime) {
        if (game.paused) {
            return;
        }
        for (Entity e: players) {
            PLAYER.get(e).antAmount = 0;
        }
        for (Entity e: entities) {
            Ant ant = ANT.get(e);
            ant.owner.antAmount++;
            ant.nextEat -= deltaTime;
            if (ant.nextEat <= 0) {
                ant.nextEat = 5 + rnd.nextFloat() * 3;
                if (ant.owner.food > 1) {
                    ant.owner.food--;
                } else {
                    getEngine().removeEntity(e);
                }
            }

            Physical physical = POSITION.get(e);
            if (physical.orientation < -MathUtils.PI) {
                physical.orientation += MathUtils.PI2;
            } else if (physical.orientation > MathUtils.PI) {
                physical.orientation -= MathUtils.PI2;
            }
            Player owner = ant.owner;
            Pheromon pheromon = getOrCreatePheromon(owner, physical.position.x, physical.position.y);
            switch (ant.state) {
                case IDLE:
                case SEARCH_FOOD:
                    ant.distance += deltaTime;
                    pheromon.homePath += deltaTime / (ant.distance + 1);
                    pheromon.foodPath = Math.max(0, pheromon.foodPath - deltaTime / 2);
                    break;
                case GATHER_FOOD:
                    if (physical.moveTime == 0) {
                        ant.remaining = Math.max(ant.remaining - deltaTime, 0);
                        if (ant.remaining == 0) {
                            ant.state = BRING_FOOD_HOME;
                        }
                    }
                    break;
                case BRING_FOOD_HOME:
                    ant.distance += deltaTime;
                    pheromon.foodPath += deltaTime / (ant.distance + 1);
                    pheromon.homePath = Math.max(0, pheromon.homePath - deltaTime / 10);
                    break;
            }
            if (physical.moveTime > 0) {
                continue;
            }
            switch (ant.state) {
                case IDLE:
                case SEARCH_FOOD:
                    searchFood(owner, ant, physical);
                    break;
                case BRING_FOOD_HOME:
                    bringFoodHome(owner, ant, physical);
                    break;
            }
        }
    }

    private void bringFoodHome(Player owner, Ant ant, Physical physical) {
        for (Entity f: foodStorages) {
            Base base = BASE.get(f);
            if (!base.owner.equals(owner)) {
                continue;
            }
            Physical fpos = POSITION.get(f);
            Vector2 position = fpos.position;
            if (position.dst(physical.position) < 50) {
                ant.state = IDLE;
                ant.distance = 0;
                owner.food += GATHER_AMOUNT;
                moveToward(physical, position.x, position.y, 2);
                return;
            }
        }
        bestHomeTrail.reset();
        owner.grid.root.inRadius(physical.position.x, physical.position.y, 100, bestHomeTrail);
        applyMoveTime(physical);
        if (bestHomeTrail.best == null ||
                Vector2.dst(bestHomeTrail.bestTarget.x,
                        bestHomeTrail.bestTarget.y,
                        physical.position.x,
                        physical.position.y) < 30) {
            applyScanOrientation(physical);
        } else {
            moveToward(physical, bestHomeTrail.bestTarget.x, bestHomeTrail.bestTarget.y, 30);
        }
    }

    private void applyScanOrientation(Physical physical) {
        physical.orientation += rnd.nextFloat() - 0.3f;
    }

    private void applyMoveTime(Physical physical) {
        physical.moveTime = (rnd.nextFloat() + 1) / 3;
    }

    private void moveToward(Physical physical, float x, float y, float precision) {
        float dY = y - physical.position.y + rnd.nextFloat() * precision * 2 - precision;
        float dX = x - physical.position.x + rnd.nextFloat() * precision * 2 - precision;
        physical.orientation = (float) Math.atan2(dY, dX);
    }

    private void searchFood(Player owner, Ant ant, Physical physical) {
        for (Entity f: food) {
            Physical fpos = POSITION.get(f);
            Vector2 position = fpos.position;
            Food food = FOOD.get(f);
            if (food.amount > 0 && position.dst(physical.position) < 50) {
                ant.distance = 0;
                food.amount = Math.max(0, food.amount - GATHER_AMOUNT);
                ant.state = GATHER_FOOD;
                ant.remaining = 3;
                getOrCreatePheromon(owner, position.x, position.y).foodPath++;
                moveToward(physical, position.x, position.y, 2);
                return;
            }
        }
        bestFoodTrail.reset();
        owner.grid.root.inRadius(physical.position.x, physical.position.y, 100, bestFoodTrail);
        ant.state = SEARCH_FOOD;
        applyMoveTime(physical);
        if (bestFoodTrail.best == null || rnd.nextFloat() < 0.02f ||
                Vector2.dst(bestFoodTrail.bestTarget.x,
                        bestFoodTrail.bestTarget.y,
                        physical.position.x,
                        physical.position.y) < 2) {
            applyScanOrientation(physical);
        } else {
            moveToward(physical, bestFoodTrail.bestTarget.x, bestFoodTrail.bestTarget.y, 30);
        }
    }

    public Pheromon getOrCreatePheromon(Player owner, float x, float y) {
        closest.reset();
        owner.grid.root.inRadius(x, y, 50, closest);
        if (closest.best != null) {
            return closest.best;
        }
        Pheromon pheromon = new Pheromon();
        owner.grid.root.addValue((float) Math.ceil(x / RASTER) * RASTER, (float) (Math.ceil(y / RASTER) * RASTER), pheromon);
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
            float score = p.foodPath;
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
        float bestScore;
        final Vector2 bestTarget = new Vector2();

        public void reset() {
            best = null;
            bestScore = Float.NEGATIVE_INFINITY;
        }

        @Override
        public boolean accept(float x, float y, Pheromon p) {
            float score = p.homePath;
            if (score > 0) {
                score += rnd.nextFloat() * EPS;
            }
            if (score > 0 && (best == null || score > bestScore)) {
                best = p;
                bestScore = score;
                bestTarget.set(x, y);
            }
            return false;
        }
    }
}
