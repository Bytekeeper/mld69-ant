package org.bytekeeper;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
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

import static org.bytekeeper.Components.PHYSICAL;
import static org.bytekeeper.Components.PLAYER;
import static org.bytekeeper.State.ATTACK;

public class AntGame extends ApplicationAdapter {
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
    private TextButton buildWarriorButton;
    public boolean paused = true;
    public LocationQueries<Entity> entityQueries = new LocationQueries<>();

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
        buildWorkerButton = new TextButton(String.format("Spawn gatherer (%.0f)", AntType.GATHERER.cost), skin);
        buildWorkerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (humanPlayer.food >= AntType.GATHERER.cost) {
                    spawnLarva(humanPlayer, AntType.GATHERER, Vector2.Zero);
                }
            }
        });
        bottomRow.add(buildWorkerButton).pad(10);
        buildWarriorButton = new TextButton(String.format("Spawn warrior (%.0f)", AntType.WARRIOR.cost), skin);
        buildWarriorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (humanPlayer.food >= AntType.WARRIOR.cost) {
                    spawnLarva(humanPlayer, AntType.WARRIOR, Vector2.Zero);
                }
            }
        });
        bottomRow.add(buildWarriorButton).pad(10);

        tutorial1();

        engine = new Engine();
        engine.addSystem(new AntSystem(this));
        engine.addSystem(new AntMoveSystem(this));
        engine.addSystem(new AIPlayerSystem(this));
        engine.addSystem(new LarvaSystem(this));
        engine.addSystem(new FoodSystem(this));
        engine.addSystem(new WorldRenderSystem(this));
        winConditionSystem = new WinConditionSystem(this);
        engine.addSystem(winConditionSystem);
        engine.addEntityListener(Family.all(Physical.class).get(), new EntityListener() {
            @Override
            public void entityAdded(Entity entity) {
                Vector2 position = PHYSICAL.get(entity).position;
                entityQueries.addValue(position.x, position.y, entity);
            }

            @Override
            public void entityRemoved(Entity entity) {
                Vector2 position = PHYSICAL.get(entity).position;
                entityQueries.removeValue(position.x, position.y, entity);
            }
        });

        humanPlayer = PLAYER.get(createPlayer());
        humanPlayer.color.set(1, 1, 1, 1);

        for (int i = 0; i < 20; i++) {
            placeFood();
        }
        spawnStartBase(Vector2.Zero, humanPlayer);
        Color[] colors = new Color[] {Color.RED, Color.TEAL, Color.BLUE, Color.GREEN, Color.MAGENTA};
        for (int i = 0; i < 4; i++) {
            Entity ai = createPlayer();
            ai.add(new AIState());
            Player aiPlayer = PLAYER.get(ai);
            aiPlayer.color.set(colors[i]);
            v1.set(Vector2.X).scl(500 + rnd.nextFloat() * 400).rotateRad(MathUtils.PI2 * i / 4 + rnd.nextFloat());
            spawnStartBase(v1, aiPlayer);
        }
    }

    private Entity createPlayer() {
        Player player = new Player();
        player.food = 100;

        Entity playerEntity = new Entity();
        playerEntity.add(player);
        engine.addEntity(playerEntity);
        return playerEntity;
    }

    private void spawnStartBase(Vector2 position, Player player) {
        for (int i = 0; i < 4; i++) {
            spawnWorkerAnt(player, position);
        }
        spawnBase(player, position);
    }

    private void tutorial1() {
        Dialog dialog = addTutorialDialog(new Runnable() {
            @Override
            public void run() {
                tutorial2();
            }
        });
        dialog.text("Starting with a base and 4 ants, ");
        dialog.getContentTable().row();
        dialog.text("defeat the enemy ant colonies!");
        dialog.getContentTable().row();
        dialog.text("Build gatherers to collect food.");
        dialog.getContentTable().row();
        dialog.text("Build warriors to attack and defend.");
        dialog.pack();
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
        Dialog dialog = addTutorialDialog(null);
        dialog.text("The cake is not a lie, but a food source!");
        dialog.getContentTable().row();
        dialog.text("The colored dots beside show how much food is left.");
        dialog.getContentTable().row();
        dialog.text("Your ants consume food as well, so keep some around.");
        dialog.pack();
    }

    private void endTutorial() {
        paused = false;
    }

    public void spawnLarva(Player owner, AntType type, Vector2 position) {
        owner.food -= type.cost;
        Entity larvaEntity = new Entity();
        Physical physical = new Physical();
        physical.position.set(rnd.nextFloat() * 50 - 25, rnd.nextFloat() * 50 - 25).add(position);
        Larva larva = new Larva();
        larva.buildTimeRemaining = 10;
        larva.owner = owner;
        larva.morphInto = type;
        larvaEntity.add(larva);
        larvaEntity.add(physical);
        engine.addEntity(larvaEntity);
    }

    public void spawnWorkerAnt(Player owner, Vector2 position) {
        Ant ant = spawnAnt(owner, position, 80);
        ant.state = State.SEARCH_FOOD;
        ant.type = AntType.GATHERER;
    }

    private Ant spawnAnt(Player owner, Vector2 position, float speed) {
        Ant ant = new Ant();
        ant.owner = owner;
        Physical physical = new Physical();
        physical.orientation = rnd.nextFloat() * MathUtils.PI2 - MathUtils.PI;
        physical.position.set(position);
        physical.speed = speed;

        Entity entity = new Entity();
        entity.add(ant);
        entity.add(physical);
        engine.addEntity(entity);

        return ant;
    }

    public void spawnWarriorAnt(Player owner, Vector2 position) {
        Ant ant = spawnAnt(owner, position, 100);
        ant.state = ATTACK;
        ant.type = AntType.WARRIOR;
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
        buildWorkerButton.setDisabled(humanPlayer.food < AntType.GATHERER.cost);
        buildWarriorButton.setDisabled(humanPlayer.food < AntType.WARRIOR.cost);

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
            if (winConditionSystem.wonBy == humanPlayer) {
                dialog.text("You have won!!!");
            } else {
                dialog.text("You lost :(");
            }
            dialog.show(stage);
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
