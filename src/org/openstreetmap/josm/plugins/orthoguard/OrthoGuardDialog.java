/* GPL-2.0-or-later */
package org.openstreetmap.josm.plugins.orthoguard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.actions.OrthogonalizeAction;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.data.UndoRedoHandler;

/** Review, select and correct findings from an OrthoGuard scan. */
public final class OrthoGuardDialog extends JDialog {
    private final DataSet dataSet;
    private final List<Way> scanScope;
    private final FindingTableModel tableModel;
    private final JTable table;
    private final JLabel summary;

    public OrthoGuardDialog(Window owner, DataSet dataSet,
            Collection<Way> scanScope, List<OrthoGuardFinding> findings) {
        super(owner, "JOSM OrthoGuard — Building Angle Review", ModalityType.APPLICATION_MODAL);
        this.dataSet = dataSet;
        this.scanScope = new ArrayList<>(scanScope);
        this.tableModel = new FindingTableModel(findings);
        this.table = new JTable(tableModel);
        this.summary = new JLabel();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        setMinimumSize(new Dimension(760, 380));
        setPreferredSize(new Dimension(900, 480));

        JPanel north = new JPanel(new BorderLayout(4, 4));
        north.add(new JLabel("Likely rectangular footprints with one or more interior corner angles different from 90°. Review before changing geometry."),
                BorderLayout.NORTH);
        north.add(summary, BorderLayout.SOUTH);
        north.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(north, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(105);
        table.getColumnModel().getColumn(3).setPreferredWidth(225);
        table.getColumnModel().getColumn(4).setPreferredWidth(125);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    selectSelectedOnMap();
                }
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton selectButton = new JButton("Select + zoom to map");
        selectButton.addActionListener(event -> selectSelectedOnMap());
        JButton fixSelectedButton = new JButton("Fix selected...");
        fixSelectedButton.addActionListener(event -> fixWays(getSelectedWays(), false));
        JButton fixAllButton = new JButton("Fix all flagged...");
        fixAllButton.addActionListener(event -> fixWays(getAllFindingWays(), true));
        JButton rescanButton = new JButton("Rescan");
        rescanButton.addActionListener(event -> refresh());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(event -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(selectButton);
        buttons.add(fixSelectedButton);
        buttons.add(fixAllButton);
        buttons.add(rescanButton);
        buttons.add(closeButton);
        buttons.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 8, 8));
        add(buttons, BorderLayout.SOUTH);

        refreshSummary();
        pack();
        setLocationRelativeTo(owner);
    }

    private List<Way> getSelectedWays() {
        List<Way> selected = new ArrayList<>();
        for (int row : table.getSelectedRows()) {
            OrthoGuardFinding finding = tableModel.getFindingAt(row);
            if (finding != null) {
                selected.add(finding.getWay());
            }
        }
        return selected;
    }

    private List<Way> getAllFindingWays() {
        List<Way> ways = new ArrayList<>();
        for (OrthoGuardFinding finding : tableModel.getFindings()) {
            ways.add(finding.getWay());
        }
        return ways;
    }

    private void selectSelectedOnMap() {
        List<Way> ways = getSelectedWays();
        if (ways.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select one or more findings in the table first.",
                    "OrthoGuard", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        dataSet.setSelected(ways);
        setVisible(false);
        dispose();
        if (MainApplication.getMap() != null && MainApplication.getMap().mapView != null) {
            BoundingXYVisitor bounds = new BoundingXYVisitor();
            for (Way way : ways) {
                bounds.visit(way);
            }
            if (bounds.hasExtend()) {
                MainApplication.getMap().mapView.zoomTo(bounds);
            }
        }
    }

    private void fixWays(List<Way> ways, boolean allFlagged) {
        if (ways.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    allFlagged ? "There are no flagged footprints to correct."
                            : "Select one or more findings in the table first.",
                    "OrthoGuard", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String scopeText = allFlagged ? "all " + ways.size() + " flagged footprint(s)"
                : ways.size() + " selected footprint(s)";
        int answer = JOptionPane.showConfirmDialog(this,
                "OrthoGuard will use JOSM's Orthogonalize Shape algorithm on " + scopeText + ".\n\n"
                + "This changes node positions and can distort genuinely irregular buildings.\n"
                + "Review the geometry afterwards. The change can be undone with JOSM Undo.\n\n"
                + "Apply correction?",
                "Confirm geometry correction", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            List<OsmPrimitive> primitives = new ArrayList<>(ways);
            SequenceCommand command = OrthogonalizeAction.orthogonalize(primitives);
            if (command == null) {
                JOptionPane.showMessageDialog(this,
                        "JOSM did not produce a correction command for these footprints.",
                        "OrthoGuard", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            UndoRedoHandler.getInstance().add(command);
            refresh();
        } catch (OrthogonalizeAction.InvalidUserInputException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "JOSM could not orthogonalize the selected geometry.\n\n" + ex.getMessage(),
                    "OrthoGuard — correction not applied", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "An unexpected error prevented the correction. No automatic retry was attempted.\n\n"
                    + ex.getMessage(), "OrthoGuard — correction failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refresh() {
        tableModel.setFindings(OrthoGuardDetector.scan(scanScope));
        refreshSummary();
    }

    private void refreshSummary() {
        int count = tableModel.getRowCount();
        summary.setText(count == 0
                ? "No likely rectangular footprints with non-90° corners were found. Rounded and multi-corner footprints are excluded."
                : count + " likely rectangular footprint(s) flagged. Asterisk (*) marks each corner not equal to 90°. "
                        + "Review each shape; the flag is not proof of a mapping error.");
        summary.setHorizontalAlignment(SwingConstants.LEFT);
    }

    private static final class FindingTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
            "OSM way", "Building tag", "Corners ≠90°", "Interior corner angles", "Maximum angle"
        };
        private List<OrthoGuardFinding> findings;

        FindingTableModel(List<OrthoGuardFinding> findings) {
            this.findings = new ArrayList<>(findings);
        }

        void setFindings(List<OrthoGuardFinding> findings) {
            this.findings = new ArrayList<>(findings);
            fireTableDataChanged();
        }

        List<OrthoGuardFinding> getFindings() {
            return new ArrayList<>(findings);
        }

        OrthoGuardFinding getFindingAt(int row) {
            if (row < 0 || row >= findings.size()) {
                return null;
            }
            return findings.get(row);
        }

        @Override
        public int getRowCount() {
            return findings.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            OrthoGuardFinding finding = findings.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return finding.getDisplayId();
                case 1:
                    return finding.getBuildingType();
                case 2:
                    return finding.getCornersNotNinety();
                case 3:
                    return finding.getCornerAngleSummary();
                case 4:
                    return String.format(java.util.Locale.ROOT, "%.1f°", finding.getMaximumCornerAngleDegrees());
                default:
                    return "";
            }
        }
    }
}
