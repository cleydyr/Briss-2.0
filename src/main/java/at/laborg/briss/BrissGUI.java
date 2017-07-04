// $Id: BrissGUI.java 70 2012-05-26 16:20:19Z laborg $
/**
 * Copyright 2010, 2011 Gerhard Aigner, Rastislav Wartiak
 * <p>
 * This file is part of BRISS.
 * <p>
 * BRISS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * BRISS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * BRISS. If not, see http://www.gnu.org/licenses/.
 */
package at.laborg.briss;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jpedal.exception.PdfException;

import com.itextpdf.text.DocumentException;

import at.laborg.briss.exception.CropException;
import at.laborg.briss.gui.HelpDialog;
import at.laborg.briss.gui.MergedPanel;
import at.laborg.briss.gui.WrapLayout;
import at.laborg.briss.model.ClusterDefinition;
import at.laborg.briss.model.PageCluster;
import at.laborg.briss.model.PageExcludes;
import at.laborg.briss.model.WorkingSet;
import at.laborg.briss.utils.BrissFileHandling;
import at.laborg.briss.utils.ClusterCreator;
import at.laborg.briss.utils.FileDrop;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.FileChooser;

/**
 * @author gerhard, hybridtupel
 */

public class BrissGUI extends JFrame implements PropertyChangeListener, ComponentListener {

    private static final long serialVersionUID = 5623134571633965275L;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int DEFAULT_WIDTH = 800;
    private static final int MIN_HEIGHT = 400;
    private static final int MIN_WIDTH = 400;

    private static final String DONATION_URI = "http://sourceforge.net/project/project_donations.php?group_id=320676"; //$NON-NLS-1$
    private static final String RES_ICON_PATH = "Briss_icon_032x032.gif"; //$NON-NLS-1$
    private static final String RES_DROP_IMG_PATH = "drop.png"; //$NON-NLS-1$
    private static final String PROGRESS = "progress";

    private JMenuBar menuBar;
    private JPanel previewPanel;
    private JProgressBar progressBar;
    private JMenuItem loadButton, showHelpButton, openDonationLinkButton, exitButton;
    private List<MergedPanel> mergedPanels = null;

    private File lastOpenDir;

    private WorkingSet workingSet;
    private final FileChooser fileChooser;
    private DragAndDropPanel dndPanel;
    private JScrollPane scrollPane;
    private CardLayout cardLayout;
    private JPanel wrapperPanel;
    private JButton showPreview;
    private JButton startCropping;

    public BrissGUI(String[] args) {
        super(Messages.getString("BrissGUI.windowTitle")); //$NON-NLS-1$
        init();
        tryToLoadFileFromArgument(args);


        new JFXPanel(); // used for initializing javafx thread
        fileChooser = new FileChooser();
        initFileChooser();
    }

    private void initFileChooser() {
        fileChooser.setInitialDirectory(lastOpenDir);
        FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(pdfFilter);
    }

