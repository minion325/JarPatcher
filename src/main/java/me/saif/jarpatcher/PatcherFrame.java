package me.saif.jarpatcher;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class PatcherFrame {

    private static final FileFilter fileFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().endsWith(".jar");
        }

        @Override
        public String getDescription() {
            return "Jar Files";
        }
    };

    private JPanel rootPanel;
    private JButton selectMainJar;
    private JButton addFileButton;

    private JButton removeFilesButton;
    private JButton patchJarButton;
    private JList<String> fileToPatchList;

    private JList<String> patchesList;
    private JProgressBar progressBar;

    public PatcherFrame() {
        fileToPatchList.setModel(new DefaultListModel<>());
        patchesList.setModel(new DefaultListModel<>());
        patchesList.setCellRenderer(new DefaultListCellRenderer());
        selectMainJar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();

                if (Main.getJarPatcher() != null)
                    chooser.setSelectedFile(Main.getJarPatcher().getMainFile());

                chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());
                chooser.addChoosableFileFilter(fileFilter);
                chooser.setMultiSelectionEnabled(false);
                int returnVal = chooser.showOpenDialog(PatcherFrame.this.rootPanel);

                while (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();

                    if (!file.getName().endsWith(".jar")) {
                        returnVal = chooser.showOpenDialog(PatcherFrame.this.rootPanel);
                        continue;
                    }

                    ((DefaultListModel<String>) PatcherFrame.this.patchesList.getModel()).clear();
                    DefaultListModel<String> dlm = ((DefaultListModel<String>) fileToPatchList.getModel());
                    dlm.clear();

                    dlm.add(0, file.toPath().getFileName().toString());

                    Main.setJarPatcher(new JarPatcher(file));

                    PatcherFrame.this.patchJarButton.setEnabled(Main.getJarPatcher().isReady());
                    break;
                }
            }
        });

        addFileButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();

                if (Main.getJarPatcher() == null) {
                    OkDialog okDialog = new OkDialog();
                    okDialog.pack();
                    okDialog.setVisible(true);
                    return;
                } else {
                    File dir = Main.getJarPatcher().getMainFile().getParentFile();
                    chooser.setCurrentDirectory(dir);
                }

                chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());
                chooser.addChoosableFileFilter(fileFilter);
                chooser.setMultiSelectionEnabled(true);

                int returnVal = chooser.showOpenDialog(PatcherFrame.this.rootPanel);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File[] files = chooser.getSelectedFiles();

                    for (File file : files) {
                        if (!file.getName().endsWith(".jar")) {
                            OkDialog okDialog = new OkDialog();
                            okDialog.getLabel().setText(file.getName() + " is not a valid patch file.");
                            okDialog.pack();
                            okDialog.setVisible(true);
                            return;
                        } else if (Main.getJarPatcher().getMainFile().equals(file)) {
                            OkDialog okDialog = new OkDialog();
                            okDialog.getLabel().setText(file.getName() + " is the same as the file you are trying to patch.");
                            okDialog.pack();
                            okDialog.setVisible(true);
                            return;
                        }
                    }

                    for (File file : files) {
                        Main.getJarPatcher().getAddendumFiles().add(file);
                        ((DefaultListModel<String>) PatcherFrame.this.patchesList.getModel()).addElement(file.toPath().getFileName().toString());
                    }

                    PatcherFrame.this.patchJarButton.setEnabled(Main.getJarPatcher().isReady());
                }
            }
        });

        removeFilesButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (Main.getJarPatcher() == null || !Main.getJarPatcher().isReady())
                    return;

                int[] indices = new int[PatcherFrame.this.patchesList.getSelectedIndices().length];
                for (int i = PatcherFrame.this.patchesList.getSelectedIndices().length - 1; i >= 0; i--) {
                    indices[indices.length - 1 - i] = PatcherFrame.this.patchesList.getSelectedIndices()[i];
                }

                for (int index : indices) {
                    ((DefaultListModel<String>) PatcherFrame.this.patchesList.getModel()).remove(index);
                    Main.getJarPatcher().getAddendumFiles().remove(index);
                }

                PatcherFrame.this.patchJarButton.setEnabled(Main.getJarPatcher().isReady());
            }
        });

        this.patchJarButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();

                chooser.setCurrentDirectory(Main.getJarPatcher().getMainFile().getParentFile());
                chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());
                chooser.addChoosableFileFilter(fileFilter);
                chooser.setMultiSelectionEnabled(false);

                int returnVal = chooser.showSaveDialog(PatcherFrame.this.getRootPanel());

                if (returnVal == 0) {
                    File file = chooser.getSelectedFile();

                    OkDialog okDialog = null;
                    if (Main.getJarPatcher().getMainFile().equals(file)) {
                        okDialog = new OkDialog();
                        okDialog.getLabel().setText("You cannot save to the file that you are about to patch!");
                    } else if (Main.getJarPatcher().getAddendumFiles().contains(file)) {
                        okDialog = new OkDialog();
                        okDialog.getLabel().setText("You cannot save to a file that your are using as a patch!");
                    }

                    if (okDialog != null) {
                        okDialog.pack();
                        okDialog.setVisible(true);
                        return;
                    }

                    progressBar.setVisible(true);
                    patchJarButton.setEnabled(false);
                    new Thread(() -> {
                        try {
                            Main.getJarPatcher().patch(file, progressBar);
                            new OkDialog().openDialog("Applied patches to " + Main.getJarPatcher().getMainFile().getAbsolutePath() + " and saved to " + file.getAbsolutePath());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            ErrorDialog errorDialog = new ErrorDialog(ex);
                            errorDialog.pack();
                            errorDialog.setVisible(true);
                        } finally {
                            patchJarButton.setEnabled(true);
                            progressBar.setVisible(false);
                        }
                    }).start();

                }
            }
        });
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(4, 2, new Insets(10, 20, 20, 20), -1, -1));
        rootPanel.setAlignmentX(0.0f);
        rootPanel.setMaximumSize(new Dimension(500, 500));
        rootPanel.setMinimumSize(new Dimension(500, 500));
        rootPanel.setName("JarPatcher");
        rootPanel.setPreferredSize(new Dimension(500, 500));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        selectMainJar = new JButton();
        selectMainJar.setActionCommand("Select File");
        selectMainJar.setHorizontalTextPosition(0);
        selectMainJar.setText("Select File");
        panel2.add(selectMainJar, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Select File to patch");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileToPatchList = new JList();
        Font fileToPatchListFont = this.$$$getFont$$$(null, -1, -1, fileToPatchList.getFont());
        if (fileToPatchListFont != null) fileToPatchList.setFont(fileToPatchListFont);
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        fileToPatchList.setModel(defaultListModel1);
        fileToPatchList.setSelectionMode(0);
        panel2.add(fileToPatchList, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addFileButton = new JButton();
        addFileButton.setHorizontalTextPosition(0);
        addFileButton.setText("Add File");
        panel3.add(addFileButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(0);
        label2.setHorizontalTextPosition(0);
        label2.setText("Patches");
        panel3.add(label2, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        removeFilesButton = new JButton();
        removeFilesButton.setText("Remove File");
        panel3.add(removeFilesButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        patchesList = new JList();
        panel3.add(patchesList, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        patchJarButton = new JButton();
        patchJarButton.setAlignmentX(0.0f);
        patchJarButton.setDoubleBuffered(true);
        patchJarButton.setEnabled(false);
        patchJarButton.setHorizontalTextPosition(0);
        patchJarButton.setMargin(new Insets(0, 0, 0, 0));
        patchJarButton.setMultiClickThreshhold(2000L);
        patchJarButton.setRequestFocusEnabled(false);
        patchJarButton.setSelected(false);
        patchJarButton.setText("Patch");
        rootPanel.add(patchJarButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressBar = new JProgressBar();
        progressBar.setRequestFocusEnabled(true);
        progressBar.setVisible(false);
        rootPanel.add(progressBar, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
