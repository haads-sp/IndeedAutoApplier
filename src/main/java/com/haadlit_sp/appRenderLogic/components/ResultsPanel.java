package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;


/** The postings a search found, each tagged New or Applied. Narrow API: {@link #setResults}. */
public class ResultsPanel extends JPanel {

    private static final String LIST = "list";
    private static final String EMPTY = "empty";

    private final DefaultListModel<JobPosting> model = new DefaultListModel<>();
    private final CardLayout swap = new CardLayout();
    private final JPanel body = new JPanel(swap);
    private Set<String> appliedIds = Set.of();
    private Map<String, Integer> fitScores = Map.of();

    public ResultsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, Theme.SPACE_SM));
        add(Theme.eyebrow("Matching postings"), BorderLayout.NORTH);

        JList<JobPosting> list = new JList<>(model);
        list.setBackground(Theme.SURFACE);
        list.setFixedCellHeight(52);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new PostingRenderer());

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        body.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        body.add(scroll, LIST);
        body.add(emptyState(), EMPTY);
        add(body, BorderLayout.CENTER);
        swap.show(body, EMPTY);
    }

    private JPanel emptyState() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.SURFACE);
        JLabel message = Theme.muted("Run a search to see matching postings here.");
        message.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(message, BorderLayout.CENTER);
        return panel;
    }

    public void setResults(List<JobPosting> postings, Set<String> appliedIds,
                           Map<String, Integer> fitScores) {
        this.appliedIds = appliedIds == null ? Set.of() : appliedIds;
        this.fitScores = fitScores == null ? Map.of() : Map.copyOf(fitScores);
        model.clear();
        postings.forEach(model::addElement);
        swap.show(body, postings.isEmpty() ? EMPTY : LIST);
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
            String easy = p.easyApply()
                    ? " &nbsp;<span style='color:#0E7A4F'>Easy apply</span>" : "";
            String location = p.location().isBlank() ? "" : " · " + escape(p.location());
            Integer score = fitScores.get(p.id());
            String fit = score == null ? ""
                    : " &nbsp;<span style='color:" + fitColor(score) + "'><b>" + score + "% fit</b></span>";

            setText("<html><b>" + escape(p.title()) + "</b>" + easy + fit
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

        /** Green for strong, amber for partial, red for poor — matches how the user scans a list. */
        private String fitColor(int score) {
            if (score >= 70) {
                return "#0E7A4F";
            }
            return score >= 40 ? "#B87A00" : "#B3382E";
        }
    }
}
