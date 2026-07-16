package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/** A labeled row that lets the user pick one PDF (or several, when {@code multi}). */
public class FileField extends JPanel {

    private final boolean multi;
    private final JLabel valueLabel = Theme.body("None selected");
    private final List<Path> files = new ArrayList<>();
    private Runnable onChange = () -> {};

    public FileField(String label, boolean multi) {
        this.multi = multi;
        setOpaque(false);
        setLayout(new BorderLayout(Theme.GAP, 0));
        setBorder(Theme.pad(Theme.GAP / 2, 0, Theme.GAP / 2, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel name = Theme.heading(label);
        name.setPreferredSize(new Dimension(160, 24));
        add(name, BorderLayout.WEST);

        valueLabel.setForeground(Theme.MUTED);
        add(valueLabel, BorderLayout.CENTER);

        JButton choose = Theme.secondaryButton("Choose…");
        choose.addActionListener(e -> chooseFiles());
        add(choose, BorderLayout.EAST);
    }

    private void chooseFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(multi);
        chooser.setFileFilter(new FileNameExtensionFilter("PDF documents (*.pdf)", "pdf"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        files.clear();
        if (multi) {
            for (File f : chooser.getSelectedFiles()) {
                files.add(f.toPath());
            }
        } else {
            files.add(chooser.getSelectedFile().toPath());
        }
        updateLabel();
        onChange.run();
    }

    /** Called on the EDT whenever the selection changes. */
    public void onChange(Runnable action) {
        this.onChange = action;
    }

    private void updateLabel() {
        if (files.isEmpty()) {
            valueLabel.setText("None selected");
            valueLabel.setForeground(Theme.MUTED);
            return;
        }
        valueLabel.setText(files.size() == 1
                ? files.get(0).getFileName().toString()
                : files.size() + " files selected");
        valueLabel.setForeground(Theme.TEXT);
    }

    /** First (or only) selected file, or {@code null} if none. */
    public Path selectedFile() {
        return files.isEmpty() ? null : files.get(0);
    }

    /** All selected files (possibly empty). */
    public List<Path> selectedFiles() {
        return List.copyOf(files);
    }
}
