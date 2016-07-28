package org.bytekeeper;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Created by dante on 23.07.16.
 */
public class LocationQueries<T> {
    private static final float BLOCK_SIZE = 200;

    private Array<Array<Array<Wrapper<T>>>> values = new Array<>();

    public void addValue(float x, float y, T value) {
        Array<Wrapper<T>> block = getBlock(x, y, true);
        block.add(new Wrapper<>(value, x, y));
    }

    public void removeValue(float x, float y, T value) {
        Array<Wrapper<T>> block = getBlock(x, y, false);
        for (int i = block.size - 1; i >= 0; i--) {
            Wrapper<T> wrapper = block.get(i);
            if (wrapper.value == value && wrapper.position.epsilonEquals(x, y, 0.0000001f)) {
                block.removeIndex(i);
                return;
            }
        }
    }

    public void inRadius(float x, float y, float r, Callback callback) {
        for (float a = x - r; a <= x + r; a += BLOCK_SIZE) {
            for (float b = y - r; b <= y + r; b += BLOCK_SIZE) {
                Array<Wrapper<T>> block = getBlock(a, b, false);
                if (block != null && inRadius(block, x, y, r, callback)) {
                    return;
                }
            }
        }
    }

    private boolean inRadius(Array<Wrapper<T>> block, float x, float y, float r, Callback callback) {
        for (int i = block.size - 1; i >= 0; i--) {
            Wrapper<T> item = block.get(i);
            if (item.position.dst(x, y) <= r) {
                if (callback.accept(item.position.x, item.position.y, item.value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Array<Wrapper<T>> getBlock(float x, float y, boolean createIfMissing) {
        int row = (int) (y / BLOCK_SIZE) * 2;
        if (row < 0) {
            row = -row - 1;
        }
        int col = (int) ((x / BLOCK_SIZE) * 2);
        if (col < 0) {
            col = -col - 1;
        }
        Array<Array<Wrapper<T>>> column = null;
        if (values.size > row) {
            column = values.get(row);
        } else if (createIfMissing) {
            values.setSize(row + 1);
        } else {
            return null;
        }
        if (column == null) {
            column = new Array<>();
            values.set(row, column);
        }
        Array<Wrapper<T>> entries = null;
        if (column.size > col) {
            entries = column.get(col);
        } else if (createIfMissing) {
            column.setSize(col + 1);
        } else {
            return null;
        }
        if (entries == null) {
            entries = new Array<>();
            column.set(col, entries);
        }
        return entries;
    }

    public interface Callback<T> {
        boolean accept(float x, float y, T e);
    }

    private static class Wrapper<T> {
        final Vector2 position = new Vector2();
        final T value;

        private Wrapper(T value, float x, float y) {
            this.value = value;
            position.set(x, y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Wrapper<?> wrapper = (Wrapper<?>) o;

            if (!position.equals(wrapper.position)) return false;
            return value.equals(wrapper.value);

        }

        @Override
        public int hashCode() {
            int result = position.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }
    }

}
