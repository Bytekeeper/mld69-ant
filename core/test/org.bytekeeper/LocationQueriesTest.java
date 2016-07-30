package org.bytekeeper;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by dante on 28.07.16.
 */
public class LocationQueriesTest {
    private LocationQueries<String> sut = new LocationQueries<>();
    private RandomXS128 rnd = new RandomXS128();

    @Test
    public void shouldFindItemsOnBounds() {
        // GIVEN
        sut.addValue(-1000, 0, "a");
        sut.addValue(1000, 0, "b");

        Collector collector = new Collector();

        // WHEN
        sut.inRadius(0, 0, 1000, collector);

        // THEN
        assertThat(collector.result, hasItems(new X(new Vector2(-1000, 0), "a"),
                new X(new Vector2(1000, 0), "b")));
    }

    @Test
    public void shouldNotFindItemsOutsideBounds() {
        // GIVEN
        sut.addValue(-1000.1f, 0, "a");
        sut.addValue(1000.1f, 0, "b");

        Collector collector = new Collector();

        // WHEN
        sut.inRadius(0, 0, 1000, collector);

        // THEN
        assertThat(collector.result, CoreMatchers.is(Collections.<X> emptyList()));
    }

    @Test
    public void shouldFindItemsInSameSpot() {
        // GIVEN
        for (int i = 0; i < 100; i++) {
            sut.addValue(-1000, 1000, "a");
        }

        Collector collector = new Collector();

        // WHEN
        sut.inRadius(-1000, 1000, 0, collector);

        // THEN
        assertThat(collector.result.size(), is(100));
    }


    @Test
    public void shouldFindItemInNegativeLocation() {
        // GIVEN
        sut.addValue(-1000, -1000, "a");
        sut.addValue(1000, -1000, "b");

        Collector collector = new Collector();

        // WHEN
        sut.inRadius(-500, -1000, 500, collector);

        // THEN
        assertThat(collector.result, is(Collections.singletonList(new X(new Vector2(-1000, -1000), "a"))));
    }

    @Test
    public void shouldNegativeYItems() {
        // GIVEN
        sut.addValue(200, -210, "a");
        sut.addValue(-200, -190, "b");

        Collector collector = new Collector();

        // WHEN
        sut.inRadius(0, 0, 1000, collector);

        // THEN
        assertThat(collector.result, hasItems(new X(new Vector2(200, -210), "a"), new X(new Vector2(-200, -190), "b")));
    }

    @Test
    public void shouldGetAllItemsInRadius() {
        // GIVEN
        for (int i = 0; i < 100; i++) {
            float x = 200 + rnd.nextFloat() * 1000 - 500;
            float y = 200 + rnd.nextFloat() * 1000 - 500;
            sut.addValue(x, y, "a");
        }

        Collector collector = new Collector();

        // WHEN
        sut.inRadius(500, 500, 1400, collector);

        // THEN
        assertThat(collector.result.size(), is(100));
    }


    private static class Collector implements LocationQueries.Callback<String> {
        List<X> result = new ArrayList<>();

        @Override
        public boolean accept(float x, float y, String e) {
            result.add(new X(new Vector2(x, y), e));
            return false;
        }


    }

    private static class X {
        public final Vector2 pos;
        public final String value;

        private X(Vector2 pos, String value) {
            this.pos = pos;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            X x = (X) o;

            if (!pos.equals(x.pos)) return false;
            return value.equals(x.value);

        }

        @Override
        public int hashCode() {
            int result = pos.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "X{" +
                    "pos=" + pos +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}