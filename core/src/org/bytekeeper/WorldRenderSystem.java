package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import static org.bytekeeper.Components.*;

/**
 * Created by dante on 23.07.16.
 */
public class WorldRenderSystem extends EntitySystem {
    private final AntGame antGame;
    private final Texture particle;
    private final Texture grass0;
    private final TextureRegion antTexture;
    private final Texture cakeTexture;
    private final Texture baseTexture;
    private final Texture larvaTexture;
    private Iterable<Entity> larvas;
    private Iterable<Entity> ants;
    private Iterable<Entity> foods;
    private Iterable<Entity> bases;
    private SpriteBatch batch;

    private final Vector2 v1 = new Vector2();
    private final Color c1 = new Color();

    public WorldRenderSystem(AntGame antGame) {
        this.antGame = antGame;
        batch = new SpriteBatch();
        particle = new Texture(Gdx.files.classpath("particle.png"), true);
        grass0 = new Texture(Gdx.files.internal("tileable_grass_00.png"), true);
        grass0.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.MipMapLinearLinear);
        grass0.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        antTexture = new TextureRegion(new Texture(Gdx.files.internal("ant.png"), true));
        larvaTexture = new Texture(Gdx.files.internal("larva.png"), true);
        cakeTexture = new Texture(Gdx.files.internal("cake.png"), true);
        baseTexture = new Texture(Gdx.files.internal("base.png"), true);
    }

    @Override
    public void addedToEngine(Engine engine) {
        ants = engine.getEntitiesFor(Family.all(Physical.class, Ant.class).get());
        larvas = engine.getEntitiesFor(Family.all(Physical.class, Larva.class).get());
        foods = engine.getEntitiesFor(Family.all(Physical.class, Food.class).get());
        bases = engine.getEntitiesFor(Family.all(Physical.class, Base.class).get());
    }

    @Override
    public void update(float deltaTime) {
        Camera camera = antGame.viewport.getCamera();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float x = camera.position.x;
        float y = camera.position.y;
        float u = x / 1024;
        float v = -y / 1024;

        batch.setColor(1, 1, 1, 1);

        batch.draw(grass0, x - 512, y - 768/2, 1024, 768, u, v, u + 1, v - 768/1024f);

        for (Entity e: foods) {
            Physical physical = POSITION.get(e);
            Vector2 position = physical.position;
            Food food = FOOD.get(e);
            batch.draw(cakeTexture, position.x - 20, position.y - 20, 40, 40);
            float g = Math.min(1, food.amount / 300);
            batch.setColor(1 - g, g, 0, 1);
            batch.draw(particle, position.x + 10, position.y - 10, 15, 15);
            batch.setColor(1, 1, 1, 1);
        }

        for (Entity e: bases) {
            Physical physical = POSITION.get(e);
            Vector2 position = physical.position;
            batch.setColor(BASE.get(e).owner.color);
            batch.draw(baseTexture, position.x - 60, position.y - 60, 120, 120);
        }

        for (Entity e: larvas) {
            Physical physical = POSITION.get(e);
            Vector2 position = physical.position;
            Larva larva = LARVA.get(e);
            float scale = (float) (20 + Math.sin(larva.buildTimeRemaining * MathUtils.PI2) * 5);
            batch.setColor(larva.owner.color);
            batch.draw(larvaTexture, position.x - scale / 2, position.y - scale / 2, scale, scale);
        }

        for (Entity e: ants) {
            Ant ant = ANT.get(e);
            Physical physical = POSITION.get(e);

            if (State.GATHER_FOOD.equals(ant.state)) {
                v1.set(Vector2.X).rotateRad(physical.orientation + MathUtils.PI).scl((float) (3 * Math.sin(ant.remaining * MathUtils.PI2 * 2))).
                        add(physical.position);
            } else {
                v1.set(physical.position);
            }
            c1.set(ant.owner.color).
                    mul(0.7f, 0.5f, 0.2f, 1);
            batch.setColor(c1);
            batch.draw(antTexture, v1.x - 10, v1.y - 10, 10, 10, 20, 20, 1, 1, MathUtils.radiansToDegrees * physical.orientation);
        }
        batch.end();
    }

    private class RenderCallBack implements Grid.ElementCallback<Pheromon> {
        private float maxFood = 1;
        private float maxHome = 1;

        @Override
        public boolean accept(float x, float y, Pheromon p) {
            maxFood = Math.max(p.foodPath, maxFood);
            maxHome = Math.max(p.homePath, maxHome);
            batch.setColor(p.foodPath / maxFood, p.homePath / maxHome, 0, 1);
            batch.draw(particle, x, y, 5, 5);
            return false;
        }
    }
}