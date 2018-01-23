package meshIneBits.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import meshIneBits.Bit3D;
import meshIneBits.GeneratedPart;
import meshIneBits.Layer;
import meshIneBits.MeshIneBitsMain;
import meshIneBits.config.CraftConfig;
import meshIneBits.config.CraftConfigLoader;
import meshIneBits.config.PatternConfig;
import meshIneBits.config.PatternParameterConfig;
import meshIneBits.config.Setting;
import meshIneBits.gui.processing.ProcessingView;
import meshIneBits.gui.utilities.ButtonIcon;
import meshIneBits.gui.utilities.CustomFileChooser;
import meshIneBits.gui.utilities.LabeledListReceiver;
import meshIneBits.gui.utilities.LabeledSpinner;
import meshIneBits.gui.utilities.OptionsContainer;
import meshIneBits.gui.utilities.RibbonTab;
import meshIneBits.gui.utilities.TabContainerSeparator;
import meshIneBits.patterntemplates.PatternTemplate;
import meshIneBits.util.Logger;
import meshIneBits.util.Optimizer;
import meshIneBits.util.XmlTool;

public class Ribbon extends JTabbedPane implements Observer {
	private static final long serialVersionUID = -1759701286071368808L;
	private ViewObservable viewObservable;
	private File file = null;
	private File patternConfigFile = null;
	private JPanel FileTab;
	private SlicerTab SlicerTab;
	private TemplateTab TemplateTab;
	private ReviewTab ReviewTab;
	private HashMap<String, Setting> setupAnnotations = loadAnnotations();

	public HashMap<String, Setting> getSetupAnnotations() {
		return setupAnnotations;
	}

	public Ribbon() {
		viewObservable = ViewObservable.getInstance();

		// Add the tab
		FileTab = new JPanel();
		SlicerTab = new SlicerTab();
		TemplateTab = new TemplateTab();
		ReviewTab = new ReviewTab();
		addTab("File", FileTab);
		addTab("Slicer", new JScrollPane(SlicerTab));
		addTab("Template", new JScrollPane(TemplateTab));
		addTab("Review", new JScrollPane(ReviewTab));

		// Add the menu button
		FileMenuButton fileMenuBtn = new FileMenuButton();
		Ribbon.this.setTabComponentAt(0, fileMenuBtn);

		Ribbon.this.setSelectedIndex(indexOfTab("Slicer"));

		// Disabling the tabs that are useless before slicing
		setEnabledAt(indexOfTab("Review"), false);
		setEnabledAt(indexOfTab("File"), false);

		// Visual options
		setFont(new Font(this.getFont().toString(), Font.PLAIN, 15));
	}

	@Override
	public void update(Observable o, Object arg) {
		// If no STL loaded, disable slice and generate layers button
		if (viewObservable.getCurrentPart() == null) {
			setEnabledAt(indexOfTab("Review"), false);
			SlicerTab.getComputeSlicesBtn().setEnabled(false);
			TemplateTab.getComputeTemplateBtn().setEnabled(false);
		}
		// If a STL is loaded & sliced & layers generated, enable both button
		// (to allow redo computation)
		if ((viewObservable.getCurrentPart() != null) && viewObservable.getCurrentPart().isSliced()) {
			SlicerTab.getComputeSlicesBtn().setEnabled(true);
			TemplateTab.getComputeTemplateBtn().setEnabled(true);
		}

		// If a STL is loaded & sliced but layers not generated, enable the
		// generate layers button
		if ((viewObservable.getCurrentPart() != null) && viewObservable.getCurrentPart().isGenerated()) {
			setEnabledAt(indexOfTab("Review"), true);
			ReviewTab.getSelectedSlice().setText(" " + String.valueOf(viewObservable.getCurrentPart().getLayers()
					.get(viewObservable.getCurrentLayerNumber()).getSliceToSelect()));
			TemplateTab.getComputeTemplateBtn().setEnabled(true);
			// Add this to observe the optimizer
			viewObservable.getCurrentPart().getOptimizer().addObserver(this);
		}

		// If the auto-optimization is complete
		if (o instanceof Optimizer) {
			ReviewTab.getAutoOptimizeGPartBtn().setEnabled(true);
			ReviewTab.getAutoOptimizeLayerBtn().setEnabled(true);
		}
		revalidate();
	}

	/**
	 * To show the pop-up menu for loading model, etc.
	 */
	private class FileMenuButton extends JToggleButton {
		private static final long serialVersionUID = 5613899244422633632L;
		private FileMenuPopUp filePopup;

