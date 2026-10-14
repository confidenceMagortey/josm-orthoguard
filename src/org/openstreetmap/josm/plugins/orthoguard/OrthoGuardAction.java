/* GPL-2.0-or-later */
package org.openstreetmap.josm.plugins.orthoguard;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;

/** Menu action which scans all closed building ways in the active data layer. */
public final class OrthoGuardAction extends AbstractAction {
    public OrthoGuardAction() {
        super("OrthoGuard: Scan rectangular building angles...");
        putValue(SHORT_DESCRIPTION,
                "Find likely rectangular building footprints with non-90-degree corners");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
        if (dataSet == null) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    "Open an editable OSM data layer before running OrthoGuard.",
                    "OrthoGuard", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Collection<Way> scanScope = new ArrayList<>(dataSet.getWays());
        List<OrthoGuardFinding> findings = OrthoGuardDetector.scan(scanScope);
        OrthoGuardDialog dialog = new OrthoGuardDialog(
                MainApplication.getMainFrame(), dataSet, scanScope, findings);
        dialog.setLocationRelativeTo(MainApplication.getMainFrame());
        dialog.setVisible(true);
    }
}
