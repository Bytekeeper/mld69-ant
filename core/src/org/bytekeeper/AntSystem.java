package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import static org.bytekeeper.Components.*;
import static org.bytekeeper.State.*;

/**
 * Created by dante on 23.07.16.
 */
public class AntSystem extends EntitySystem {
    private static final float RASTER = 50;
    private static final float GATHER_AMOUNT = 10;
    private static final float PHEROMON_RANGE = RASTER * 4;
    private ImmutableArray<Entity> entities;
    private AntGame game;
    private BestFoodTrail bestFoodTrail = new BestFoodTrail();
    private BestHomeTrail bestHomeTrail = new BestHomeTrail();
    private BestDangerTrail bestDangerTrail = new BestDangerTrail();
    private ClosestEnemy closestEnemy = new ClosestEnemy();
    private static RandomXS128 rnd = new RandomXS128();
    private Iterable<Entity> foodStorages;
    private Iterable<Entity> food;
    private Iterable<Entity> players;
    private Vector2 v1 = new Vector2();

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

            Physical physical = PHYSICAL.get(e);
            if (physical.orientation < -MathUtils.PI) {
                physical.orientation += MathUtils.PI2;
            } else if (physical.orientation > MathUtils.PI) {
                physical.orientation -= MathUtils.PI2;
            }
            Player owner = ant.owner;
            Wrapper wrapper = getOrCreatePheromon(owner, physical.position.x, physical.position.y);
            Pheromon pheromon = wrapper.pheromon;
            float dst = wrapper.dst / RASTER + 1;
            pheromon.foodPath = Math.max(0, pheromon.foodPath - deltaTime / 2 / dst);
            pheromon.homePath = Math.max(0, pheromon.homePath - deltaTime / 10 / dst);
            pheromon.danger = Math.max(0, pheromon.danger - deltaTime / 2 / dst);
            ant.distanceTravelled += deltaTime;
            switch (ant.state) {
                case SEARCH_FOOD:
                    pheromon.homePath = pheromon.homePath + deltaTime / (ant.distanceTravelled + 5) * 10 / dst;
                    break;
                case GATHER_FOOD:
                    pheromon.foodPath =  pheromon.foodPath + deltaTime / (ant.distanceTravelled + 5) * 10 / dst;
                    if (physical.moveTime == 0) {
                        ant.remaining = Math.max(ant.remaining - deltaTime, 0);
                        if (ant.remaining == 0) {
                            ant.state = BRING_FOOD_HOME;
                            ant.distanceTravelled = 0;
                        }
                    }
                    break;
                case RECOVER_FROM_ATTACKING:
                    if (physical.moveTime == 0) {
                        ant.remaining = Math.max(ant.remaining - deltaTime, 0);
                        if (ant.remaining == 0) {
                            ant.state = ATTACK;
                        }
                    }
                    break;
                case BRING_FOOD_HOME:
                    pheromon.foodPath =  pheromon.foodPath + deltaTime / (ant.distanceTravelled + 5) * 5 / dst;
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
                        physical.position.dst(otherPhysical.position) < RASTER) {
                    enemies++;
                }
            }
            pheromon.danger += pheromon.danger + enemies / dst / 200;
            switch (ant.state) {
                case SEARCH_FOOD:
                    searchFood(owner, ant, physical);
                    break;
                case BRING_FOOD_HOME:
                    bringFoodHome(owner, ant, physical);
                    break;
                case ATTACK:
                    seekDanger(owner, ant, physical);
                    break;
            }
        }
    }

    private void seekDanger(Player owner, Ant ant, Physical physical) {
        closestEnemy.reset();
        closestEnemy.enemyOf = owner;
        game.entityQueries.inRadius(physical.position.x, physical.position.y, PHEROMON_RANGE, closestEnemy);
        if (closestEnemy.best != null) {
            Vector2 targetPosition = PHYSICAL.get(closestEnemy.best).position;
            float dst = targetPosition.dst(physical.position);
            if (dst < 20) {
                getEngine().removeEntity(closestEnemy.best);
                ant.state = RECOVER_FROM_ATTACKING;
                ant.remaining = 1;
            }
            moveToward(physical, targetPosition, 2);
            return;
        }

        bestDangerTrail.reset();
        bestDangerTrail.source.set(physical.position);
        bestDangerTrail.orientation = physical.orientation;
        owner.pheromons.inRadius(physical.position.x, physical.position.y, PHEROMON_RANGE, bestDangerTrail);
        moveToOrContinue(physical, bestDangerTrail.result());
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
                ant.state = SEARCH_FOOD;
                owner.food += GATHER_AMOUNT;
                ant.distanceTravelled = 0;
            }
            if (dst < RASTER * 2) {
                moveToward(physical, position, 2);
                return;
            }
        }
        bestHomeTrail.reset();
        bestHomeTrail.source.set(physical.position);
        bestHomeTrail.orientation = physical.orientation;
        owner.pheromons.inRadius(physical.position.x, physical.position.y, PHEROMON_RANGE, bestHomeTrail);
        Vector2 target = bestHomeTrail.result();
        moveToOrContinue(physical, target);
    }

    private void moveToOrContinue(Physical physical, Vector2 target) {
        if (target == null) {
            applyMoveTime(physical);
            if (target == null) {
                applyScanOrientation(physical);
            }
        } else {
            moveToward(physical, target, 20);
        }
    }

    private void applyScanOrientation(Physical physical) {
        physical.orientation += rnd.nextFloat() - 0.3f;
    }

    private void applyMoveTime(Physical physical) {
        physical.moveTime = (rnd.nextFloat() + 1) / 3;
    }

    private void moveToward(Physical physical, Vector2 target, float precision) {
        v1.set(target);
        v1.sub(physical.position);
        v1.add(rnd.nextFloat() * precision * 2 - precision, rnd.nextFloat() * precision * 2 - precision);
        if (v1.len2() > 0) {
            physical.orientation = v1.angleRad();
            physical.moveTime = v1.len() / physical.speed;
        }
    }

    private void searchFood(Player owner, Ant ant, Physical physical) {
        ant.state = SEARCH_FOOD;
        for (Entity f: food) {
            Physical fpos = PHYSICAL.get(f);
            Vector2 position = fpos.position;
            Food food = FOOD.get(f);
            if (food.amount > 0) {
                if (position.dst(physical.position) < 20) {
                    food.amount = Math.max(0, food.amount - GATHER_AMOUNT);
                    ant.state = GATHER_FOOD;
                    ant.remaining = 3;
                }
                if (position.dst(physical.position) < RASTER) {
                    moveToward(physical, position, 2);
                    return;
                }
            }
        }
        bestFoodTrail.reset();
        bestFoodTrail.source.set(physical.position);
        bestFoodTrail.orientation = physical.orientation;
        owner.pheromons.inRadius(physical.position.x, physical.position.y, PHEROMON_RANGE, bestFoodTrail);
        Vector2 target = bestFoodTrail.result();
        moveToOrContinue(physical, rnd.nextFloat() < 0.01f ? null : target);
    }

    public Wrapper getOrCreatePheromon(Player owner, float x, float y) {
        float _x = x;
        float _y = y;
        x = (float) Math.floor(x / RASTER + .5f) * RASTER;
        y = (float) Math.floor(y / RASTER + .5f) * RASTER;
        _x -= x;
        _y -= y;
        Array<Pheromon> values = owner.pheromons.getValues(x, y);
        if (values.size > 0) {
            return new Wrapper(values.get(0), (float) Math.sqrt(_x * _x + _y * _y));
        }
        Pheromon pheromon = new Pheromon();
        owner.pheromons.addValue(x, y, pheromon);
        return new Wrapper(pheromon, (float) Math.sqrt(_x * _x + _y * _y));
    }

    public static class BestFoodTrail extends BestTrail<Pheromon> {
        @Override
        protected float eval(Pheromon e) {
            return e.foodPath - e.danger;
        }
    }

    public static class BestHomeTrail extends BestTrail<Pheromon> {
        @Override
        protected float eval(Pheromon e) {
            return e.homePath - e.danger;
        }
    }

    public static class BestDangerTrail extends BestTrail<Pheromon> {
        @Override
        protected float eval(Pheromon e) {
            return e.danger * e.homePath;
        }
    }

    public static abstract class BestTrail<T> implements LocationQueries.Callback<T> {
        final Vector2 bestTarget = new Vector2();
        final Vector2 source = new Vector2();
        final Vector2 v1 = new Vector2();
        final Vector2 v2 = new Vector2();
        float orientation;
        float sum;

        public void reset() {
            bestTarget.setZero();
            sum = 0;
        }

        public Vector2 result() {
            return sum > 0 ? bestTarget.scl(1 / sum) : null;
        }

        @Override
        public boolean accept(float x, float y, T e) {
            float res = eval(e);
            v2.set(Vector2.X);
            float dot = v1.set(x, y).sub(source).nor().dot(v2.rotateRad(orientation));
            res *= (dot + 1.7f);
            bestTarget.add(x * res, y * res);
            sum += res;
            return false;
        }

        protected abstract float eval(T e);
    }

    private static class Wrapper {
        final Pheromon pheromon;
        final float dst;

        private Wrapper(Pheromon pheromon, float dst) {
            this.pheromon = pheromon;
            this.dst = dst;
        }
    }

    private class ClosestEnemy extends Closest {
        Player enemyOf;

        @Override
        protected boolean matches(Entity entity) {
            return ANT.has(entity) && PHYSICAL.has(entity) && ANT.get(entity).owner != enemyOf;
        }
    }

    private abstract class Closest implements LocationQueries.Callback<Entity> {
        final Vector2 source = new Vector2();
        float bestDistance;
        Entity best;

        public void reset() {
            bestDistance = Float.POSITIVE_INFINITY;
            best = null;
        }

        @Override
        public boolean accept(float x, float y, Entity e) {
            if (!matches(e)) {
                return false;
            }
            float dst = source.dst(x, y) + rnd.nextFloat() * MathUtils.FLOAT_ROUNDING_ERROR;
            if (dst < bestDistance) {
                bestDistance = dst;
                best = e;
            }
            return false;
        }

        protected abstract boolean matches(Entity entity);
    }
}
