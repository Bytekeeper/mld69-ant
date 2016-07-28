package org.bytekeeper;

import com.badlogic.ashley.core.ComponentMapper;

import static com.badlogic.ashley.core.ComponentMapper.getFor;

/**
 * Created by dante on 23.07.16.
 */
public class Components {
    public static final ComponentMapper<AIState> AI_STATE = getFor(AIState.class);
    public static final ComponentMapper<Ant> ANT = getFor(Ant.class);
    public static final ComponentMapper<Base> BASE = getFor(Base.class);
    public static final ComponentMapper<Food> FOOD = getFor(Food.class);
    public static final ComponentMapper<Larva> LARVA = getFor(Larva.class);
    public static final ComponentMapper<Player> PLAYER = getFor(Player.class);
    public static final ComponentMapper<Physical> PHYSICAL = getFor(Physical.class);
}
