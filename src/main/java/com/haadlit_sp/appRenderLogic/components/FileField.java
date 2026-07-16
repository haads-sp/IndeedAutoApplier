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


/** A labelled row that lets the user pick one PDF (or several, when {@code multi}). */
public class FileField extends JPanel {

    private static final int ROW_HEIGHT = 40;
    private static final int LABEL_WIDTH = 104;

    private final boolean multi;
    private final JLabel valueLabel = Theme.body("None selected");
    private final List<Path> files = new ArrayList<>();
    private Runnable onChange = () -> {};

    public FileField(String label, boolean multi) {
        this.multi = multi;
        setOpaque(false);
        setLayout(new BorderLayout(Theme.SPACE_MD, 0));
        setBorder(Theme.pad(Theme.SPACE_XS, 0, Theme.SPACE_XS, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));

        JLabel name = Theme.body(label);
        name.setPreferredSize(new Dimension(LABEL_WIDTH, 24));
        add(name, BorderLayout.WEST);

        valueLabel.setForeground(Theme.TEXT_FAINT);
        add(valueLabel, BorderLayout.CENTER);

        JButton choose = Theme.secondaryButton(multi ? "Choose files…" : "Choose file…");
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
            valueLabel.setForeground(Theme.TEXT_FAINT);
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
