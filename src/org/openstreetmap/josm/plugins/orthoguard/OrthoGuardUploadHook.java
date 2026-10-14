/* GPL-2.0-or-later */
package org.openstreetmap.josm.plugins.orthoguard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.upload.UploadHook;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;

/** Warns before upload when changed nodes/ways belong to flagged building ways. */
public final class OrthoGuardUploadHook implements UploadHook {
    @Override
    public boolean checkUpload(APIDataSet apiDataSet) {
        DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
        if (dataSet == null || apiDataSet == null || apiDataSet.isEmpty()) {
            return true;
        }

        Set<OsmPrimitive> uploadPrimitives = new HashSet<>(apiDataSet.getPrimitives());
        List<Way> uploadAffectedBuildingWays = new ArrayList<>();
        for (Way way : dataSet.getWays()) {
            if (!OrthoGuardDetector.isBuildingFootprint(way)) {
                continue;
            }
            if (uploadPrimitives.contains(way) || hasUploadingNode(way, uploadPrimitives)) {
                uploadAffectedBuildingWays.add(way);
            }
        }

        List<OrthoGuardFinding> findings = OrthoGuardDetector.scan(uploadAffectedBuildingWays);
        if (findings.isEmpty()) {
            return true;
        }

        Object[] options = {"Review findings (cancel this upload)", "Upload anyway", "Cancel upload"};
        int result = JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
                "OrthoGuard found " + findings.size()
                + " likely rectangular building footprint(s) with interior corner angles different from 90 degrees affected by this upload.\n\n"
                + "Rounded and multi-corner footprints are excluded. These are review warnings, not proof of errors. Inspect them before uploading.\n\n"
                + "Reviewing findings will stop this upload attempt so you can edit and retry.",
                "OrthoGuard — pre-upload angle check", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (result == 0) {
            Collection<Way> scope = new ArrayList<>(uploadAffectedBuildingWays);
            OrthoGuardDialog dialog = new OrthoGuardDialog(MainApplication.getMainFrame(),
                    dataSet, scope, findings);
            dialog.setLocationRelativeTo(MainApplication.getMainFrame());
            dialog.setVisible(true);
            return false;
        }
        return result == 1;
    }

    private boolean hasUploadingNode(Way way, Set<OsmPrimitive> uploadPrimitives) {
        for (org.openstreetmap.josm.data.osm.Node node : way.getNodes()) {
            if (uploadPrimitives.contains(node)) {
                return true;
            }
        }
        return false;
    }
}