		public FileMenuButton() {
			// Visual options
			this.setFocusable(false);
			this.setBorder(null);
			this.setContentAreaFilled(false);

			// Setting up
			ImageIcon icon = new ImageIcon(
					new ImageIcon(this.getClass().getClassLoader().getResource("resources/" + "bars.png")).getImage()
							.getScaledInstance(24, 24, Image.SCALE_REPLICATE));
			this.setIcon(icon);

			ImageIcon selectedIcon = new ImageIcon(
					new ImageIcon(this.getClass().getClassLoader().getResource("resources/" + "blue-bars.png"))
							.getImage().getScaledInstance(24, 24, Image.SCALE_REPLICATE));

			filePopup = new FileMenuPopUp();

			// Actions listener
			this.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ev) {
					JToggleButton b = FileMenuButton.this;
					if (b.isSelected()) {
						filePopup.show(b, b.getLocationOnScreen().x, b.getLocationOnScreen().y);
						setIcon(selectedIcon);
					} else {
						filePopup.setVisible(false);
						setIcon(icon);
					}
				}
			});

			filePopup.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					FileMenuButton.this.setSelected(false);
					setIcon(icon);
				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					FileMenuButton.this.setSelected(false);
					setIcon(icon);
				}

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					FileMenuButton.this.setSelected(true);
					setIcon(icon);
				}

			});
		}

		private class FileMenuPopUp extends JPopupMenu {
			private static final long serialVersionUID = 3631645660924751860L;


			public FileMenuPopUp() {
				// Setting up
				JMenuItem openMenu = new FileMenuItem("Open Model", "file-o.png");
				JMenuItem closeMenu = new FileMenuItem("Close part", "times.png");
				JMenuItem openPatternConfigMenu = new FileMenuItem("Load pattern configuration", "conf-o.png");
				JMenuItem savePatternConfigMenu = new FileMenuItem("Save pattern configuration", "conf-save.png");
				JMenuItem exportMenu = new FileMenuItem("Export XML", "file-code-o.png");
				JMenuItem aboutMenu = new FileMenuItem("About", "info-circle.png");

				add(openMenu);
				add(closeMenu);
				addSeparator();
				add(openPatternConfigMenu);
				add(savePatternConfigMenu);
				add(exportMenu);
				addSeparator();
				add(aboutMenu);

				openMenu.setRolloverEnabled(true);

				// Actions listener
				openMenu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						final JFileChooser fc = new CustomFileChooser();
						fc.addChoosableFileFilter(new FileNameExtensionFilter("STL files", "stl"));
						fc.setSelectedFile(new File(CraftConfig.lastSlicedFile.replace("\n", "\\n")));
						int returnVal = fc.showOpenDialog(null);

						if (returnVal == JFileChooser.APPROVE_OPTION) {
							file = fc.getSelectedFile();
							Logger.updateStatus("Ready to slice " + file.getName());
							Ribbon.this.SlicerTab.setReadyToSlice(true);
						}
					}
				});

				closeMenu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Ribbon.this.setSelectedIndex(indexOfTab("Slicer"));
						Ribbon.this.viewObservable.setPart(null);
					}
				});
				
				openPatternConfigMenu.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						final JFileChooser fc = new CustomFileChooser();
						String ext = CraftConfigLoader.PATTERN_CONFIG_EXTENSION;
						fc.addChoosableFileFilter(new FileNameExtensionFilter(ext.toUpperCase() + " files", ext));
						String filePath = CraftConfig.lastPatternConfigFile.replace("\n", "\\n");
						if (filePath != null){
							fc.setSelectedFile(new File(filePath));
						}
						if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
							patternConfigFile = fc.getSelectedFile();
							CraftConfig.lastPatternConfigFile = patternConfigFile.getAbsolutePath();
							PatternConfig loadedConf = CraftConfigLoader.loadPatternConfig(patternConfigFile);
							if (loadedConf != null){
								TemplateTab.patternParametersContainer.setupPatternParameters(loadedConf);
								Logger.updateStatus("Pattern configuration loaded.");
							}
						}
					}
				});
				
				savePatternConfigMenu.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						final JFileChooser fc = new CustomFileChooser();
						String ext = CraftConfigLoader.PATTERN_CONFIG_EXTENSION;
						fc.addChoosableFileFilter(new FileNameExtensionFilter(ext.toUpperCase() + " files", ext));
						if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
							File f = fc.getSelectedFile();
							if (!f.getName().endsWith("." + ext)){
								f = new File(f.getPath() + "." + ext);
							}
							CraftConfigLoader.savePatternConfig(f);
						}
					}
				});

				exportMenu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						final JFileChooser fc = new JFileChooser();
						fc.addChoosableFileFilter(new FileNameExtensionFilter("XML files", "xml"));
						int returnVal = fc.showSaveDialog(null);

						GeneratedPart part = ViewObservable.getInstance().getCurrentPart();
						if ((returnVal == JFileChooser.APPROVE_OPTION) && (part != null) && part.isGenerated()) {
							XmlTool xt = new XmlTool(part, Paths.get(fc.getSelectedFile().getPath()));
							xt.writeXmlCode();
						} else {
							Logger.error("The XML file has not been generated");
						}
					}
				});

				aboutMenu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new AboutDialogWindow(null, "About MeshIneBits", true);
					}
				});
			}

			private class FileMenuItem extends JMenuItem {

				private static final long serialVersionUID = 3576752233844578812L;

				public FileMenuItem(String label, String iconName) {
					super(label);

					// Visual options
					setRolloverEnabled(true);
					this.setHorizontalAlignment(LEFT);
					this.setMargin(new Insets(0, 0, 0, 2));

					// Setting up
					try {
						ImageIcon icon = new ImageIcon(
								new ImageIcon(this.getClass().getClassLoader().getResource("resources/" + iconName))
										.getImage().getScaledInstance(22, 22, Image.SCALE_DEFAULT));
						this.setIcon(icon);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// Actions listener
					addMouseListener(new MouseListener() {
						@Override
						public void mouseClicked(MouseEvent e) {
						};

						@Override
						public void mouseEntered(MouseEvent e) {
							setArmed(true);
						}

						@Override
						public void mouseExited(MouseEvent e) {
							setArmed(false);
						}

						@Override
						public void mousePressed(MouseEvent e) {
						}

						@Override
						public void mouseReleased(MouseEvent e) {
						};
					});
				}
			}

			private class AboutDialogWindow extends JDialog {
				private static final long serialVersionUID = -3389839563563221684L;

				public AboutDialogWindow(JFrame parent, String title, boolean modal) {
					super(parent, title, modal);

					// Visual options
					Image windowIcon = new ImageIcon(this.getClass().getClassLoader().getResource("resources/icon.png"))
							.getImage();
					this.setIconImage(windowIcon);
					this.setSize(350, 160);
					this.setLocationRelativeTo(null);
					this.setResizable(false);

					// Setting up the dialog
					JPanel jp = new JPanel();
					jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));

					JLabel bg = new JLabel("");
					ImageIcon icon = new ImageIcon(
							new ImageIcon(this.getClass().getClassLoader().getResource("resources/MeshIneBits.png"))
									.getImage().getScaledInstance(248, 42, Image.SCALE_SMOOTH));
					bg.setIcon(icon);
					bg.setFont(new Font(null, Font.BOLD | Font.ITALIC, 120));
					bg.setForeground(new Color(0, 0, 0, 8));
					bg.setAlignmentX(Component.CENTER_ALIGNMENT);

					JLabel copyrightLabel = new JLabel("Copyright© 2016 Thibault Cassard & Nicolas Gouju.");
					copyrightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

					JButton helpFileBtn = new JButton("Open help file (PDF format)");
					helpFileBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

					jp.add(new JLabel(" "));
					jp.add(bg);
					jp.add(copyrightLabel);
					jp.add(new JLabel(" "));
					jp.add(helpFileBtn);
					AboutDialogWindow.this.getContentPane().add(jp, BorderLayout.CENTER);

					// Actions listener
					helpFileBtn.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							AboutDialogWindow.this.dispose();
							Desktop dt = Desktop.getDesktop();
							try {
								dt.open(new File(
										this.getClass().getClassLoader().getResource("resources/help.pdf").getPath()));
							} catch (IOException e1) {
								Logger.error("Failed to load help file");
							}
						}
					});

					this.setVisible(true);
				}
			}

		}

	}

	/**
	 * For reviewing result and auto-optimizing
	 */
	private class ReviewTab extends RibbonTab {

		private static final long serialVersionUID = -6062849183461607573L;
		private JLabel selectedSlice;
		private JButton autoOptimizeLayerBtn;
		private JButton autoOptimizeGPartBtn;

		public JButton getAutoOptimizeLayerBtn() {
			return autoOptimizeLayerBtn;
		}

		public JButton getAutoOptimizeGPartBtn() {
			return autoOptimizeGPartBtn;
		}

		public JLabel getSelectedSlice() {
			return selectedSlice;
		}

		public ReviewTab() {
			super();

			// Setting up

			// Options of display
			JCheckBox slicesCheckBox = new RibbonCheckBox("Show slices") {
				private static final long serialVersionUID = 7090657482323001875L;

				@Override
				public void update(Observable o, Object arg) {
					setSelected(viewObservable.showSlices());
				}
			};

			JCheckBox liftPointsCheckBox = new RibbonCheckBox("Show lift points") {
				private static final long serialVersionUID = 7090657482323001875L;

				@Override
				public void update(Observable o, Object arg) {
					setSelected(viewObservable.showLiftPoints());
				}
			};
			JCheckBox previousLayerCheckBox = new RibbonCheckBox("Show previous layer") {
				private static final long serialVersionUID = 7090657482323001875L;

				@Override
				public void update(Observable o, Object arg) {
					setSelected(viewObservable.showPreviousLayer());
				}
			};
			JCheckBox cutPathsCheckBox = new RibbonCheckBox("Show cut paths") {
				private static final long serialVersionUID = 7090657482323001875L;

				@Override
				public void update(Observable o, Object arg) {
					setSelected(viewObservable.showCutPaths());
				}
			};

			JCheckBox irregularBitsCheckBox = new RibbonCheckBox("Show irregular bits") {
				private static final long serialVersionUID = 7090657482323001875L;

				@Override
				public void update(Observable o, Object arg) {
					setSelected(viewObservable.showIrregularBits());
				}
			};

			OptionsContainer displayCont = new OptionsContainer("Display options");
			displayCont.add(slicesCheckBox);
			displayCont.add(liftPointsCheckBox);
			displayCont.add(previousLayerCheckBox);
			displayCont.add(cutPathsCheckBox);
			displayCont.add(irregularBitsCheckBox);

			add(displayCont);

			// For manipulating selected slices
			add(new TabContainerSeparator());

			OptionsContainer sliceSelectionCont = new OptionsContainer("Selected slice");
			sliceSelectionCont.setLayout(new BoxLayout(sliceSelectionCont, BoxLayout.PAGE_AXIS));

			ButtonIcon upArrow = new ButtonIcon("", "angle-up.png");
			upArrow.setAlignmentX(CENTER_ALIGNMENT);
			upArrow.setHorizontalAlignment(SwingConstants.CENTER);

			selectedSlice = new JLabel();
			selectedSlice.setFont(new Font("Helvetica", Font.PLAIN, 20));
			selectedSlice.setHorizontalAlignment(SwingConstants.CENTER);
			selectedSlice.setPreferredSize(new Dimension(90, 25));
			selectedSlice.setAlignmentX(CENTER_ALIGNMENT);

			ButtonIcon downArrow = new ButtonIcon("", "angle-down.png");
			downArrow.setHorizontalAlignment(SwingConstants.CENTER);
			downArrow.setAlignmentX(CENTER_ALIGNMENT);

			sliceSelectionCont.add(upArrow);
			sliceSelectionCont.add(selectedSlice);
			sliceSelectionCont.add(downArrow);

			add(sliceSelectionCont);

			// For modifying the chosen bit
			add(new TabContainerSeparator());
			OptionsContainer modifCont = new OptionsContainer("Replace bit");
			JButton replaceBitBtn1 = new ButtonIcon("", "cut-length.png", true, 80, 25);
			JButton replaceBitBtn2 = new ButtonIcon("", "cut-width.png", true, 80, 25);
			JButton replaceBitBtn3 = new ButtonIcon("", "cut-quart.png", true, 80, 25);
			JButton deleteBitBtn = new ButtonIcon("", "delete-bit.png", true, 80, 25);
			JButton replaceByFullBitBtn = new ButtonIcon("", "full-bit.png", true, 80, 25);
			modifCont.add(replaceBitBtn1);
			modifCont.add(replaceBitBtn2);
			modifCont.add(replaceBitBtn3);
			modifCont.add(deleteBitBtn);
			modifCont.add(replaceByFullBitBtn);

			add(modifCont);

			// For auto-optimizing
			add(new TabContainerSeparator());
			OptionsContainer autoOptimizeCont = new OptionsContainer("Auto-optimizing bits' distribution");
			autoOptimizeLayerBtn = new ButtonIcon("Auto-optimize this layer", "cog.png");
			autoOptimizeGPartBtn = new ButtonIcon("Auto-optimize this generated part", "cog.png");
			autoOptimizeLayerBtn.setToolTipText(
					"Trying to minimize the irregular bits in this layer. This does not guarantee all irregularities eliminated.");
			autoOptimizeGPartBtn.setToolTipText(
					"Trying to minimize the irregular bits in this generated part. This does not guarantee all irregularities eliminated.");
			autoOptimizeCont.add(autoOptimizeLayerBtn);
			autoOptimizeCont.add(autoOptimizeGPartBtn);

			add(autoOptimizeCont);
			
			// For 3d view
			add(new TabContainerSeparator());
			OptionsContainer processingViewCont = new OptionsContainer("3D view");
			ButtonIcon processingViewBtn = new ButtonIcon("Open 3D view", "3D.png");
			processingViewCont.add(processingViewBtn);
			add(processingViewCont);
			
			
			
			autoOptimizeLayerBtn.setEnabled(false);
			autoOptimizeGPartBtn.setEnabled(false);
			

			/////////////////////////////////////////////
			// Actions listener

			// For display options
			slicesCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					viewObservable.toggleShowSlice(slicesCheckBox.isSelected());
				}
			});

			liftPointsCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					viewObservable.toggleShowLiftPoints(liftPointsCheckBox.isSelected());
				}
			});

			previousLayerCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					viewObservable.toggleShowPreviousLayer(previousLayerCheckBox.isSelected());
				}
			});

			cutPathsCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					viewObservable.toggleShowCutPaths(cutPathsCheckBox.isSelected());
				}
			});

			irregularBitsCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					viewObservable.toggleShowIrregularBits(irregularBitsCheckBox.isSelected());
				}
			});

			// For selecting slices
			upArrow.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Layer currentLayer = viewObservable.getCurrentPart().getLayers()
							.get(viewObservable.getCurrentLayerNumber());
					currentLayer.setSliceToSelect(currentLayer.getSliceToSelect() + 1);
				}
			});

			downArrow.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Layer currentLayer = viewObservable.getCurrentPart().getLayers()
							.get(viewObservable.getCurrentLayerNumber());
					currentLayer.setSliceToSelect(currentLayer.getSliceToSelect() - 1);
				}
			});

			// For replacing bits
			replaceBitBtn1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					replaceSelectedBit(100, 50);
				}
			});

			replaceBitBtn2.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					replaceSelectedBit(50, 100);
				}
			});

			replaceBitBtn3.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					replaceSelectedBit(50, 50);
				}
			});

			deleteBitBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					replaceSelectedBit(0, 0);
				}
			});

			replaceByFullBitBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					replaceSelectedBit(100, 100);
				}
			});

			// For auto-optimizing
			autoOptimizeLayerBtn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					autoOptimizeLayerBtn.setEnabled(false);
					autoOptimizeGPartBtn.setEnabled(false);
					GeneratedPart currentPart = viewObservable.getCurrentPart();
					Layer currentLayer = currentPart.getLayers().get(viewObservable.getCurrentLayerNumber());
					currentPart.getOptimizer().automaticallyOptimizeLayer(currentLayer);
				}
			});

			autoOptimizeGPartBtn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					autoOptimizeLayerBtn.setEnabled(false);
					autoOptimizeGPartBtn.setEnabled(false);
					GeneratedPart currentPart = viewObservable.getCurrentPart();
					currentPart.getOptimizer().automaticallyOptimizeGeneratedPart(currentPart);
				}
			});
			
			
			//For 3D view
			processingViewBtn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ProcessingView.startProcessingView(null);
				}
			});

		}

		private void replaceSelectedBit(double percentageLength, double percentageWidth) {
			ViewObservable vo = ViewObservable.getInstance();
			GeneratedPart part = vo.getCurrentPart();
			Layer layer = part.getLayers().get(vo.getCurrentLayerNumber());

			if (vo.getSelectedBitKey() == null) {
				Logger.warning("There is no bit selected");
				return;
			}

			Bit3D bit = layer.getBit3D(vo.getSelectedBitKey());

			vo.setSelectedBitKey(layer.replaceBit(bit, percentageLength, percentageWidth));
		}

		/**
		 * Options of viewing.
		 */
		private class RibbonCheckBox extends JCheckBox implements Observer {

			private static final long serialVersionUID = 9143671052675167109L;

			public RibbonCheckBox(String label) {
				super(label);
				// Visual options
				this.setBackground(Color.WHITE);
				this.setFocusable(false);

				// Setting up
				viewObservable.addObserver(this);
			}

			@Override
			public void update(Observable o, Object arg) {

			}

		}
	}

	/**
	 * For slicing object into multiple slices
	 */
	private class SlicerTab extends RibbonTab {

		private static final long serialVersionUID = -2435250564072409684L;
		private JLabel fileSelectedLabel;
		private JButton computeSlicesBtn;

		public JButton getComputeSlicesBtn() {
			return computeSlicesBtn;
		}

		public void setReadyToSlice(boolean ready) {
			fileSelectedLabel.setText(file.getName());
			computeSlicesBtn.setEnabled(true);
		}

		public SlicerTab() {
			super();

			// Setting up
			OptionsContainer slicerCont = new OptionsContainer("Slicer options");
			LabeledSpinner sliceHeightSpinner = new LabeledSpinner("sliceHeight", setupAnnotations.get("sliceHeight"));
			LabeledSpinner firstSliceHeightPercentSpinner = new LabeledSpinner("firstSliceHeightPercent",
					setupAnnotations.get("firstSliceHeightPercent"));
			slicerCont.add(sliceHeightSpinner);
			slicerCont.add(firstSliceHeightPercentSpinner);

			OptionsContainer computeCont = new OptionsContainer("Compute");
			computeSlicesBtn = new ButtonIcon("Slice model", "gears.png");
			computeSlicesBtn.setHorizontalAlignment(SwingConstants.CENTER);
			computeSlicesBtn.setEnabled(false);
			fileSelectedLabel = new JLabel("No file selected");
			computeCont.add(fileSelectedLabel);
			computeCont.add(computeSlicesBtn);

			add(slicerCont);
			add(new TabContainerSeparator());
			add(computeCont);

			// Actions listener
			addConfigSpinnerChangeListener(sliceHeightSpinner, "sliceHeight");

			addConfigSpinnerChangeListener(firstSliceHeightPercentSpinner, "firstSliceHeightPercent");

			computeSlicesBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {

					if (Ribbon.this.viewObservable.getCurrentPart() != null) {
						Ribbon.this.viewObservable.setPart(null);
					}

					if (file != null) {
						computeSlicesBtn.setEnabled(false);
						try {
							MeshIneBitsMain.sliceModel(file.toString());
						} catch (Exception e1) {
							e1.printStackTrace();
							StringBuilder sb = new StringBuilder();
							sb.append(e1.toString());
							sb.append("\n");
							for (StackTraceElement el : e1.getStackTrace()) {
								sb.append(el.toString());
								sb.append("\n");
							}
							JOptionPane.showMessageDialog(null, sb, "Exception", JOptionPane.ERROR_MESSAGE);
							computeSlicesBtn.setEnabled(true);
						}
					}
				}
			});

		}
	}

	/**
	 * For customizing parameters of the chosen pattern
	 */
	private class TemplateTab extends RibbonTab {

		private static final long serialVersionUID = -2963705108403089250L;

		private JButton computeTemplateBtn;
		private PatternParametersContainer patternParametersContainer;

		public JButton getComputeTemplateBtn() {
			return computeTemplateBtn;
		}

		public TemplateTab() {
			super();

			// Setting up
			// Bits options
			OptionsContainer bitsCont = new OptionsContainer("Bits options");
			LabeledSpinner bitThicknessSpinner = new LabeledSpinner("bitThickness",
					setupAnnotations.get("bitThickness"));
			LabeledSpinner bitWidthSpinner = new LabeledSpinner("bitWidth", setupAnnotations.get("bitWidth"));
			LabeledSpinner bitLengthSpinner = new LabeledSpinner("bitLength", setupAnnotations.get("bitLength"));
			bitsCont.add(bitThicknessSpinner);
			bitsCont.add(bitWidthSpinner);
			bitsCont.add(bitLengthSpinner);

			// Pattern choice
			GalleryContainer patternGallery = new GalleryContainer("Pattern");

			// Template options
			patternParametersContainer = new PatternParametersContainer("Pattern parameters");
			patternParametersContainer.setupPatternParameters();

			// Computing options
			OptionsContainer computeCont = new OptionsContainer("Compute");
			computeCont.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
			computeTemplateBtn = new ButtonIcon("Generate layers", "cog.png");
			computeTemplateBtn.setEnabled(false);
			computeTemplateBtn.setHorizontalAlignment(SwingConstants.CENTER);
			LabeledSpinner layersOffsetSpinner = new LabeledSpinner("layersOffset",
					setupAnnotations.get("layersOffset"));
			LabeledSpinner minPercentageOfSlicesSpinner = new LabeledSpinner("minPercentageOfSlices",
					setupAnnotations.get("minPercentageOfSlices"));
			LabeledSpinner defaultSliceToSelectSpinner = new LabeledSpinner("defaultSliceToSelect",
					setupAnnotations.get("defaultSliceToSelect"));
			LabeledSpinner suctionCupDiameter = new LabeledSpinner("suckerDiameter", setupAnnotations.get("suckerDiameter"));
			computeCont.add(layersOffsetSpinner);
			computeCont.add(minPercentageOfSlicesSpinner);
			computeCont.add(defaultSliceToSelectSpinner);
			computeCont.add(suctionCupDiameter);
			computeCont.add(new JLabel()); // dummy
			computeCont.add(computeTemplateBtn);

			// Overall
			add(bitsCont);
			add(new TabContainerSeparator());
			add(patternGallery);
			add(new TabContainerSeparator());
			// add(patternParameters);
			add(patternParametersContainer);
			add(new TabContainerSeparator());
			add(computeCont);

			// Actions listener
			addConfigSpinnerChangeListener(bitThicknessSpinner, "bitThickness");
			addConfigSpinnerChangeListener(bitWidthSpinner, "bitWidth");
			addConfigSpinnerChangeListener(bitLengthSpinner, "bitLength");
			addConfigSpinnerChangeListener(layersOffsetSpinner, "layersOffset");
			addConfigSpinnerChangeListener(minPercentageOfSlicesSpinner, "minPercentageOfSlices");
			addConfigSpinnerChangeListener(defaultSliceToSelectSpinner, "defaultSliceToSelect");

			computeTemplateBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					computeTemplateBtn.setEnabled(false);
					Ribbon.this.viewObservable.getCurrentPart().buildBits2D();
					CraftConfigLoader.saveConfig(null);
				}
			});
		}

		private class GalleryContainer extends OptionsContainer {

			private static final long serialVersionUID = 5081506030712556983L;

			/**
			 * For the display
			 */
			private JLabel templateChosen;
			private JPopupMenu templatesMenu;

			public GalleryContainer(String title) {
				super(title);
				// this.setLayout(new GridLayout(1, 2, 3, 3));
				this.setLayout(new BorderLayout());
				// For the chosen template
				PatternTemplate defaultTemplate = CraftConfig.templateChoice;
				ImageIcon image = new ImageIcon(
						this.getClass().getClassLoader().getResource("resources/" + defaultTemplate.getIconName()));
				ImageIcon icon = new ImageIcon(image.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT));
				this.templateChosen = new JLabel(defaultTemplate.getCommonName(), icon, SwingConstants.CENTER);
				this.templateChosen.setPreferredSize(new Dimension(150, 50));
				this.templateChosen.setToolTipText(descriptiveText(defaultTemplate));
				this.add(templateChosen, BorderLayout.CENTER);
				// For the menu
				image = new ImageIcon(this.getClass().getClassLoader().getResource("resources/" + "angle-down.png"));
				icon = new ImageIcon(image.getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
				JButton choosingTemplateBtn = new JButton(icon);
				this.add(choosingTemplateBtn, BorderLayout.SOUTH);
				choosingTemplateBtn.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						templatesMenu.show(choosingTemplateBtn, 0, choosingTemplateBtn.getHeight());
					}
				});
				this.templatesMenu = new TemplatesMenu(this);
			}

			protected String descriptiveText(PatternTemplate template) {
				StringBuilder str = new StringBuilder();
				str.append("<html><div>");
				str.append("<p><strong>" + template.getCommonName() + "</strong></p>");
				str.append("<p>" + template.getDescription() + "</p>");
				str.append("<p><strong>How-To-Use</strong><br/>" + template.getHowToUse() + "</p>");
				str.append("</div></html>");
				return str.toString();
			}

			/**
			 * All the available templates for choosing
			 * 
			 * @author NHATHAN
			 */
			private class TemplatesMenu extends JPopupMenu {

				/**
				 * 
				 */
				private static final long serialVersionUID = 4906068175556528411L;
				private GalleryContainer parent;

				public TemplatesMenu(GalleryContainer galleryContainer) {
					super("...");
					this.parent = galleryContainer;
					// Load all templates we have
					// TODO
					// Load all saved templates
					CraftConfig.templatesLoaded = new Vector<PatternTemplate>(
							Arrays.asList(CraftConfig.templatesPreloaded));
					for (PatternTemplate template : CraftConfig.templatesLoaded) {
						this.addNewTemplate(template);
					}
					// A button to load external template
					this.addSeparator();
					JMenuItem loadExternalTemplateBtn = new JMenuItem("More...");
					this.add(loadExternalTemplateBtn);
					// TODO
					// Implement function "add external pattern"
				}

				public void addNewTemplate(PatternTemplate template) {
					JMenuItem newTemplate = new JMenuItem(template.getCommonName());
					ImageIcon image = new ImageIcon(
							this.getClass().getClassLoader().getResource("resources/" + template.getIconName()));
					ImageIcon icon = new ImageIcon(image.getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
					newTemplate.setIcon(icon);
					this.add(newTemplate);
					newTemplate.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							parent.refreshTemplateChosen(template);
							CraftConfig.templateChoice = template;
							patternParametersContainer.setupPatternParameters();
						}
					});
				}
			}

			public void refreshTemplateChosen(PatternTemplate template) {
				this.templateChosen.setText(template.getCommonName());
				ImageIcon image = new ImageIcon(
						this.getClass().getClassLoader().getResource("resources/" + template.getIconName()));
				ImageIcon icon = new ImageIcon(image.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT));
				this.templateChosen.setIcon(icon);
				this.templateChosen.setToolTipText(descriptiveText(template));
			}
		}

		/**
		 * Contains specialized parameters for the chosen pattern
		 */
		private class PatternParametersContainer extends OptionsContainer {

			/**
			 * 
			 */
			private static final long serialVersionUID = -5486094986597798629L;

			public PatternParametersContainer(String title) {
				super(title);
			}

			/**
			 * Remove all loaded components in containers then load new
			 * parameters from the currently chosen pattern.
			 */
			public void setupPatternParameters() {
				this.removeAll();
				for (PatternParameterConfig paramConfig : CraftConfig.templateChoice.getPatternConfig().values()) {
					if (paramConfig.getCurrentValue() instanceof Double) {
						LabeledSpinner spinner = new LabeledSpinner(paramConfig);
						addPatternParameterListener(spinner, paramConfig);
						this.add(spinner);
					}
					if (paramConfig.getCurrentValue() instanceof List<?>) {
						LabeledListReceiver listReceiver = new LabeledListReceiver(paramConfig);
						this.add(listReceiver);
					}
				}
			}

			/**
			 * Remove all loaded components in containers then load new
			 * parameters from the given <tt>config</tt> (input will be filtered
			 * by attribute's name, type)
			 * 
			 * @param config
			 */
			public void setupPatternParameters(PatternConfig config) {
				this.removeAll();
				for (PatternParameterConfig paramConfig : CraftConfig.templateChoice.getPatternConfig().values()) {
					if (paramConfig.getCurrentValue() instanceof Double) {
						// Update current value
						PatternParameterConfig importParam = config.get(paramConfig.uniqueName);
						if (importParam != null){
							paramConfig.setCurrentValue(importParam.getCurrentValue());
						}
						// Then show
						LabeledSpinner spinner = new LabeledSpinner(paramConfig);
						addPatternParameterListener(spinner, paramConfig);
						this.add(spinner);
					}
					if (paramConfig.getCurrentValue() instanceof List<?>) {
						// Update current value
						PatternParameterConfig importParam = config.get(paramConfig.uniqueName);
						if (importParam != null){
							paramConfig.setCurrentValue(importParam.getCurrentValue());
						}
						// Then show
						LabeledListReceiver listReceiver = new LabeledListReceiver(paramConfig);
						this.add(listReceiver);
					}
				}
			}

		}
	}

	/**
	 * Only for double-type static field of {@link CraftConfig}.
	 * 
	 * @param spinner
	 * @param configFieldName
	 */
	private void addConfigSpinnerChangeListener(LabeledSpinner spinner, String configFieldName) {
		spinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				try {
					Field f = CraftConfig.class.getField(configFieldName);
					f.setDouble(null, spinner.getValue());
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void addPatternParameterListener(LabeledSpinner spinner, PatternParameterConfig config) {
		spinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				config.setCurrentValue(spinner.getValue());
			}
		});
	}

	/**
	 * Get annotations for fields from {@link CraftConfig}
	 * 
	 * @return names of attributes associated by their annotations
	 */
	private HashMap<String, Setting> loadAnnotations() {
		HashMap<String, Setting> result = new HashMap<>();
		try {
			// Get all declared attributes
			Field[] fieldList = Class.forName("meshIneBits.config.CraftConfig").getDeclaredFields();
			for (Field field : fieldList) {
				result.put(field.getName(), field.getAnnotation(Setting.class));
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}
}