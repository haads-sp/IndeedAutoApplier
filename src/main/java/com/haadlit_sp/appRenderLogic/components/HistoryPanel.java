package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appCoreLogic.model.HistoryEntry;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;


/** The session history list. Narrow API: {@link #setHistory(List)}. */
public class HistoryPanel extends JPanel {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
    private static final String LIST = "list";
    private static final String EMPTY = "empty";

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final CardLayout swap = new CardLayout();
    private final JPanel body = new JPanel(swap);

    public HistoryPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, Theme.SPACE_SM));
        add(Theme.eyebrow("Applied this session"), BorderLayout.NORTH);

        JList<String> list = new JList<>(model);
        list.setFont(Theme.MONO);
        list.setForeground(Theme.TEXT);
        list.setBackground(Theme.SURFACE);
        list.setFixedCellHeight(24);
        list.setBorder(Theme.pad(Theme.SPACE_SM));

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        body.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        body.add(scroll, LIST);
        body.add(buildEmptyState(), EMPTY);
        add(body, BorderLayout.CENTER);
        swap.show(body, EMPTY);
    }

    /** An empty screen is an invitation to act, so the message lives inside the box, not under it. */
    private JPanel buildEmptyState() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.SURFACE);
        JLabel message = Theme.muted("Each application appears here as it is sent.");
        message.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(message, BorderLayout.CENTER);
        return panel;
    }

    public void setHistory(List<HistoryEntry> entries) {
        model.clear();
        for (HistoryEntry e : entries) {
            model.addElement(String.format("%s  %s @ %s  (%s)",
                    TIME.format(e.timestamp()), e.title(), e.company(), e.outcome()));
        }
        swap.show(body, entries.isEmpty() ? EMPTY : LIST);
    }
}
