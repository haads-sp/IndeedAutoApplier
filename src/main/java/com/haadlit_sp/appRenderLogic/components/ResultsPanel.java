package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The postings a search found, each tagged New or Applied and — in AI mode — carrying a fit score.
 * The score bands double as filters: clicking one narrows the list to that range, clicking it again
 * clears it. Narrow API: {@link #setResults}.
 */
public class ResultsPanel extends JPanel {

    private static final String LIST = "list";
    private static final String EMPTY = "empty";
    /** Postings visible without scrolling the list itself. */
    private static final int VISIBLE_ROWS = 7;
    /**
     * Separator between the badges on a row. It must be an EM SPACE: under FlatLaf every ordinary
     * space form (a plain space, {@code &nbsp;}, {@code &#160;}, a literal U+00A0) is collapsed
     * BETWEEN two styled HTML runs, which ran the badges together ("Easy apply54% fit"). Built
     * from its codepoint on purpose — written literally it is invisible in source and has already
     * been lost once in an edit.
     */
    private static final String BADGE_GAP = Character.toString(0x2003);

    /**
     * How well a posting matches, as one of three bands. One definition serves both the coloured
     * chip on each row and the filter buttons, so the two can never disagree about where a score
     * belongs.
     */
    private enum FitBand {
        STRONG("≥70%", "#0E7A4F", 70, 100),
        PARTIAL("30–69%", "#B87A00", 30, 69),
        WEAK("<30%", "#B3382E", 0, 29);

        private final String label;
        private final String hex;
        private final int min;
        private final int max;

        FitBand(String label, String hex, int min, int max) {
            this.label = label;
            this.hex = hex;
            this.min = min;
            this.max = max;
        }

        boolean contains(Integer score) {
            return score != null && score >= min && score <= max;
        }

        Color color() {
            return Color.decode(hex);
        }

        static FitBand of(int score) {
            for (FitBand band : values()) {
                if (band.contains(score)) {
                    return band;
                }
            }
            return WEAK;
        }
    }

    private final DefaultListModel<JobPosting> model = new DefaultListModel<>();
    private final CardLayout swap = new CardLayout();
    private final JPanel body = new JPanel(swap);
    private final JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_XS, 0));
    private final Map<FitBand, JButton> filterButtons = new EnumMap<>(FitBand.class);
    private final JLabel emptyMessage = Theme.muted("Run a search to see matching postings here.");

    private List<JobPosting> allPostings = List.of();
    private Set<String> appliedIds = Set.of();
    private Map<String, Integer> fitScores = Map.of();
    private FitBand activeBand = null;   // null = show everything

    public ResultsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, Theme.SPACE_SM));

        filterRow.setOpaque(false);
        for (FitBand band : FitBand.values()) {
            JButton button = filterButton(band);
            filterButtons.put(band, button);
            filterRow.add(button);
        }
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.eyebrow("Matching postings"), BorderLayout.WEST);
        header.add(filterRow, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JList<JobPosting> list = new JList<>(model);
        list.setBackground(Theme.SURFACE);
        list.setFixedCellHeight(52);
        // Ask for room for several postings at once. Without this the list has no opinion about
        // its height and whatever is above it squeezes the results down to a single row.
        list.setVisibleRowCount(VISIBLE_ROWS);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new PostingRenderer());

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        body.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        body.add(scroll, LIST);
        body.add(emptyState(), EMPTY);
        add(body, BorderLayout.CENTER);
        swap.show(body, EMPTY);
        refreshFilterRow();
    }

    private JButton filterButton(FitBand band) {
        JButton button = new JButton(band.label);
        button.setFont(Theme.SMALL);
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            // Clicking the active band clears it — that is how the user gets back to everything.
            activeBand = band.equals(activeBand) ? null : band;
            applyFilter();
        });
        styleFilterButton(button, band, false);
        return button;
    }

    private static void styleFilterButton(JButton button, FitBand band, boolean active) {
        button.setBackground(active ? band.color() : Theme.SURFACE);
        button.setForeground(active ? Theme.ON_ACCENT : band.color());
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(band.color()),
                Theme.pad(Theme.SPACE_XS, Theme.SPACE_SM, Theme.SPACE_XS, Theme.SPACE_SM)));
    }

    private JPanel emptyState() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.SURFACE);
        emptyMessage.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(emptyMessage, BorderLayout.CENTER);
        return panel;
    }

    public void setResults(List<JobPosting> postings, Set<String> appliedIds,
                           Map<String, Integer> fitScores) {
        this.allPostings = postings == null ? List.of() : postings;
        this.appliedIds = appliedIds == null ? Set.of() : appliedIds;
        this.fitScores = fitScores == null ? Map.of() : Map.copyOf(fitScores);
        applyFilter();
    }

    /** Rebuild the visible list for the active band, and keep the filter row in step. */
    private void applyFilter() {
        List<JobPosting> visible = new ArrayList<>();
        for (JobPosting posting : allPostings) {
            if (activeBand == null || activeBand.contains(fitScores.get(posting.id()))) {
                visible.add(posting);
            }
        }
        model.clear();
        visible.forEach(model::addElement);

        emptyMessage.setText(allPostings.isEmpty()
                ? "Run a search to see matching postings here."
                : "No postings in this range — pick another, or click the same button to clear it.");
        swap.show(body, visible.isEmpty() ? EMPTY : LIST);
        refreshFilterRow();
    }

    /** Show a count per band, and hide the whole row until there is something to filter on. */
    private void refreshFilterRow() {
        Map<FitBand, Integer> counts = new EnumMap<>(FitBand.class);
        for (FitBand band : FitBand.values()) {
            counts.put(band, 0);
        }
        for (JobPosting posting : allPostings) {
            Integer score = fitScores.get(posting.id());
            if (score != null) {
                counts.merge(FitBand.of(score), 1, Integer::sum);
            }
        }
        filterButtons.forEach((band, button) -> {
            button.setText(band.label + "  " + counts.get(band));
            styleFilterButton(button, band, band.equals(activeBand));
        });
        // Standard mode never scores, and AI scores arrive progressively — so the row appears
        // only once filtering by score would actually mean something.
        filterRow.setVisible(!fitScores.isEmpty());
    }

    /** Two lines per posting: title (+ easy-apply), then a New/Applied tag with company · location. */
    private final class PostingRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            JobPosting p = (JobPosting) value;
            boolean applied = appliedIds.contains(p.id());

            String tag = applied
                    ? "<span style='color:#8A96A3'>APPLIED</span>"
                    : "<span style='color:#0B6363'>NEW</span>";
            String location = p.location().isBlank() ? "" : " · " + escape(p.location());
            Integer score = fitScores.get(p.id());
            List<String> badges = new ArrayList<>(2);
            if (p.easyApply()) {
                badges.add("<font color='#0E7A4F'>Easy apply</font>");
            }
            if (score != null) {
                badges.add("<font color='" + FitBand.of(score).hex + "'><b>" + score
                        + "% fit</b></font>");
            }
            String badgeText = badges.isEmpty() ? ""
                    : "&nbsp;&nbsp;" + String.join(BADGE_GAP, badges);

            setText("<html><b>" + escape(p.title()) + "</b>" + badgeText
                    + "<br><span style='color:#5B6776'>" + tag + " &nbsp; "
                    + escape(p.company()) + location + "</span></html>");
            setBorder(Theme.pad(Theme.SPACE_SM, Theme.SPACE_MD, Theme.SPACE_SM, Theme.SPACE_MD));
            setBackground(selected ? Theme.ACCENT_SOFT : Theme.SURFACE);
            setForeground(Theme.TEXT);
            return this;
        }

        private String escape(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
