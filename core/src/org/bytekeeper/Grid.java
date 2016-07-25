package org.bytekeeper;

import com.badlogic.gdx.math.Vector2;

/**
 * Created by dante on 23.07.16.
 */
public class Grid<T> {
    public Node<T> root = new Node<>();

    public static class Node<T> {
        public Node[][] children;
        public float x, y;
        public T value;

        public void addValue(float x, float y, T value) {
            if (this.value == null) {
                this.value = value;
                this.x = x;
                this.y = y;
                return;
            }
            if (children == null) {
                children = new Node[][]{
                        {new Node(), new Node()},
                        {new Node(), new Node()},
                };
            }
            int c = x < this.x ? 0 : 1;
            int r = y < this.y ? 0 : 1;
            children[c][r].addValue(x, y, value);
        }

        public void inRadius(float x, float y, float r, ElementCallback callback) {
            if (value == null) {
                return;
            }
            if (Vector2.dst(x, y, this.x, this.y) <= r) {
                if (callback.accept(this.x, this.y, value)) {
                    return;
                }
            }
            if (children == null) {
                return;
            }
            inRadiusColumn(x, y, r, callback);
        }

        private void inRadiusColumn(float x, float y, float r, ElementCallback callback) {
            if (x - r <= this.x) {
                inRadiusRow(0, x, y, r, callback);
            }
            if (x + r >= this.x) {
                inRadiusRow(1, x, y, r, callback);
            }
        }

        private void inRadiusRow(int col, float x, float y, float r, ElementCallback callback) {
            if (y - r <= this.y) {
                children[col][0].inRadius(x, y, r, callback);
            }
            if (y + r >= this.y) {
                children[col][1].inRadius(x, y, r, callback);
            }
        }
    }

    public interface ElementCallback<T> {
        boolean accept(float x, float y, T e);
    }
}
