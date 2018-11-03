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

package meshIneBits.patterntemplates;

import meshIneBits.Layer;
import meshIneBits.Mesh;
import meshIneBits.Pavement;
import meshIneBits.config.PatternConfig;
import meshIneBits.util.Vector2;

import java.awt.geom.Area;
import java.io.Serializable;

/**
 * This is a factory paving the layers. Use {@link #pave(Layer)} to pave.
 */
public abstract class PatternTemplate implements Serializable, Cloneable {

    /**
     * Contains all customizable special parameters of the pattern template
     */
    PatternConfig config = new PatternConfig();

    /**
     * Prepare own parameters (use {@link PatternTemplate#initiateConfig()}
     */
    PatternTemplate() {
        super();
        initiateConfig();
    }

    /**
     * Prepare own parameters from a externally loaded configuration.
     *
     * @param patternConfig all given parameters will be filtered again before applying. Those
     *                      which do not appear in the default configuration of template will
     *                      be discarded.
     */
    public PatternTemplate(PatternConfig patternConfig) {
        super();
        initiateConfig();
        // Setup currentValue fields
        for (String configName : config.keySet()) {
            if (patternConfig.containsKey(configName)) {
                Object currentValue = config.get(configName).getCurrentValue(),
                        newValue = patternConfig.get(configName).getCurrentValue();
                if (currentValue.getClass().isAssignableFrom(newValue.getClass())) {
                    config.get(configName).setCurrentValue(newValue);
                }
            }
        }
    }

    /**
     * Initialize the special configurations if no predefined configuration found.
     */
    protected abstract void initiateConfig();

    /**
     * Calculate private parameters, after slicing and before generating bits.
     *
     * @param mesh the current part in workplace
     * @return <tt>false</tt> if the preparation fails
     */
    public abstract boolean ready(Mesh mesh);

    /**
     * Construct the layer based on this pattern. This constructor is similar to
     * make cake: apply a mould onto flour.
     *
     * @param layer target to fill {@link meshIneBits.Bit2D} in
     * @return schema to fill the given surface
     * @since 0.3
     */
    public abstract Pavement pave(Layer layer);

    /**
     * A general case of {@link #pave(Layer)}
     *
     * @param layer base of filling
     * @param area  target to fill. Expressed in general coordinate system
     * @return schema of paving instructions
     * @since 0.4
     */
    public abstract Pavement pave(Layer layer, Area area);

    /**
     * Auto-optimization. Also a constructor who considers completely the layer's
     * boundary.
     *
     * @param actualState the whole actual bits' placement in layer
     * @return <ul>
     * <li>&ge;0: the number of bits not solved yet</li>
     * <li>0: success</li>
     * <li>-1: failure</li>
     * <li>-2: not implemented</li>
     * </ul>
     */
    public abstract int optimize(Layer actualState);

    /**
     * Move the bit by the minimum distance automatically calculated.
     * <p>
     * The distance depends on the chosen pattern. Realize the move on the input
     * pattern. Do not use {@link Layer#moveBit(Vector2, Vector2)} here to
     * avoid infinite loop
     *
     * @param actualState    the actual state of layer which is paved by this pattern template
     * @param bitKey         the transformed origin of bit
     * @param localDirection the direction in the coordinate system of bit
     * @return the new origin of the moved bit
     */
    public abstract Vector2 moveBit(Pavement actualState, Vector2 bitKey, Vector2 localDirection);

    /**
     * Similar to {@link #moveBit(Pavement, Vector2, Vector2)} except the distance is
     * free to decide.
     *
     * @param actualState    the actual state of layer which is paved by this pattern template
     * @param bitKey         the transformed origin of bit
     * @param localDirection the direction in the coordinate system of bit
     * @param distance       an positive real number (in double precision)
     * @return the new origin of the moved bit
     * @see PatternTemplate#moveBit(Pavement, Vector2, Vector2)
     */
    public abstract Vector2 moveBit(Pavement actualState, Vector2 bitKey, Vector2 localDirection, double distance);

    /**
     * A hint for paving sequentially from lowest layer to highest one
     *
     * @return <tt>false</tt> to parallelize execution
     */
    public boolean isInterdependent() {
        return false;
    }

    /**
     * @return the full name of icon representation the template
     */
    public String getIconName() {
        return "pattern-default.png";
    }

    /**
     * @return the common name of the template
     */
    public String getCommonName() {
        return "An Unknown Template";
    }

    /**
     * @return a block of text of description about this template
     */
    public String getDescription() {
        return "A predefined template.";
    }

    /**
     * @return a block of text about how to use this template
     */
    public String getHowToUse() {
        return "Customize parameters to reach the desired pattern.";
    }

    public PatternConfig getPatternConfig() {
        return config;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        PatternTemplate patternTemplate;
        patternTemplate = (PatternTemplate) super.clone();
        patternTemplate.config = (PatternConfig) config.clone();
        return patternTemplate;
    }
}