    private void tryToLoadFileFromArgument(String[] args) {
        if (args.length == 0)
            return;
        File fileArg = new File(args[0]);
        if (fileArg.exists() && fileArg.getAbsolutePath().trim().endsWith(".pdf")) { //$NON-NLS-1$
            try {
                importNewPdfFile(fileArg);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(),
                        Messages.getString("BrissGUI.brissError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            } catch (PdfException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(),
                        Messages.getString("BrissGUI.brissError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            }
        }

    }

    private void init() {

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        this.setTransferHandler(new BrissTransferHandler(this));

        setUILook();

        loadAppIcon();

        // Create the menu bar.
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu(Messages.getString("BrissGUI.file")); //$NON-NLS-1$
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        loadButton = new JMenuItem(Messages.getString("BrissGUI.loadFile"), KeyEvent.VK_L); //$NON-NLS-1$
        loadButton.setEnabled(true);
        loadButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0));
        loadButton.addActionListener(a -> showOpenFileDialog());
        fileMenu.add(loadButton);

        fileMenu.addSeparator();

        exitButton = new JMenuItem(Messages.getString("BrissGUI.exit"), KeyEvent.VK_E); //$NON-NLS-1$
        exitButton.addActionListener(a -> System.exit(0));
        fileMenu.add(exitButton);

        openDonationLinkButton = new JMenuItem(Messages.getString("BrissGUI.donate")); //$NON-NLS-1$
        openDonationLinkButton.addActionListener(a -> {
            openDonation();
        });
        helpMenu.add(openDonationLinkButton);

        showHelpButton = new JMenuItem(Messages.getString("BrissGUI.showHelp")); //$NON-NLS-1$
        showHelpButton.addActionListener(a -> new HelpDialog(this, Messages.getString("BrissGUI.brissHelp"), Dialog.ModalityType.MODELESS));
        helpMenu.add(showHelpButton);

        setJMenuBar(menuBar);

        previewPanel = new JPanel();
        previewPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 4, 4));
        previewPanel.setEnabled(true);
        previewPanel.setBackground(new Color(186, 186, 186));
        previewPanel.addComponentListener(this);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(400, 30));
        progressBar.setVisible(false);

        startCropping = new JButton("Crop PDF");
        startCropping.setVisible(false);
        startCropping.addActionListener(a -> startCropping());

        showPreview = new JButton(("Preview"));
        showPreview.addActionListener(a -> showPreview());
        showPreview.setVisible(false);

        scrollPane = new JScrollPane(previewPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVisible(true);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);

        dndPanel = new DragAndDropPanel(loadDragAndDropLabelImage(), e -> showOpenFileDialog());
        new FileDrop(dndPanel, true, files -> {
            if (files.length == 1) {
                File file = files[0];
                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    loadPDF(file);
                }
            }
        });

        cardLayout = new CardLayout();
        wrapperPanel = new JPanel(cardLayout);
        wrapperPanel.add(scrollPane, "scroll");
        wrapperPanel.add(dndPanel, "dnd");
        add(wrapperPanel, BorderLayout.CENTER);

        cardLayout.last(wrapperPanel);

        JPanel footer = new JPanel();
        footer.add(progressBar);
        footer.add(showPreview);
        footer.add(startCropping);
        add(footer, BorderLayout.PAGE_END);

        setWindowBounds();
        pack();
        setVisible(true);
    }

	private static void openDonation() {
		try {
		    if (Desktop.isDesktopSupported()) {
		        Desktop.getDesktop().browse(URI.create(DONATION_URI));
		    }
		} catch (IOException e) {
		    // Ignore error
		    e.printStackTrace();
		}
	}

    private void startCropping() {
        showSaveFileDialog();
    }

    private void setWindowBounds() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setLocation(screenWidth / 2 - DEFAULT_WIDTH / 2, screenHeight / 2 - DEFAULT_HEIGHT / 2);
    }

    private void setUILook() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException ex) {
            System.out.println(Messages.getString("BrissGUI.lookAndFeelError")); //$NON-NLS-1$
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
        }
    }

    private void loadAppIcon() {
        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource(RES_ICON_PATH));
        setIconImage(icon.getImage());
    }

    private ImageIcon loadDragAndDropLabelImage() {
        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource(RES_DROP_IMG_PATH));
        return icon;
    }

    private void showSaveFileDialog() {
        Platform.runLater(() -> {
            // Open JavaFX file chooser in FX-thread which is not as ugly as the swing one
            initFileChooser();
            fileChooser.setTitle("Save cropped PDF File");
            fileChooser.setInitialFileName(BrissFileHandling.getRecommendedFileName(workingSet.getSourceFile()));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
            	setWorkingState(Messages.getString("BrissGUI.loadingPDF")); //$NON-NLS-1$
                SwingUtilities.invokeLater(() -> {
                	try {
                		OperationFacade.cropAndSave(file, workingSet, lastOpenDir);
                    } catch (IOException | DocumentException | CropException e) {
                        JOptionPane.showMessageDialog(this, e.getMessage(),
                                Messages.getString("BrissGUI.croppingError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                    } finally {
                        setIdleState(); //$NON-NLS-1$
                    }
                });
            }
        });
    }

    private void showOpenFileDialog() {
        Platform.runLater(() -> {
            initFileChooser();
            // Open JavaFX file chooser in FX-thread which is not as ugly as the swing one
            fileChooser.setTitle("Open PDF File");
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                SwingUtilities.invokeLater(() -> loadPDF(file));
            }
        });
    }

    private void loadPDF(File file) {
        try {
            importNewPdfFile(file);
            setTitle("BRISS - " + file.getName()); //$NON-NLS-1$
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), Messages.getString("BrissGUI."), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
        } catch (PdfException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    Messages.getString("BrissGUI.loadingError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
        }
    }

    private void showPreview() {
        try {
            setWorkingState(Messages.getString("BrissGUI.createShowPreview")); //$NON-NLS-1$
            OperationFacade.createAndExecuteCropJobForPreview(workingSet);
        } catch (IOException | DocumentException | CropException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    Messages.getString("BrissGUI.croppingError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
        } finally {
            setIdleState(); //$NON-NLS-1$
        }
    }


    private void setIdleState() {
        progressBar.setVisible(false);
        progressBar.setValue(0);
        progressBar.setString("");
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private void setWorkingState(String stateMessage) {
        progressBar.setVisible(true);
        progressBar.setString(stateMessage);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    protected void importNewPdfFile(File loadFile) throws IOException, PdfException {
        lastOpenDir = loadFile.getParentFile();
        previewPanel.removeAll();
        cardLayout.first(wrapperPanel);
        progressBar.setVisible(true);
        progressBar.setString(Messages.getString("BrissGUI.loadingNewFile")); //$NON-NLS-1$
        ClusterPagesTask clusterTask = new ClusterPagesTask(loadFile, null);
        clusterTask.addPropertyChangeListener(this);
        clusterTask.execute();
    }

    public void alignSelRects(int x, int y, int w, int h) {
        // set position and size of selected rectangles
        for (MergedPanel mp : mergedPanels) {
            mp.setSelCropSize(w, h);
            mp.moveToSelelectedCrops(x, y);
        }
    }

    public void moveSelectedRects(int x, int y) {
        // move selected rectangles
        // parameters are relative to current position
        for (MergedPanel mp : mergedPanels) {
            mp.moveSelelectedCrops(x, y);
        }
    }

    public void setDefinedSizeSelRects() {
        // set size of selected rectangles
        // based on user input

        String defInput = ""; //$NON-NLS-1$

        // get maximum dimensions
        int maxWidth = -1;
        int maxHeight = -1;
        for (MergedPanel panel : mergedPanels) {
            int panelMaxWidth = panel.getWidestSelectedRect();
            if (maxWidth < panelMaxWidth) {
                maxWidth = panelMaxWidth;
            }
            int panelMaxHeight = panel.getHeighestSelectedRect();
            if (maxHeight < panelMaxHeight) {
                maxHeight = panelMaxHeight;
            }
        }
        if ((maxWidth >= 0) && (maxHeight >= 0)) {
            maxWidth = Math.round(25.4f * maxWidth / 72f);
            maxHeight = Math.round(25.4f * maxHeight / 72f);
            defInput = Integer.toString(maxWidth) + " " + Integer.toString(maxHeight); //$NON-NLS-1$
        }

        // get user input
        // maximums are used as a default
        String input = JOptionPane.showInputDialog(Messages.getString("BrissGUI.size"), defInput);

        if (input == null || input.equals("")) //$NON-NLS-1$
            return;

        String[] dims = input.split(" ", 2); //$NON-NLS-1$
        if (dims.length != 2)
            return;

        int w = -1;
        int h = -1;
        try {
            w = Integer.parseInt(dims[0]);
            h = Integer.parseInt(dims[1]);
        } catch (NumberFormatException e) {
            return;
        }

        // convert from milimeters to points
        w = Math.round(w * 72f / 25.4f);
        h = Math.round(h * 72f / 25.4f);

        for (MergedPanel mp : mergedPanels) {
            mp.setSelCropWidth(w);
            mp.setSelCropHeight(h);
        }
    }

    public void setPositionSelRects() {
        // set position of selected rectangles
        // based on user input

        String defInput = ""; //$NON-NLS-1$

        // get minimums of positions
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (MergedPanel panel : mergedPanels) {
            int panelMinX = panel.getLeftmostSelectedRect();
            if (minX > panelMinX) {
                minX = panelMinX;
            }
            int panelMinY = panel.getUpmostSelectedRect();
            if (minY > panelMinY) {
                minY = panelMinY;
            }
        }
        if ((minX < Integer.MAX_VALUE) && (minY < Integer.MAX_VALUE)) {
            minX = Math.round(25.4f * minX / 72f);
            minY = Math.round(25.4f * minY / 72f);
            defInput = Integer.toString(minX) + " " + Integer.toString(minY); //$NON-NLS-1$
        }

        // get user input
        // minimums are used as a default
        String input = JOptionPane.showInputDialog(Messages.getString("BrissGUI.position"), defInput);

        if (input == null || input.equals("")) //$NON-NLS-1$
            return;

        String[] dims = input.split(" ", 2); //$NON-NLS-1$
        if (dims.length != 2)
            return;

        int x = -1;
        int y = -1;
        try {
            x = Integer.parseInt(dims[0]);
            y = Integer.parseInt(dims[1]);
        } catch (NumberFormatException e) {
            return;
        }

        // convert from milimeters to points
        x = Math.round(x * 72f / 25.4f);
        y = Math.round(y * 72f / 25.4f);

        for (MergedPanel mp : mergedPanels) {
            mp.moveToSelelectedCrops(x, y);
        }
    }

    public void resizeSelRects(int w, int h) {
        // change size of selected rectangles (relative)
        for (MergedPanel mp : mergedPanels) {
            mp.resizeSelCrop(w, h);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (PROGRESS.equals(evt.getPropertyName())) { //$NON-NLS-1$
            progressBar.setValue((Integer) evt.getNewValue());
        }
    }

    private void setStateAfterClusteringFinished(ClusterDefinition newClusters, PageExcludes newPageExcludes,
                                                 File newSource) {
        updateWorkingSet(newClusters, newPageExcludes, newSource);

        previewPanel.removeAll();
        mergedPanels = new ArrayList<MergedPanel>();

		for (MergedPanel p : generateMergedPanels(workingSet, this)) {
            previewPanel.add(p);
            mergedPanels.add(p);
        }
        progressBar.setString(Messages.getString("BrissGUI.clusteringRenderingFinished")); //$NON-NLS-1$

        setIdleState(); //$NON-NLS-1$
        pack();
        setExtendedState(Frame.MAXIMIZED_BOTH);
        previewPanel.repaint();
        showPreview.setVisible(true);
        startCropping.setVisible(true);
        progressBar.setVisible(false);
        repaint();
    }

    private static List<MergedPanel> generateMergedPanels(WorkingSet workingSet, BrissGUI brissGUI) {
    	List<MergedPanel> mergedPanels = new ArrayList<MergedPanel>();
    	List<PageCluster> clusterList = workingSet.getClusterDefinition().getClusterList();
    	for (PageCluster cluster : clusterList) {
    		mergedPanels.add(new MergedPanel(cluster, brissGUI));
    	}
    	return mergedPanels;
	}

	private void updateWorkingSet(ClusterDefinition newClusters, PageExcludes newPageExcludes, File newSource) {
        if (workingSet == null) {
            // completely new
            workingSet = new WorkingSet(newSource);
        } else if (workingSet.getSourceFile().equals(newSource)) {
            // just reload with other excludes
            OperationFacade.copyCropsToClusters(workingSet.getClusterDefinition(), newClusters);
        }
        workingSet.setSourceFile(newSource);
        workingSet.setClusters(newClusters);
        workingSet.setPageExcludes(newPageExcludes);
    }

    private class ClusterPagesTask extends SwingWorker<Void, Integer> {

        private final File source;
        private final PageExcludes pageExcludes;
        private ClusterDefinition clusterDefinition = null;

        public ClusterPagesTask(File source, PageExcludes pageExcludes) {
            super();
            this.source = source;
            this.pageExcludes = pageExcludes;
        }

        @Override
        protected void done() {
            setStateAfterClusteringFinished(clusterDefinition, pageExcludes, source);
        }

        @Override
        protected Void doInBackground() {

        	try {
				clusterDefinition = ClusterCreator.clusterPages(source, pageExcludes);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            OperationFacade.processClusters(clusterDefinition, source, this::publish);

            return null;
        }

        @Override
        protected void process(List<Integer> chunks) {
        	for (Integer progress : chunks) {
        		setProgress(progress);
        	}
        }
		
    }

    @Override
    public void componentResized(ComponentEvent e) {
        previewPanel.revalidate();
        for (Component component : previewPanel.getComponents()) {
            component.repaint();
        }
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }
}
