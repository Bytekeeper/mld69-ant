package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import static org.bytekeeper.Components.LARVA;
import static org.bytekeeper.Components.POSITION;

/**
 * Created by dante on 23.07.16.
 */
public class WorldRenderSystem extends EntitySystem {
    private final AntGame antGame;
    private final Texture particle;
    private final Texture grass0;
    private final TextureRegion ant;
    private final Texture cakeTexture;
    private final Texture baseTexture;
    private final Texture larvaTexture;
    private Iterable<Entity> larvas;
    private Iterable<Entity> ants;
    private Iterable<Entity> foods;
    private Iterable<Entity> bases;
    private SpriteBatch batch;
    private Grid.ElementCallback renderCallback = new RenderCallBack();

    public WorldRenderSystem(AntGame antGame) {
        this.antGame = antGame;
        batch = new SpriteBatch();
        particle = new Texture(Gdx.files.classpath("particle.png"), true);
        grass0 = new Texture(Gdx.files.internal("tileable_grass_00.png"), true);
        grass0.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.MipMapLinearLinear);
        grass0.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        ant = new TextureRegion(new Texture(Gdx.files.internal("ant.png"), true));
        larvaTexture = new Texture(Gdx.files.internal("larva.png"), true);
        cakeTexture = new Texture(Gdx.files.internal("cake.png"), true);
        baseTexture = new Texture(Gdx.files.internal("base.png"), true);
    }

    @Override
    public void addedToEngine(Engine engine) {
        ants = engine.getEntitiesFor(Family.all(Physical.class, AntAI.class).get());
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
            batch.draw(cakeTexture, position.x - 20, position.y - 20, 40, 40);
        }

        for (Entity e: bases) {
            Physical physical = POSITION.get(e);
            Vector2 position = physical.position;
            batch.draw(baseTexture, position.x - 60, position.y - 60, 120, 120);
        }

        for (Entity e: larvas) {
            Physical physical = POSITION.get(e);
            Vector2 position = physical.position;
            Larva larva = LARVA.get(e);
            float scale = (float) (20 + Math.sin(larva.buildTimeRemaining * MathUtils.PI2) * 5);
            batch.draw(larvaTexture, position.x - scale / 2, position.y - scale / 2, scale, scale);
        }

        batch.setColor(0.7f, 0.5f, 0.2f, 1);

        for (Entity e: ants) {
            Physical physical = POSITION.get(e);
            Vector2 position = physical.position;
            batch.draw(ant, position.x - 10, position.y - 10, 10, 10, 20, 20, 1, 1, MathUtils.radiansToDegrees * physical.orientation);
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