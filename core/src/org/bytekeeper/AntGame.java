package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class AntGame extends ApplicationAdapter {
    public static final float COST_GATHERER = 5;
    public static final int SCROLL_SPEED = 300;
    private RandomXS128 rnd = new RandomXS128();
	private Engine engine;
    public Viewport viewport;
    public Camera camera;
    public Grid<Pheromon> grid = new Grid<>();
    public Stage stage;
    public Player player;
    private Label foodLabel;
    private Label antAmountLabel;
    private Skin skin;
    private TextButton buildWorkerButton;
    private AntAISystem antAISystem;

    @Override
	public void create () {
        viewport = new FitViewport(1024, 768);
        viewport.apply();
        camera = viewport.getCamera();
        player = new Player();
        player.food = 100;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

//        root.debug();
        foodLabel = new Label("x rations", skin);
        antAmountLabel = new Label("x ants", skin);

        Table topRow = new Table();
        root.add(topRow).pad(10).top().left().expand();
        topRow.add(foodLabel).pad(10);
        topRow.add(antAmountLabel).pad(10);
        root.row();

        Table bottomRow = new Table();
        root.add(bottomRow).pad(10).bottom().left().expand();
        buildWorkerButton = new TextButton("Spawn gatherer", skin);
        buildWorkerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (player.food > COST_GATHERER) {
                    player.food -= COST_GATHERER;
                    spawnLarva(Buildable.GATHERER);
                }
            }
        });
        bottomRow.add(buildWorkerButton).pad(10);

        engine = new Engine();
        engine.addSystem(new AntMoveSystem());
        antAISystem = new AntAISystem(this);
        engine.addSystem(antAISystem);
        engine.addSystem(new LarvaSystem(this));
        engine.addSystem(new FoodSystem());
        engine.addSystem(new WorldRenderSystem(this));

        for (int i = 0; i < 4; i++) {
            addAnt(Vector2.Zero);
        }
        for (int i = 0; i < 20; i++) {
            placeFood();
        }
        addBase();
    }

    private void spawnLarva(Buildable type) {
        Entity larvaEntity = new Entity();
        Physical physical = new Physical();
        physical.position.set(rnd.nextFloat() * 50 - 25, rnd.nextFloat() * 50 - 25);
        larvaEntity.add(physical);
        Larva larva = new Larva();
        larva.buildTimeRemaining = 10;
        larvaEntity.add(larva);
        engine.addEntity(larvaEntity);
    }

    public void addAnt(Vector2 position) {
        Entity entity = new Entity();
        entity.add(new AntAI());
        Physical physical = new Physical();
        physical.orientation = rnd.nextFloat() * MathUtils.PI2 - MathUtils.PI;
        physical.position.set(position);
        entity.add(physical);
        engine.addEntity(entity);
    }

    private void placeFood() {
        Entity food = new Entity();
        Physical physical = new Physical();
        physical.position.set(rnd.nextFloat() * 2000 - 1000, rnd.nextFloat() * 2000 - 1000);
        Food food1 = new Food();
        food1.amount = 200 + rnd.nextFloat() * 100;

        food.add(physical);
        food.add(food1);
        engine.addEntity(food);
    }

    private void addBase() {
        Entity storage = new Entity();
        Base base = new Base();

        storage.add(new Physical());
        storage.add(base);
        engine.addEntity(storage);
    }

    @Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float deltaTime = Gdx.graphics.getDeltaTime();

        if (Gdx.input.getX() < 20) {
            camera.translate(-deltaTime * SCROLL_SPEED, 0, 0);
        }
        if (Gdx.input.getX() > 1004) {
            camera.translate(deltaTime * SCROLL_SPEED, 0, 0);
        }
        if (Gdx.input.getY() < 20) {
            camera.translate(0, deltaTime * SCROLL_SPEED, 0);
        }
        if (Gdx.input.getY() > 748) {
            camera.translate(0, -deltaTime * SCROLL_SPEED, 0);
        }
        camera.update();
        stage.act(deltaTime);

        engine.update(deltaTime);

        foodLabel.setText((int) player.food + " rations");
        antAmountLabel.setText(antAISystem.antAmount + " ants");
        buildWorkerButton.setDisabled(player.food < COST_GATHERER);

        stage.draw();
	}
	
	@Override
	public void dispose () {
	}
}
