package org.bytekeeper;

import com.badlogic.ashley.core.ComponentMapper;

import static com.badlogic.ashley.core.ComponentMapper.getFor;

/**
 * Created by dante on 23.07.16.
 */
public class Components {
    public static final ComponentMapper<Physical> POSITION = getFor(Physical.class);
    public static final ComponentMapper<AntAI> ANT_AI = getFor(AntAI.class);
    public static final ComponentMapper<Food> FOOD = getFor(Food.class);
    public static final ComponentMapper<Larva> LARVA = getFor(Larva.class);
}
