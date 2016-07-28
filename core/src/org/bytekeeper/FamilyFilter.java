package org.bytekeeper;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;

/**
 * Created by dante on 28.07.16.
 */
public class FamilyFilter implements LocationQueries.Callback<Entity> {
    private final Family family;
    private final LocationQueries.Callback<Entity> delegate;

    public FamilyFilter(Family family, LocationQueries.Callback<Entity> delegate) {
        this.family = family;
        this.delegate = delegate;
    }

    @Override
    public boolean accept(float x, float y, Entity e) {
        if (family.matches(e) && delegate.accept(x, y, e)) {
            return true;
        }
        return false;
    }
}
