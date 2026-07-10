package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appCoreLogic.model.HistoryEntry;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;


/** The session history list. Narrow API: {@link #setHistory(List)}. */
public class HistoryPanel extends JPanel {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());

    private final DefaultListModel<String> model = new DefaultListModel<>();

    public HistoryPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, Theme.GAP / 2));
        add(Theme.heading("Applied this session"), BorderLayout.NORTH);

        JList<String> list = new JList<>(model);
        list.setFont(Theme.BODY);
        list.setBackground(Theme.SURFACE);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(scroll, BorderLayout.CENTER);
    }

    public void setHistory(List<HistoryEntry> entries) {
        model.clear();
        for (HistoryEntry e : entries) {
            model.addElement(String.format("%s — %s @ %s  (%s)",
                    TIME.format(e.timestamp()), e.title(), e.company(), e.outcome()));
        }
    }
}
