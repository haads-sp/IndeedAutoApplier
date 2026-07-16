package com.haadlit_sp.appRenderLogic.theme;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;


/** Single source of truth for app identity: what it is called, its version, and its icon. */
public final class AppInfo {

    private AppInfo() {}

    private static final Logger LOG = System.getLogger(AppInfo.class.getName());

    public static final String NAME = "Indeed Auto-Applier";
    public static final String VERSION = "0.1.0";

    private static final String ICON_RESOURCE = "/IndeedAutoApplier_logo_1.png";
    /** Windows picks whichever it needs for the title bar, the taskbar button, and alt-tab. */
    private static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};

    /**
     * The window/taskbar icon at every size Windows asks for. Empty when the logo cannot be read,
     * which just leaves the stock Java icon rather than stopping the app from starting.
     */
    public static List<Image> icons() {
        try (InputStream in = AppInfo.class.getResourceAsStream(ICON_RESOURCE)) {
            if (in == null) {
                LOG.log(Level.WARNING, "App icon {0} is not on the classpath", ICON_RESOURCE);
                return List.of();
            }
            BufferedImage source = ImageIO.read(in);
            if (source == null) {
                LOG.log(Level.WARNING, "App icon {0} is not a readable image", ICON_RESOURCE);
                return List.of();
            }
            // Reduce the large source once with area averaging, then derive the rest from that:
            // scaling straight from ~900px to 16px in one hop leaves the small icons aliased.
            BufferedImage base = scaled(source, ICON_SIZES[ICON_SIZES.length - 1]);
            List<Image> icons = new ArrayList<>();
            for (int size : ICON_SIZES) {
                icons.add(size == base.getWidth() ? base : scaled(base, size));
            }
            return icons;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not load the app icon", e);
            return List.of();
        }
    }

    private static BufferedImage scaled(BufferedImage source, int size) {
        Image smooth = source.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(smooth, 0, 0, null);
        g.dispose();
        return out;
    }
}
