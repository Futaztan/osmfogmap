package com.osmfogmap;

import org.osmdroid.util.GeoPoint;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import smile.math.MathEx;
import smile.neighbor.KNNSearch;
import smile.neighbor.Neighbor;
import smile.neighbor.RNNSearch;
import smile.sort.HeapSelect;





public class MyKDtree <E> implements KNNSearch<double[], E>, RNNSearch<double[], E>, Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * The root in the KD-tree.
     */
    static class Node implements Serializable {

        /**
         * Number of dataset stored in this node.
         */
        int count;
        /**
         * The smallest point index stored in this node.
         */
        int index;
        /**
         * The index of coordinate used to split this node.
         */
        int split;
        /**
         * The cutoff used to split the specific coordinate.
         */
        double cutoff;
        /**
         * The child node which values of split coordinate is less than the cutoff value.
         */
        MyKDtree.Node lower;
        /**
         * The child node which values of split coordinate is greater than or equal to the cutoff value.
         */
        MyKDtree.Node  upper;

        /**
         * If the node is a leaf node.
         */
        boolean isLeaf() {
            return lower == null && upper == null;
        }
    }
    /**
     * The object keys.
     */
    private final double[][] keys;
    /**
     * The data objects.
     */
    private final E[] data;
    /**
     * The root node of KD-Tree.
     */
    private final MyKDtree.Node  root;
    /**
     * The index of objects in each node.
     */
    private final int[] index;

    /**
     * Constructor.
     * @param key the object keys.
     * @param data the data objects.
     */
    public MyKDtree(double[][] key, E[] data) {
        if (key.length != data.length) {
            throw new IllegalArgumentException("The array size of keys and data are different.");
        }

        this.keys = key;
        this.data = data;

        int n = key.length;
        index = new int[n];
        for (int i = 0; i < n; i++) {
            index[i] = i;
        }

        // Build the tree
        int d = keys[0].length;
        double[] lowerBound = new double[d];
        double[] upperBound = new double[d];
        root = buildNode(0, n, lowerBound, upperBound);
    }

    /**
     * Return a KD-tree of the data.
     * @param data the data objects, which are also used as key.
     * @return KD-tree.
     */
    public static MyKDtree<double[]> of(double[][] data) {
        return new MyKDtree<>(data, data);
    }

    @Override
    public String toString() {
        return "KD-Tree";
    }

    /**
     * Builds a subtree.
     * @param begin the beginning index of samples for the subtree (inclusive).
     * @param end the ending index of samples for the subtree (exclusive).
     * @param lowerBound the work space of lower bound of each dimension of samples.
     * @param upperBound the work space of upper bound of each dimension of samples.
     */
    private MyKDtree.Node buildNode(int begin, int end, double[] lowerBound, double[] upperBound) {
        int d = keys[0].length;

        // Allocate the node
        MyKDtree.Node node = new MyKDtree.Node();

        // Fill in basic info
        node.count = end - begin;
        node.index = begin;

        // Calculate the bounding box
        double[] key = keys[index[begin]];
        System.arraycopy(key, 0, lowerBound, 0, d);
        System.arraycopy(key, 0, upperBound, 0, d);

        for (int i = begin + 1; i < end; i++) {
            key = keys[index[i]];
            for (int j = 0; j < d; j++) {
                double c = key[j];
                if (lowerBound[j] > c) {
                    lowerBound[j] = c;
                }
                if (upperBound[j] < c) {
                    upperBound[j] = c;
                }
            }
        }

        // Calculate bounding box stats
        double maxRadius = -1;
        for (int i = 0; i < d; i++) {
            double radius = (upperBound[i] - lowerBound[i]) / 2;
            if (radius > maxRadius) {
                maxRadius = radius;
                node.split = i;
                node.cutoff = (upperBound[i] + lowerBound[i]) / 2;
            }
        }

        // If the max spread is 0, make this a leaf node
        if (MathEx.isZero(maxRadius, 1E-8)) {
            node.lower = node.upper = null;
            return node;
        }

        // Partition the data around the midpoint in this dimension. The
        // partitioning is done in-place by iterating from left-to-right and
        // right-to-left in the same way as quicksort.
        int i1 = begin, i2 = end - 1, size = 0;
        while (i1 <= i2) {
            boolean i1Good = (keys[index[i1]][node.split] < node.cutoff);
            boolean i2Good = (keys[index[i2]][node.split] >= node.cutoff);

            if (!i1Good && !i2Good) {
                int temp = index[i1];
                index[i1] = index[i2];
                index[i2] = temp;
                i1Good = i2Good = true;
            }

            if (i1Good) {
                i1++;
                size++;
            }

            if (i2Good) {
                i2--;
            }
        }

        // If either side is empty, make this a leaf node.
        if (size == 0 || size == node.count) {
            node.lower = node.upper = null;
            return node;
        }

        // Create the child nodes
        node.lower = buildNode(begin, begin + size, lowerBound, upperBound);
        node.upper = buildNode(begin + size, end, lowerBound, upperBound);

        return node;
    }

    /**
     * Returns the nearest neighbors of the given target starting from the give
     * tree node.
     *
     * @param q    the query key.
     * @param node the root of subtree.
     * @param neighbor the current nearest neighbor.
     */
    private void search(double[] q, MyKDtree.Node node, MyNeighborBuilder<double[], E> neighbor) {
        if (node.isLeaf()) {
            // look at all the instances in this leaf
            for (int idx = node.index; idx < node.index + node.count; idx++) {
                int i = index[idx];
                if (q != keys[i]) {
                    double distance = MathEx.distance(q, keys[i]);
                    if (distance < neighbor.distance) {
                        neighbor.index = i;
                        neighbor.distance = distance;
                    }
                }
            }
        } else {
            MyKDtree.Node nearer, further;
            double diff = q[node.split] - node.cutoff;
            if (diff < 0) {
                nearer = node.lower;
                further = node.upper;
            } else {
                nearer = node.upper;
                further = node.lower;
            }

            search(q, nearer, neighbor);

            // now look in further half
            if (neighbor.distance >= Math.abs(diff)) {
                search(q, further, neighbor);
            }
        }
    }

    /**
     * Returns (in the supplied heap object) the k nearest
     * neighbors of the given target starting from the give
     * tree node.
     *
     * @param q    the query key.
     * @param node the root of subtree.
     * @param heap the heap object to store/update the kNNs found during the search.
     */
    private void search(double[] q, MyKDtree.Node node, HeapSelect<MyNeighborBuilder<double[], E>> heap) {
        if (node.isLeaf()) {
            // look at all the instances in this leaf
            for (int idx = node.index; idx < node.index + node.count; idx++) {
                int i = index[idx];
                if (q != keys[i]) {
                    double distance = MathEx.distance(q, keys[i]);
                    MyNeighborBuilder<double[], E> datum = heap.peek();
                    if (distance < datum.distance) {
                        datum.distance = distance;
                        datum.index = i;
                        heap.heapify();
                    }
                }
            }
        } else {
            MyKDtree.Node nearer, further;
            double diff = q[node.split] - node.cutoff;
            if (diff < 0) {
                nearer = node.lower;
                further = node.upper;
            } else {
                nearer = node.upper;
                further = node.lower;
            }

            search(q, nearer, heap);

            // now look in further half
            if (heap.peek().distance >= Math.abs(diff)) {
                search(q, further, heap);
            }
        }
    }

    /**
     * Returns the neighbors in the given range of search target from the give
     * tree node.
     *
     * @param q the query key.
     * @param node the root of subtree.
     * @param radius the radius of search range from target.
     * @param neighbors the list of found neighbors in the range.
     */
    private void search(double[] q, MyKDtree.Node node, double radiusMeters, List<Neighbor<double[], E>> neighbors) {
        // Földi sugár méterben -> földgömbön mért fokra váltás
        double lat = q[0]; // szélesség
        double lon = q[1]; // hosszúság (nem kell külön most)

        // Átlagos konverzió: 1° lat ≈ 111320 m; 1° lon ≈ cos(lat) * 111320 m
        double metersPerDegreeLat = 111320.0;
        double metersPerDegreeLon = Math.cos(Math.toRadians(lat)) * 111320.0;

        // Átváltás: 230 méter sugár -> fokban mért sugár (Euklideszi 2D)
        double radiusLat = radiusMeters / metersPerDegreeLat;
        double radiusLon = radiusMeters / metersPerDegreeLon;

        // Most az Euklideszi távolsághoz egy "átlagolt" fok értéket használunk:
        double radius = Math.sqrt(radiusLat * radiusLat + radiusLon * radiusLon);

        // Most jöhet az eredeti keresés – Euklideszi távolság `radius`-on belül
        searchRecursive(q, node, radius, neighbors);
    }

    private void searchRecursive(double[] q, MyKDtree.Node node, double radius, List<Neighbor<double[], E>> neighbors) {
        if (node.isLeaf()) {
            for (int idx = node.index; idx < node.index + node.count; idx++) {
                int i = index[idx];
                if (q != keys[i]) {
                    double distance = MathEx.distance(q, keys[i]); // Ez Euklideszi távolság fokban
                    if (distance <= radius) {
                        neighbors.add(new Neighbor<>(keys[i], data[i], i, distance));
                    }
                }
            }
        } else {
            MyKDtree.Node nearer, further;
            double diff = q[node.split] - node.cutoff;
            if (diff < 0) {
                nearer = node.lower;
                further = node.upper;
            } else {
                nearer = node.upper;
                further = node.lower;
            }

            searchRecursive(q, nearer, radius, neighbors);

            if (radius >= Math.abs(diff)) {
                searchRecursive(q, further, radius, neighbors);
            }
        }
    }
    @Override
    public Neighbor<double[], E> nearest(double[] q) {
        MyNeighborBuilder<double[], E> neighbor = new MyNeighborBuilder<>();
        search(q, root, neighbor);
        neighbor.key = keys[neighbor.index];
        neighbor.value = data[neighbor.index];
        return neighbor.toNeighbor();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Neighbor<double[], E>[] search(double[] q, int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("Invalid k: " + k);
        }

        if (k > keys.length) {
            throw new IllegalArgumentException("Neighbor array length is larger than the dataset size");
        }

        HeapSelect<MyNeighborBuilder<double[], E>> heap = new HeapSelect<>(MyNeighborBuilder.class, k);
        for (int i = 0; i < k; i++) {
            heap.add(new MyNeighborBuilder<>());
        }

        search(q, root, heap);
        heap.sort();

        return Arrays.stream(heap.toArray())
                .map(neighbor -> {
                    neighbor.key = keys[neighbor.index];
                    neighbor.value = data[neighbor.index];
                    return neighbor.toNeighbor();
                }).toArray(Neighbor[]::new);
    }

    @Override
    public void search(double[] q, double radius, List<Neighbor<double[], E>> neighbors) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Invalid radius: " + radius);
        }

        search(q, root, radius, neighbors);
    }
}

