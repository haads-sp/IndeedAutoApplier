package com.haadlit_sp.appRenderLogic;

import com.haadlit_sp.appCoreLogic.model.AnswerMode;
import com.haadlit_sp.appRenderLogic.theme.AppInfo;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * Modal chooser shown before the main window: Standard vs AI-enhanced answering, with an optional
 * "remember" so it doesn't ask again. Closing the dialog without choosing means Standard, not
 * remembered — the safe default that downloads nothing.
 */
public final class StartupModeDialog {

    /** What the user picked, and whether to skip this dialog on future launches. */
    public record Choice(AnswerMode mode, boolean remember) {}

    private StartupModeDialog() {}

    /** Shows the chooser and blocks until Continue or close. Must be called on the EDT. */
    public static Choice show(AnswerMode initial) {
        JDialog dialog = new JDialog((Frame) null, AppInfo.NAME, true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        List<Image> icons = AppInfo.icons();
        if (!icons.isEmpty()) {
            dialog.setIconImages(icons);
        }

        AnswerMode[] selected = {initial};
        Choice[] result = {new Choice(AnswerMode.STANDARD, false)};

        ButtonGroup group = new ButtonGroup();
        Map<AnswerMode, JRadioButton> buttons = new EnumMap<>(AnswerMode.class);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.CANVAS);
        content.setBorder(Theme.pad(Theme.SPACE_XL));

        content.add(Theme.heading("How should it answer screener questions?"));
        content.add(Theme.vGap(Theme.SPACE_SM));
        for (AnswerMode mode : AnswerMode.values()) {
            JRadioButton radio = new JRadioButton(mode.label());
            radio.setFont(Theme.BODY_STRONG);
            radio.setForeground(Theme.TEXT);
            radio.setOpaque(false);
            radio.addActionListener(e -> selected[0] = mode);
            group.add(radio);
            buttons.put(mode, radio);

            JLabel description = Theme.muted(
                    "<html><body style='width: 380px'>" + mode.description() + "</body></html>");
            description.setBorder(Theme.pad(0, Theme.SPACE_XL + Theme.SPACE_XS, 0, 0));

            JPanel option = new JPanel(new BorderLayout(0, 2));
            option.setOpaque(false);
            option.setAlignmentX(Component.LEFT_ALIGNMENT);
            option.add(radio, BorderLayout.NORTH);
            option.add(description, BorderLayout.CENTER);
            content.add(option);
            content.add(Theme.vGap(Theme.SPACE_SM));
        }
        buttons.get(initial).setSelected(true);

        JCheckBox remember = new JCheckBox("Remember my choice and don't ask again");
        remember.setFont(Theme.BODY);
        remember.setOpaque(false);
        remember.setForeground(Theme.TEXT_MUTED);
        remember.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(remember);
        content.add(Theme.vGap(Theme.SPACE_LG));

        JButton go = Theme.primaryButton("Continue");
        go.addActionListener(e -> {
            result[0] = new Choice(selected[0], remember.isSelected());
            dialog.dispose();
        });
        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(go, BorderLayout.EAST);
        content.add(buttonRow);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);   // blocks until dispose
        return result[0];
    }
}
