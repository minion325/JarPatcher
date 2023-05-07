package me.saif.jarpatcher;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.io.File;

public class Main {

    private static JarPatcher jarPatcher;

    public static void main(String[] args) throws Exception {
        /*JarPatcher jarPatcher = new JarPatcher(new File("C:\\Users\\Saif Mohammed\\Desktop\\EliteEnchantments-2.2.5-all-patched.jar"));

        jarPatcher.getAddendumFiles().add(new File("C:\\Users\\Saif Mohammed\\Desktop\\backpacks-1.0_1.jar"));

        jarPatcher.patch(new File("C:\\Users\\Saif Mohammed\\Desktop\\patched.jar"));*/

        if (System.getProperty("os.name").contains("Windows")) {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        }

        PatcherFrame patcherFrame = new PatcherFrame();

        JFrame frame = new JFrame("JarPatcher");

        frame.setContentPane(patcherFrame.getRootPanel());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();

        frame.setVisible(true);
    }

    public static JarPatcher getJarPatcher() {
        return jarPatcher;
    }

    public static void setJarPatcher(JarPatcher jarPatcher) {
        Main.jarPatcher = jarPatcher;
    }
}
