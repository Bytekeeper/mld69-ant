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
    private static final float RASTER = 50;
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
        deltaTime = Math.min(deltaTime, 1 / 30f);
        for (Entity e: players) {
            PLAYER.get(e).antAmount = 0;
        }
        for (Entity e: entities) {
            Ant ant = ANT.get(e);
            ant.owner.antAmount++;
            ant.nextEat -= deltaTime;
            if (ant.nextEat <= 0) {
                ant.nextEat = 15 + rnd.nextFloat() * 5;
                if (ant.owner.food > 1) {
                    ant.owner.food--;
                } else {
                    getEngine().removeEntity(e);
                }
            }

            Physical physical = PHYSICAL.get(e);
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
                    pheromon.homePath += deltaTime;
                    pheromon.foodPath = Math.max(0, pheromon.foodPath - deltaTime / 2);
                    break;
                case GATHER_FOOD:
                    if (physical.moveTime == 0) {
                        ant.remaining = Math.max(ant.remaining - deltaTime, 0);
                        if (ant.remaining == 0) {
                            ant.state = BRING_FOOD_HOME;
                        }
                    }
                    ant.distance = 0;
                    break;
                case BRING_FOOD_HOME:
                    ant.distance += deltaTime;
                    pheromon.foodPath += deltaTime;
                    pheromon.homePath = Math.max(0, pheromon.homePath - deltaTime / 4);
                    break;
            }
            if (physical.moveTime > 0) {
                continue;
            }
            int enemies = 0;

            for (int i = entities.size() - 1; i >= 0 ; i--) {
                Entity other = entities.get(i);
                Ant otherAnt = ANT.get(other);
                Physical otherPhysical = PHYSICAL.get(other);
                if (!otherAnt.owner.equals(ant.owner) &&
                        physical.position.dst(otherPhysical.position) < 50) {
                    enemies++;
                }
            }
            pheromon.danger = enemies > 0 ? pheromon.danger + enemies : Math.max(0, pheromon.danger - 1);
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
            Physical fpos = PHYSICAL.get(f);
            Vector2 position = fpos.position;
            float dst = position.dst(physical.position);
            if (dst < 20) {
                ant.state = IDLE;
                owner.food += GATHER_AMOUNT;
                getOrCreatePheromon(owner, position.x, position.y).homePath++;
            }
            if (dst < 30) {
                moveToward(physical, position.x, position.y, 2);
                return;
            }
        }
        bestHomeTrail.reset();
        owner.locationQueries.inRadius(physical.position.x, physical.position.y, 100, bestHomeTrail);
        if (bestHomeTrail.best == null) {
            applyMoveTime(physical);
            applyScanOrientation(physical);
        } else {
            moveToward(physical, bestHomeTrail.bestTarget.x, bestHomeTrail.bestTarget.y, 10);
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
        physical.moveTime = (float) (Math.sqrt(dX * dX + dY * dY) / AntMoveSystem.ANT_SPEED) * (rnd.nextFloat() / 10 + 0.95f);
    }

    private void searchFood(Player owner, Ant ant, Physical physical) {
        for (Entity f: food) {
            Physical fpos = PHYSICAL.get(f);
            Vector2 position = fpos.position;
            Food food = FOOD.get(f);
            if (food.amount > 0) {
                if (position.dst(physical.position) < 20) {
                    food.amount = Math.max(0, food.amount - GATHER_AMOUNT);
                    ant.state = GATHER_FOOD;
                    ant.remaining = 3;
                    getOrCreatePheromon(owner, position.x, position.y).foodPath++;
                }
                if (position.dst(physical.position) < 50) {
                    moveToward(physical, position.x, position.y, 2);
                    return;
                }
            }
        }
        bestFoodTrail.reset();
        owner.locationQueries.inRadius(physical.position.x, physical.position.y, 50, bestFoodTrail);
        ant.state = SEARCH_FOOD;
        if (bestFoodTrail.best == null || rnd.nextFloat() < 0.01f) {
            applyMoveTime(physical);
            applyScanOrientation(physical);
        } else {
            moveToward(physical, bestFoodTrail.bestTarget.x, bestFoodTrail.bestTarget.y, 10);
        }
    }

    public Pheromon getOrCreatePheromon(Player owner, float x, float y) {
        closest.reset();
        closest.target.set(x, y);
        owner.locationQueries.inRadius(x, y, RASTER * 1.5f, closest);
        if (closest.best != null) {
            return closest.best;
        }
        Pheromon pheromon = new Pheromon();
        owner.locationQueries.addValue((float) Math.floor(x / RASTER) * RASTER, (float) (Math.floor(y / RASTER) * RASTER), pheromon);
        return pheromon;
    }

    public static class Closest implements LocationQueries.Callback<Pheromon> {
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

    public static class BestFoodTrail implements LocationQueries.Callback<Pheromon> {
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

    public static class BestHomeTrail implements LocationQueries.Callback<Pheromon> {
        Pheromon best;
        float bestScore;
        final Vector2 bestTarget = new Vector2();
        public int visited;

        public void reset() {
            best = null;
            bestScore = Float.NEGATIVE_INFINITY;
            visited = 0;
        }

        @Override
        public boolean accept(float x, float y, Pheromon p) {
            visited++;
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
