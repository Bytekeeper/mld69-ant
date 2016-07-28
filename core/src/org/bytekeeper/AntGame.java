package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
    public Stage stage;
    public Player humanPlayer;
    private Label foodLabel;
    private Label antAmountLabel;
    private Skin skin;
    private TextButton buildWorkerButton;
    private AntSystem antAISystem;
    public boolean paused = true;

    private final Vector2 v1 = new Vector2();
    private WinConditionSystem winConditionSystem;

    @Override
	public void create () {
        viewport = new FitViewport(1024, 768);
        viewport.apply();
        camera = viewport.getCamera();
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
                if (humanPlayer.food > COST_GATHERER) {
                    humanPlayer.food -= COST_GATHERER;
                    spawnLarva(humanPlayer, Buildable.GATHERER);
                }
            }
        });
        bottomRow.add(buildWorkerButton).pad(10);

        tutorial1();

        engine = new Engine();
        engine.addSystem(new AntMoveSystem(this));
        antAISystem = new AntSystem(this);
        engine.addSystem(antAISystem);
        engine.addSystem(new LarvaSystem(this));
        engine.addSystem(new FoodSystem(this));
        engine.addSystem(new WorldRenderSystem(this));
        winConditionSystem = new WinConditionSystem(this);
        engine.addSystem(winConditionSystem);

        humanPlayer = createPlayer();
        humanPlayer.color.set(1, 1, 1, 1);

        for (int i = 0; i < 20; i++) {
            placeFood();
        }
        spawnStartBase(Vector2.Zero, humanPlayer);
        Color[] colors = new Color[] {Color.RED, Color.TEAL, Color.BLUE, Color.GREEN, Color.MAGENTA};
        for (int i = 0; i < 4; i++) {
            Player ai = createPlayer();
            ai.color.set(colors[i]);
            v1.set(Vector2.X).scl(500 + rnd.nextFloat() * 400).rotateRad(MathUtils.PI2 * i / 4 + rnd.nextFloat());
            spawnStartBase(v1, ai);
        }
    }

    private Player createPlayer() {
        Player player = new Player();
        player.food = 100;

        Entity playerEntity = new Entity();
        playerEntity.add(player);
        engine.addEntity(playerEntity);
        return player;
    }

    private void spawnStartBase(Vector2 position, Player player) {
        for (int i = 0; i < 4; i++) {
            spawnWorkerAnt(player, position);
        }
        spawnBase(player, position);
    }

    private void tutorial1() {
        addTutorialDialog(new Runnable() {
            @Override
            public void run() {
                tutorial2();
            }
        });
    }

    private Dialog addTutorialDialog(Runnable nextRunnable) {
        Dialog tutorialDialog = new DialogWithRunnable("Tutorial", skin);

        if (nextRunnable != null) {
            tutorialDialog.button("Next", nextRunnable);
        }
        tutorialDialog.button("Start", new Runnable() {
            @Override
            public void run() {
                endTutorial();
            }
        });
        tutorialDialog.setPosition(20, 400);

        stage.addActor(tutorialDialog);
        return tutorialDialog;
    }

    private void tutorial2() {
        addTutorialDialog(null);
    }

    private void endTutorial() {
        paused = false;
    }

    private void spawnLarva(Player owner, Buildable type) {
        Entity larvaEntity = new Entity();
        Physical physical = new Physical();
        physical.position.set(rnd.nextFloat() * 50 - 25, rnd.nextFloat() * 50 - 25);
        Larva larva = new Larva();
        larva.buildTimeRemaining = 10;
        larva.owner = owner;
        larva.morphInto = type;
        larvaEntity.add(larva);
        larvaEntity.add(physical);
        engine.addEntity(larvaEntity);
    }

    public void spawnWorkerAnt(Player owner, Vector2 position) {
        Entity entity = new Entity();
        Ant ant = new Ant();
        ant.owner = owner;
        ant.canGatherFood = true;
        Physical physical = new Physical();
        physical.orientation = rnd.nextFloat() * MathUtils.PI2 - MathUtils.PI;
        physical.position.set(position);

        entity.add(ant);
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

    private void spawnBase(Player owner, Vector2 position) {
        Entity storage = new Entity();
        Base base = new Base();
        base.owner = owner;
        Physical physical = new Physical();
        physical.position.set(position);

        storage.add(physical);
        storage.add(base);
        engine.addEntity(storage);
    }

    @Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float deltaTime = Gdx.graphics.getDeltaTime();

        if (!paused) {

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
        }
        engine.update(deltaTime);

        checkWin();

        stage.act(deltaTime);


        foodLabel.setText((int) humanPlayer.food + " rations");
        antAmountLabel.setText(humanPlayer.antAmount + " ants");
        buildWorkerButton.setDisabled(humanPlayer.food < COST_GATHERER);

        stage.draw();
	}

    private void checkWin() {
        if (paused) {
            return;
        }
        if (winConditionSystem.wonBy != null) {
            paused = true;
            DialogWithRunnable dialog = new DialogWithRunnable("Game ended", skin);
            dialog.button("Close", new Runnable() {
                @Override
                public void run() {
                    Gdx.app.exit();
                }
            });
            stage.addActor(dialog);
        }
    }

    @Override
	public void dispose () {
	}

	private static class DialogWithRunnable extends Dialog {

        public DialogWithRunnable(String title, Skin skin) {
            super(title, skin);
        }

        @Override
        protected void result(Object object) {
            ((Runnable) object).run();
        }
    }
}
