package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appCoreLogic.model.SubmitMode;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;


/** Lets the user choose how far the app goes on each application. Defaults to the safe Review mode. */
public class SubmitModeSelector extends JPanel {

    private final ButtonGroup group = new ButtonGroup();
    private final Map<SubmitMode, JRadioButton> buttons = new EnumMap<>(SubmitMode.class);
    private Consumer<SubmitMode> onChange = mode -> {};

    public SubmitModeSelector(SubmitMode initial) {
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        for (SubmitMode mode : SubmitMode.values()) {
            add(buildOption(mode));
            add(Theme.vGap(Theme.SPACE_XS));
        }
        select(initial);
    }

    private JPanel buildOption(SubmitMode mode) {
        JRadioButton radio = new JRadioButton(mode.label());
        radio.setFont(Theme.BODY_STRONG);
        radio.setForeground(Theme.TEXT);
        radio.setOpaque(false);
        radio.addActionListener(e -> onChange.accept(mode));
        group.add(radio);
        buttons.put(mode, radio);

        JLabel description = Theme.muted(mode.description());
        description.setBorder(Theme.pad(0, Theme.SPACE_XL + Theme.SPACE_XS, 0, 0));

        JPanel option = new JPanel(new BorderLayout(0, 2));
        option.setOpaque(false);
        option.setAlignmentX(Component.LEFT_ALIGNMENT);
        option.add(radio, BorderLayout.NORTH);
        option.add(description, BorderLayout.CENTER);
        option.setMaximumSize(new Dimension(Integer.MAX_VALUE, option.getPreferredSize().height));
        return option;
    }

    private void select(SubmitMode mode) {
        AbstractButton button = buttons.get(mode);
        if (button != null) {
            button.setSelected(true);
        }
    }

    /** Called on the EDT when the user picks a different mode. */
    public void onChange(Consumer<SubmitMode> action) {
        this.onChange = action;
    }
}
