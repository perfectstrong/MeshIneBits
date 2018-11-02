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

package meshIneBits.gui.utilities;

import meshIneBits.gui.MeshToggleAction;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ToggleIcon extends JToggleButton {
    private boolean on = false;
    private final Border onBorder = BorderFactory.createLoweredBevelBorder();
    private final Border offBorder = BorderFactory.createRaisedBevelBorder();

    public ToggleIcon(MeshToggleAction meshToggleAction) {
        super(meshToggleAction);
        setHideActionText(true);
        setToolTipText(meshToggleAction.getToolTipText());
        setContentAreaFilled(false);
        setBorder(offBorder);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                setContentAreaFilled(false);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                setFocusPainted(false);
                if (on) {
                    on = false;
                    setBorder(offBorder);
                } else {
                    on = true;
                    setBorder(onBorder);
                }
            }
        });
    }
}