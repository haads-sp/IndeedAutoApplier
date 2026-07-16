package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Function;


/**
 * A text field that offers suggestions as you type. It never forces a choice — anything typed is
 * accepted, because no bundled list covers every place someone might apply.
 *
 * @param <T> the suggestion type
 */
public class AutocompleteField<T> extends JPanel {

    private static final int MAX_SUGGESTIONS = 8;
    private static final int ROW_HEIGHT = 26;

    private final JTextField field;
    private final Function<String, List<T>> lookup;
    private final Function<T, String> render;
    private final DefaultListModel<T> model = new DefaultListModel<>();
    private final JList<T> list = new JList<>(model);
    private final JPopupMenu popup = new JPopupMenu();
    /** Set while we rewrite the text ourselves, so the edit does not re-trigger a lookup. */
    private boolean applyingSuggestion;

    public AutocompleteField(String placeholder, int columns,
                             Function<String, List<T>> lookup, Function<T, String> render) {
        this.lookup = lookup;
        this.render = render;
        this.field = Theme.textField(placeholder, columns);

        setOpaque(false);
        setLayout(new BorderLayout());
        add(field, BorderLayout.CENTER);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));

        buildPopup();
        wireTyping();
        wireKeys();
    }

    private void buildPopup() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(Theme.BODY);
        list.setFixedCellHeight(ROW_HEIGHT);
        list.setBackground(Theme.SURFACE);
        list.setBorder(Theme.pad(Theme.SPACE_XS));
        list.setCellRenderer(new SuggestionRenderer());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (list.getSelectedValue() != null) {
                    accept(list.getSelectedValue());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        popup.setBorder(BorderFactory.createLineBorder(Theme.LINE_STRONG));
        popup.setFocusable(false); // keep the caret in the field while the list is showing
        popup.add(scroll);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                popup.setVisible(false);
            }
        });
    }

    private void wireTyping() {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }

            private void refresh() {
                if (!applyingSuggestion) {
                    SwingUtilities.invokeLater(AutocompleteField.this::showSuggestions);
                }
            }
        });
    }

    /** Arrow keys move through the list, Enter takes the highlighted row, Escape dismisses it. */
    private void wireKeys() {
        bind("DOWN", () -> move(1));
        bind("UP", () -> move(-1));
        bind("ESCAPE", () -> popup.setVisible(false));
        bind("ENTER", () -> {
            if (popup.isVisible() && list.getSelectedValue() != null) {
                accept(list.getSelectedValue());
            }
        });
    }

    private void bind(String key, Runnable action) {
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), key);
        field.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void move(int delta) {
        if (!popup.isVisible() || model.isEmpty()) {
            return;
        }
        int next = Math.floorMod(list.getSelectedIndex() + delta, model.size());
        list.setSelectedIndex(next);
        list.ensureIndexIsVisible(next);
    }

    private void showSuggestions() {
        String typed = field.getText();
        List<T> matches = typed.isBlank() ? List.of() : lookup.apply(typed);
        model.clear();
        for (T match : matches.subList(0, Math.min(matches.size(), MAX_SUGGESTIONS))) {
            model.addElement(match);
        }
        if (model.isEmpty() || !field.isShowing()) {
            popup.setVisible(false);
            return;
        }
        list.setSelectedIndex(0);
        // Rows + the list's own padding + the popup border, or a scrollbar appears for a few items.
        int height = model.size() * ROW_HEIGHT + (Theme.SPACE_XS * 2) + 2;
        popup.setPopupSize(field.getWidth(), height);
        popup.show(field, 0, field.getHeight() + 2);
        field.requestFocusInWindow(); // popup.show() steals focus; typing must continue
    }

    private void accept(T value) {
        applyingSuggestion = true;
        try {
            field.setText(render.apply(value));
            field.setCaretPosition(field.getText().length());
        } finally {
            applyingSuggestion = false;
        }
        popup.setVisible(false);
    }

    /** Whatever is in the field — a picked suggestion or free text the user typed. */
    public String getText() {
        return field.getText().trim();
    }

    public void setText(String text) {
        applyingSuggestion = true;
        try {
            field.setText(text);
        } finally {
            applyingSuggestion = false;
        }
    }

    /** Renders the city bold with its region/country suffix muted, so the suffix is scannable. */
    private final class SuggestionRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> jList, Object value, int index,
                                                      boolean selected, boolean focused) {
            super.getListCellRendererComponent(jList, value, index, selected, focused);
            @SuppressWarnings("unchecked")
            String text = render.apply((T) value);
            int split = text.indexOf(',');
            if (split > 0) {
                setText("<html><b>" + escape(text.substring(0, split)) + "</b>"
                        + "<font color='#8A96A3'>" + escape(text.substring(split)) + "</font></html>");
            } else {
                setText(text);
            }
            setBorder(Theme.pad(0, Theme.SPACE_SM, 0, Theme.SPACE_SM));
            setBackground(selected ? Theme.ACCENT_SOFT : Theme.SURFACE);
            setForeground(Theme.TEXT);
            return this;
        }

        private String escape(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

}
