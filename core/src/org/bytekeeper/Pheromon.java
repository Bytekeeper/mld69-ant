package org.bytekeeper;

/**
 * Created by dante on 24.07.16.
 */
public class Pheromon {
    public float homePath;
    public float foodPath;
    public float danger;

    @Override
    public String toString() {
        return "Pheromon{" +
                "homePath=" + homePath +
                ", foodPath=" + foodPath +
                ", danger=" + danger +
                '}';
    }
}
