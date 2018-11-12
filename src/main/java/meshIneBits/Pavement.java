/*
 * MeshIneBits is a Java software to disintegrate a 3d mesh (model in .stl)
 * into a network of standard parts (called "Bits").
 *
 * Copyright (C) 2016  Thibault Cassard & Nicolas Gouju.
 * Copyright (C) 2017-2018  TRAN Quoc Nhat Han.
 * Copyright (C) 2018 Vallon BENJAMIN.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package meshIneBits;

import meshIneBits.patterntemplates.PatternTemplate;
import meshIneBits.slicer.Slice;
import meshIneBits.util.AreaTool;
import meshIneBits.util.Logger;
import meshIneBits.util.Vector2;

import java.awt.geom.Area;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Build by a {@link PatternTemplate}.
 * Literally a {@link Set} of {@link Bit2D}.
 */
public class Pavement implements Cloneable, Serializable {
    /**
     * The key is the origin of {@link Bit2D} in {@link Mesh} coordinate system.
     */
    private Map<Vector2, Bit2D> mapBits;

    /**
     * Construct pavement out of bits and chosen rotation
     *
     * @param bits in {@link Mesh} coordinate system
     */

    public Pavement(Collection<Bit2D> bits) {
        // Set up map of bits
        mapBits = new HashMap<>();
        for (Bit2D bit : bits) {
            addBit(bit);
        }
    }

    /**
     * @param bit in {@link Mesh} coordinate system
     * @return the origin of inserted bit in {@link Mesh} coordinate system
     */
    public Vector2 addBit(Bit2D bit) {
        Vector2 origin = bit.getOrigin();
        // We check that there is not already a bit at this place
        for (Vector2 key : getBitsKeys()) {
            if (origin.asGoodAsEqual(key)) {
                Logger.warning(
                        "A bit already exists at these coordinates: "
                                + key
                                + ", it has been replaced by the new one.");
                removeBit(key);
            }
        }
        mapBits.put(origin, bit);
        return origin;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Pavement clone() {
        Collection<Bit2D> clonedMapBits = this.getBitsKeys().stream()
                .map(vector2 -> getBit(vector2).clone())
                .collect(Collectors.toSet());
        return new Pavement(clonedMapBits);
    }

    /**
     * Removes the {@link Bit2D} that are outside the boundaries of the
     * {@link Slice} and cut at right shape the ones that intercepts the boundaries.
     *
     * @param slice boundary in {@link Mesh} coordinate system
     * @see #computeBits(Area)
     */
    public void computeBits(Slice slice) {
        computeBits(AreaTool.getAreaFrom(slice));
    }

    /**
     * @param key in {@link Mesh} coordinate system
     * @return {@link Bit2D} in {@link Mesh} coordinate system
     */
    public Bit2D getBit(Vector2 key) {
        return mapBits.get(key);
    }

    /**
     * Retrieve all {@link Bit2D} origins. A new {@link Set} will be created to
     * preserve {@link #mapBits}
     *
     * @return a {@link Set} in {@link Mesh} coordinate system
     */
    public Set<Vector2> getBitsKeys() {
        return new HashSet<>(mapBits.keySet());
    }

    /**
     * Move the chosen bit in the wanted direction. Note: not exactly "moving", but
     * rather "removing" then "adding" new one with same size
     *
     * @param key       in {@link Mesh} coordinate system
     * @param direction in the local coordinate system of the {@link Bit2D}
     * @param distance  the distance of displacement
     * @return the key of the newly added bit in {@link Mesh} coordinate system
     */
    public Vector2 moveBit(Vector2 key, Vector2 direction, double distance) {
        Bit2D bitToMove = mapBits.get(key); // in Mesh coordinate system
        Vector2 translationInMesh =
                direction.rotate(bitToMove.getOrientation())
                        .normal()
                        .mul(distance);
        Vector2 newOrigin = bitToMove.getOrigin().add(translationInMesh);
        removeBit(key);
        return addBit(new Bit2D(
                newOrigin,
                bitToMove.getOrientation(),
                bitToMove.getLength(),
                bitToMove.getWidth()));
    }

    /**
     * Unregister a {@link Bit2D}
     *
     * @param key origin of bit in {@link Mesh} coordinate system
     */
    public void removeBit(Vector2 key) {
        mapBits.remove(key);
    }

    /**
     * Get all stocked {@link Bit2D}s
     *
     * @return whole schema in {@link Mesh} coordinate system
     */
    public Set<Bit2D> getBits() {
        return getBitsKeys().stream()
                .map(this::getBit)
                .collect(Collectors.toSet());
    }

    /**
     * Concat 2 sets
     *
     * @param bits neighbors in same {@link Layer}
     */
    public void addBits(Collection<Bit2D> bits) {
        bits.forEach(this::addBit);
    }

    /**
     * Recompute surfaces of {@link Bit2D}s
     *
     * @param area in {@link Mesh} coordinate system
     * @see #computeBits(Slice)
     */
    public void computeBits(Area area) {
        for (Vector2 key : getBitsKeys()) {
            Bit2D bit = getBit(key);
            Area bitArea = bit.getArea();
            bitArea.intersect(area);
            if (bitArea.isEmpty()) {
                // Outside of border
                mapBits.remove(key);
            } else {
                bit.updateBoundaries(bitArea);
                bit.calcCutPath();
            }
        }
    }
}