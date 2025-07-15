package com.osmfogmap.KDtree;

import smile.neighbor.Neighbor;

class MyNeighborBuilder<K, V> implements Comparable<MyNeighborBuilder<K,V>> {
    /**
     * The key of neighbor.
     */
    K key;
    /**
     * The data object of neighbor. It may be same as the key object.
     */
    V value;
    /**
     * The index of neighbor object in the dataset.
     */
    int index;
    /**
     * The distance between the query and the neighbor.
     */
    double distance;

    /**
     * Constructor.
     */
    public  MyNeighborBuilder() {
        this.index = -1;
        this.distance = Double.MAX_VALUE;
    }

    /**
     * Constructor.
     * @param key the key of neighbor.
     * @param value the value of neighbor.
     * @param index the index of neighbor object in the dataset.
     * @param distance the distance between the query and the neighbor.
     */
    public  MyNeighborBuilder(K key, V value, int index, double distance) {
        this.key = key;
        this.value = value;
        this.index = index;
        this.distance = distance;
    }

    /** Creates a neighbor object. */
    public Neighbor<K, V> toNeighbor() {
        return new Neighbor<>(key, value, index, distance);
    }

    @Override
    public int compareTo(MyNeighborBuilder<K,V> o) {
        int d = Double.compare(distance, o.distance);
        // Sometime, the dataset contains duplicate samples.
        // If the distances are same, we sort by the sample index.
        return d == 0 ? index - o.index : d;
    }
}
