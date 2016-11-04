package com.skyguy126.gci;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import com.alee.laf.scroll.WebScrollBar;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderUI implements GLEventListener, MouseWheelListener, MouseMotionListener, MouseListener, KeyListener,
		LogWriter, DropTargetListener {

	private Frame frame;
	private MenuBar menuBar;

	private JFrame detailsFrame;
	private JFrame controlFrame;

	private JSlider timeSlider;
	private JSlider generationMultiplierSlider;
	private JSlider scaleSlider;

	private CustomJButton playButton;
	private CustomJButton screenshotButton;
	private CustomJButton toggleBoundingBoxButton;

	private CustomJDialog parseErrorDialog;
	private CustomJDialog invalidExtensionDialog;
	private CustomJDialog crashDialog;
	private LoadingDialog loadingDialog;
	private InformationDialog informationDialog;

	private CustomJTextField currentCmdText;
	private CustomJTextField currentFeedRateText;
	private CustomJTextField currentSpindleSpeedText;
	private CustomJTextField measurementModeText;

	private JTextPane boundsText;
	private JTextPane logTextArea;

	private TextRenderer textRenderer;
	private TextRenderer textRenderer3D;
	private GLCapabilities glcaps;
	private GLCanvas glcanvas;
	private Animator animator;
	private GLU glu;

	private SimpleAttributeSet logFormat;
	private SimpleAttributeSet boundsFormat;

	private ReentrantLock glLock;
	private ReentrantLock animateLock;

	private Cursor panCursor;
	private Cursor rotateCursor;

	private volatile String currentFilePath;

	private double lastX;
	private double lastY;

	private int glHeight;
	private int glWidth;

	private volatile float zoomDistance;
	private volatile float curX;
	private volatile float curY;
	private volatile float curAngleX;
	private volatile float curAngleY;
	private volatile double currentTimePercent;

	private float minZ;
	private float maxZ;
	private float minY;
	private float maxY;
	private float minX;
	private float maxX;

	private int totalTicks;
	private volatile int loopNum;

	private volatile boolean lockHorizAxis;
	private volatile boolean lockVertAxis;
	private volatile boolean decreaseSensitivity;
	private volatile boolean takeScreenshot;
	private volatile boolean screenshotToClipboard;
	private volatile boolean isPlaying;
	private volatile boolean displayFileDropMessage;
	private volatile boolean displayBoundingBox;
	private volatile boolean displayAxisLabels;
	private volatile boolean displayAxis;
	private volatile boolean boundsExist;

	private volatile ByteBuffer screenshotBuffer;
	private volatile ArrayList<float[][]> vertexValues;
	private volatile ArrayList<Color> curLineColor;
	private volatile ArrayList<String> spindleSpeedText;
	private volatile ArrayList<String> feedRateText;
	private volatile ArrayList<String> currentCommandText;
	private volatile ArrayList<Integer> currentTimeScale;

	private Runnable showLoadingDialog = new Runnable() {
		@Override
		public void run() {
			loadingDialog.setVisible(true);
		}
	};

	private Runnable dismissLoadingDialog = new Runnable() {
		@Override
		public void run() {
			loadingDialog.setVisible(false);
		}
	};

	private Runnable showParseErrorDialog = new Runnable() {
		@Override
		public void run() {
			parseErrorDialog.setVisible(true);
		}
	};
	
	private Runnable dismissParseErrorDialog = new Runnable() {
		@Override
		public void run() {
			parseErrorDialog.setVisible(false);
		}
	};

	private Runnable showInvalidExtensionDialog = new Runnable() {
		@Override
		public void run() {
			invalidExtensionDialog.setVisible(true);
		}
	};
	
	private Runnable dismissInvalidExtensionDialog = new Runnable() {
		@Override
		public void run() {
			invalidExtensionDialog.setVisible(false);
		}
	};
	
	private Runnable showCrashDialog = new Runnable() {
		@Override
		public void run() {
			crashDialog.setVisible(true);
		}
	};
	
	private Runnable dismissCrashDialog = new Runnable() {
		@Override
		public void run() {
			crashDialog.setVisible(false);
			exit(-1);
		}
	};

	private Runnable showInformationDialog = new Runnable() {
		@Override
		public void run() {
			informationDialog.setVisible(true);
		}
	};

	private Thread animateVertexValues = new Thread() {
		@Override
		public void run() {
			Logger.debug("Vertex animation thread started");

			while (true) {
				animateLock.lock();
				loopNum = (int) (totalTicks * currentTimePercent);

				if (loopNum > 0) {
					currentCmdText.setText(currentCommandText.get(loopNum - 1));
					currentSpindleSpeedText.setText(spindleSpeedText.get(loopNum - 1));
					currentFeedRateText.setText(feedRateText.get(loopNum - 1));
				} else {
					currentCmdText.setText("");
					currentSpindleSpeedText.setText("");
					currentFeedRateText.setText("");
				}

				animateLock.unlock();

				try {
					Thread.sleep(Shared.TIME_SCALE);
				} catch (InterruptedException e) {
					Logger.error("Animate vertex values exception: {}", e);
					return;
				}
			}
		}
	};

	private Thread performPlayback = new Thread() {
		@Override
		public void run() {
			Logger.debug("Playback thread started");

			long timeScale = 1;

			while (true) {

				if (isPlaying) {
					Logger.debug("Playing: {}", currentTimePercent);

					if (currentTimePercent < 1.0) {
						currentTimePercent += 0.001;
						timeSlider.setValue((int) (currentTimePercent * timeSlider.getMaximum()));
					} else {
						stopPlayback();
					}
				}
				
				int position = (int) (currentTimePercent * currentTimeScale.size()) - 1;
				
				if (position <= 0) 
					timeScale = 1L;
				else
					timeScale = (long) currentTimeScale.get(position);

				try {
					Thread.sleep(Shared.TIME_SCALE / timeScale);
				} catch (InterruptedException e) {
					Logger.error("Perform playback thread exception: {}", e);
					return;
				}
			}
		}
	};

	private class FileLoader extends Thread {

		private String filePath;

		public FileLoader(String f) {
			this.filePath = f;
		}

		@Override
		public void run() {
			runOnNewThread(showLoadingDialog);
			stopPlayback();

			Logger.info("Attempting to load: {}", filePath);

			long startTime = System.currentTimeMillis();

			Parser parser = new Parser(filePath);
			boolean parseSuccess = parser.parse();
			boolean interpSuccess = false;

			if (parseSuccess) {

				if (parser.getCoordinateMode() != CoordinateMode.ABSOLUTE) {
					Logger.error("Relative coordinate mode not supported yet.");
					runOnNewThread(dismissLoadingDialog);
					return;
				}

				Interpreter interpreter = new Interpreter(parser.getGCodeArray());
				interpSuccess = interpreter.generateAbsolute();

				if (interpSuccess) {

					glLock.lock();
					animateLock.lock();

					vertexValues = interpreter.getVertexValues();
					curLineColor = interpreter.getCurrentLineColor();
					feedRateText = interpreter.getFeedRateText();
					currentCommandText = interpreter.getCurrentCommandText();
					spindleSpeedText = interpreter.getSpindleSpeedText();
					currentTimeScale = interpreter.getCurrentTimeScale();
					totalTicks = interpreter.getTotalTicks();
					loopNum = (int) (totalTicks * currentTimePercent);

					if (parser.getMeasurementMode() == MeasurementMode.INCH) {
						measurementModeText.setText("INCH");
					} else if (parser.getMeasurementMode() == MeasurementMode.MILLIMETER) {
						measurementModeText.setText("MM");
					}

					minX = interpreter.getBounds()[0];
					maxX = interpreter.getBounds()[1];
					minY = interpreter.getBounds()[2];
					maxY = interpreter.getBounds()[3];
					minZ = interpreter.getBounds()[4];
					maxZ = interpreter.getBounds()[5];

					boundsExist = true;

					float minXT = interpreter.getBounds()[0] / Shared.SEGMENT_SCALE_MULTIPLIER;
					float maxXT = interpreter.getBounds()[1] / Shared.SEGMENT_SCALE_MULTIPLIER;
					float minYT = interpreter.getBounds()[2] / Shared.SEGMENT_SCALE_MULTIPLIER;
					float maxYT = interpreter.getBounds()[3] / Shared.SEGMENT_SCALE_MULTIPLIER;
					float minZT = interpreter.getBounds()[4] / Shared.SEGMENT_SCALE_MULTIPLIER;
					float maxZT = interpreter.getBounds()[5] / Shared.SEGMENT_SCALE_MULTIPLIER;

					StyleConstants.setForeground(boundsFormat, Color.WHITE);
					StyleConstants.setBold(boundsFormat, true);
					StyleConstants.setAlignment(boundsFormat, StyleConstants.ALIGN_CENTER);
					boundsText.setText("");
					StyledDocument doc = boundsText.getStyledDocument();

					try {
						doc.setParagraphAttributes(0, doc.getLength(), boundsFormat, false);
						doc.insertString(doc.getLength(), "\n", boundsFormat);
						doc.insertString(doc.getLength(), "X min: " + minXT + "    X max: " + maxXT + "\n",
								boundsFormat);
						doc.insertString(doc.getLength(), "Y min: " + minYT + "    Y max: " + maxYT + "\n",
								boundsFormat);
						doc.insertString(doc.getLength(), "Z min: " + minZT + "    Z max: " + maxZT + "\n",
								boundsFormat);
					} catch (BadLocationException e) {
						Logger.error("Error setting bounds: {}", e);
					}

					Logger.debug("Bounding box: {}", Arrays.toString(interpreter.getBounds()));

					frame.setTitle("GCI - " + currentFilePath);

					long elapsedTime = System.currentTimeMillis() - startTime;

					Logger.info("Loaded file in {}ms", elapsedTime);

					glLock.unlock();
					animateLock.unlock();
				}
			}

			runOnNewThread(dismissLoadingDialog);
			frame.requestFocus();

			if (!parseSuccess || !interpSuccess)
				runOnNewThread(showParseErrorDialog);
		}
	}

	private class Screenshot extends Thread implements ClipboardOwner {

		private ByteBuffer buffer;
		private int height;
		private int width;
		private boolean clipboard;

		public Screenshot(ByteBuffer b, int h, int w, boolean c) {
			this.buffer = b;
			this.height = h;
			this.width = w;
			this.clipboard = c;
		}

		@Override
		public void run() {

			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics g = img.getGraphics();

			for (int h = 0; h < height; h++) {
				for (int w = 0; w < width; w++) {
					g.setColor(new Color(buffer.get() * 2, buffer.get() * 2, buffer.get() * 2));
					g.drawRect(w, height - h, 1, 1);
				}
			}

			g.dispose();
			Logger.debug("Copied bytes to buffered image");

			if (this.clipboard) {
				try {
					TransferableImage timg = new TransferableImage(img);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(timg, this);
					Logger.info("Copied screenshot to clipboard");
				} catch (Exception e) {
					Logger.error("Screenshot to clipboard failed: {}", e);
				}
			} else {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Save Screenshot");
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.setApproveButtonText("Save");

				if (fileChooser.showOpenDialog(frame.getOwner()) == JFileChooser.APPROVE_OPTION) {
					String file = fileChooser.getSelectedFile().getAbsolutePath().toString();

					// If no extension is given append one
					if (!file.endsWith(".png"))
						file += ".png";

					Logger.debug("Save Directory: {}", file);

					try {
						ImageIO.write(img, "png", new File(file));
					} catch (IOException e) {
						Logger.error("Screenshot failed: {}", e);
					}

					Logger.info("Screenshot saved to: {}", file);

				} else {
					Logger.debug("Screenshot save cancelled");
					return;
				}
			}
		}

		@Override
		public void lostOwnership(Clipboard c, Transferable t) {
			Logger.debug("Lost clipboard ownership");
		}
	}

	private class TransferableImage implements Transferable {
		private Image image;

		public TransferableImage(Image i) {
			this.image = i;
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (isDataFlavorSupported(flavor)) {
				return image;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.imageFlavor };
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor == DataFlavor.imageFlavor;
		}
	}

	public RenderUI(CountDownLatch status) {
		Configurator.defaultConfig().writer(new LogController(this)).level(Shared.LOG_LEVEL).activate();
		Logger.debug("RUI initiated");

		this.zoomDistance = 100;
		this.curX = 0;
		this.curY = 0;
		this.curAngleX = 0;
		this.curAngleY = 0;

		this.lockHorizAxis = false;
		this.lockVertAxis = false;
		this.decreaseSensitivity = false;
		this.takeScreenshot = false;
		this.screenshotToClipboard = true;
		this.isPlaying = false;
		this.displayFileDropMessage = false;
		this.displayBoundingBox = false;
		this.displayAxis = true;
		this.displayAxisLabels = true;
		this.boundsExist = false;

		this.currentFilePath = "";

		this.vertexValues = new ArrayList<float[][]>();
		this.curLineColor = new ArrayList<Color>();
		this.spindleSpeedText = new ArrayList<String>();
		this.feedRateText = new ArrayList<String>();
		this.currentCommandText = new ArrayList<String>();
		this.currentTimeScale = new ArrayList<Integer>();

		this.minX = 0f;
		this.maxX = 0f;
		this.minY = 0f;
		this.maxY = 0f;
		this.minZ = 0f;
		this.maxZ = 0f;

		this.currentTimePercent = 1.0;
		this.logFormat = new SimpleAttributeSet();
		this.boundsFormat = new SimpleAttributeSet();
		this.glLock = new ReentrantLock();
		this.animateLock = new ReentrantLock();

		if (Shared.USE_SYSTEM_LOOK_AND_FEEL) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				Logger.error(e);
			}
		}

		glcaps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		glcaps.setDoubleBuffered(true);
		glcaps.setHardwareAccelerated(true);

		glcanvas = new GLCanvas(glcaps);
		glcanvas.setSize(720, 720);
		glcanvas.addKeyListener(this);
		glcanvas.addMouseListener(this);
		glcanvas.addGLEventListener(this);
		glcanvas.addMouseWheelListener(this);
		glcanvas.addMouseMotionListener(this);

		new DropTarget(glcanvas, DnDConstants.ACTION_COPY_OR_MOVE, this, true, null);

		this.glHeight = glcanvas.getHeight();
		this.glWidth = glcanvas.getWidth();

		frame = new Frame("GCI");
		frame.setSize(800, 800);
		frame.setMinimumSize(new Dimension(400, 400));
		frame.setResizable(true);
		frame.add(glcanvas);
		frame.setLocationRelativeTo(null);
		frame.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("res/main_ico.png")).getImage());
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				exit(0);
			}

			@Override
			public void windowActivated(WindowEvent e) {
				Logger.debug("Main frame gained focus");

				controlFrame.setFocusableWindowState(false);
				detailsFrame.setFocusableWindowState(false);
				controlFrame.toFront();
				detailsFrame.toFront();
				frame.toFront();
				frame.requestFocus();
				controlFrame.setFocusableWindowState(true);
				detailsFrame.setFocusableWindowState(true);
			}

			@Override
			public void windowIconified(WindowEvent e) {
				detailsFrame.setState(JFrame.ICONIFIED);
				controlFrame.setState(JFrame.ICONIFIED);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				detailsFrame.setState(JFrame.NORMAL);
				controlFrame.setState(JFrame.NORMAL);
				frame.requestFocus();
			}
		});

		this.rotateCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				new ImageIcon(getClass().getClassLoader().getResource("res/rotate_pointer.png")).getImage(),
				new Point(0, 0), "rotate");

		this.panCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				new ImageIcon(getClass().getClassLoader().getResource("res/pan_pointer.png")).getImage(),
				new Point(0, 0), "pan");

		loadingDialog = new LoadingDialog(frame, "Please Wait...", "Loading...",
				new ImageIcon(getClass().getClassLoader().getResource("res/launch.gif")),
				new ImageIcon(getClass().getClassLoader().getResource("res/loading_ico.png")));

		informationDialog = new InformationDialog(frame, "Information", "res/info.html",
				new ImageIcon(getClass().getClassLoader().getResource("res/main_ico.png")));

		invalidExtensionDialog = new CustomJDialog(frame, "Invalid File Type", "File must be of extension *.nc", "Ok",
				dismissInvalidExtensionDialog, new ImageIcon(getClass().getClassLoader().getResource("res/error_dark.png")));

		parseErrorDialog = new CustomJDialog(frame, "Error", "Error loading file. See log for more details.", "Ok",
				dismissParseErrorDialog, new ImageIcon(getClass().getClassLoader().getResource("res/error_dark.png")));

		crashDialog = new CustomJDialog(frame, "Fatal Error", "GCI has crashed", "Exit",
				dismissCrashDialog, new ImageIcon(getClass().getClassLoader().getResource("res/error_dark.png")));

		menuBar = new MenuBar();
		Menu file = new Menu("File");
		MenuItem exitMenuItem = new MenuItem("Exit");
		exitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit(0);
			}
		});

		MenuItem openMenuItem = new MenuItem("Open");
		openMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				if (RenderUI.this.isPlaying)
					stopPlayback();

				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

				if (!currentFilePath.equals("")) {
					fileChooser.setCurrentDirectory(new File(currentFilePath).getParentFile());
					Logger.debug("Set file chooser to last directory location");
				}

				if (fileChooser.showOpenDialog(frame.getOwner()) == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					if (file != null) {
						checkAndLoadFile(file);
					}
				} else {
					Logger.debug("File open cancelled");
					return;
				}
			}
		});

		file.add(openMenuItem);
		file.addSeparator();
		file.add(exitMenuItem);

		Menu window = new Menu("Window");
		MenuItem resetLayoutItem = new MenuItem("Reset Window Layout");
		resetLayoutItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Logger.debug("Resetting window layout...");
				resetWindowLayout();
			}
		});

		MenuItem fullscreenItem = new MenuItem("Fullscreen Layout");
		fullscreenItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setFullscreenWindowLayout();
				Logger.debug("Set to fullscreen mode");
			}
		});

		window.add(fullscreenItem);
		window.add(resetLayoutItem);

		Menu about = new Menu("About");
		MenuItem sourceMenuItem = new MenuItem("Source");
		sourceMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Logger.debug("Opening source code url...");

				try {
					Desktop.getDesktop().browse(new URI(Shared.SOURCE_CODE_URL));
				} catch (IOException ex) {
					Logger.error(ex);
				} catch (URISyntaxException ex) {
					Logger.error(ex);
				}
			}
		});

		MenuItem infoMenuItem = new MenuItem("Information");
		infoMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				runOnNewThread(showInformationDialog);
			}
		});

		about.add(infoMenuItem);
		about.add(sourceMenuItem);

		menuBar.add(file);
		menuBar.add(window);
		menuBar.add(about);
		frame.setMenuBar(menuBar);

		detailsFrame = new JFrame("Details");
		detailsFrame.setLayout(new GridBagLayout());
		detailsFrame.setSize(400, 800);
		detailsFrame.setMinimumSize(new Dimension(200, 400));
		detailsFrame.setLocation((int) frame.getLocation().getX() + 800, (int) frame.getLocation().getY());
		detailsFrame.setResizable(true);
		detailsFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		detailsFrame.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("res/log_ico.png")).getImage());
		detailsFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowIconified(WindowEvent e) {
				frame.setState(JFrame.ICONIFIED);
				controlFrame.setState(JFrame.ICONIFIED);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				frame.setState(JFrame.NORMAL);
				controlFrame.setState(JFrame.NORMAL);
				frame.requestFocus();
			}
		});

		logTextArea = new JTextPane();
		logTextArea.setEditable(false);
		logTextArea.getStyledDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
		JPanel noWrapPanel = new JPanel(new BorderLayout());
		noWrapPanel.add(logTextArea);
		JScrollPane logScroller = new JScrollPane(noWrapPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		logScroller.setVerticalScrollBar(new WebScrollBar(WebScrollBar.VERTICAL));
		logScroller.setHorizontalScrollBar(new WebScrollBar(WebScrollBar.HORIZONTAL));
		logScroller.getVerticalScrollBar().setUnitIncrement(16);
		new SmartScroller(logScroller);

		GridBagConstraints g = new GridBagConstraints();
		g.gridx = g.gridy = 0;
		g.gridwidth = g.gridheight = 1;
		g.fill = GridBagConstraints.BOTH;
		g.anchor = GridBagConstraints.NORTHWEST;
		g.weightx = 100;
		g.weighty = 55;
		detailsFrame.add(logScroller, g);

		MenuBar logMenuBar = new MenuBar();
		Menu taskMenu = new Menu("Task");
		MenuItem clearMenuItem = new MenuItem("Clear Log");
		clearMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logTextArea.setText("");
			}
		});

		MenuItem saveMenuItem = new MenuItem("Save Log");
		saveMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String log = logTextArea.getDocument().getText(0, logTextArea.getDocument().getLength());

					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setDialogTitle("Save Log");
					fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					fileChooser.setApproveButtonText("Save");

					if (fileChooser.showOpenDialog(frame.getOwner()) == JFileChooser.APPROVE_OPTION) {
						String file = fileChooser.getSelectedFile().getAbsolutePath().toString();

						// If no extension is given append one
						if (!file.endsWith(".txt"))
							file += ".txt";

						Logger.debug("Save Directory: {}", file);

						try {
							File logFile = new File(file);
							BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
							writer.write(log);
							writer.close();
						} catch (IOException ex) {
							Logger.error("Log save failed: {}", ex);
						}

						Logger.info("Log saved to: {}", file);

					} else {
						Logger.debug("Log save cancelled");
						return;
					}
				} catch (BadLocationException e1) {
					Logger.error(e1);
				}
			}
		});

		taskMenu.add(clearMenuItem);
		taskMenu.add(saveMenuItem);
		logMenuBar.add(taskMenu);
		detailsFrame.setMenuBar(logMenuBar);

		JPopupMenu toolsPopupMenu = new JPopupMenu();
		JMenuItem copyMenuItem = new JMenuItem("Copy");
		copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() instanceof JTextPane)
					Logger.debug("found");

				String selectedText = logTextArea.getSelectedText();
				if (selectedText != null) {
					StringSelection text = new StringSelection(selectedText);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(text, text);
					Logger.debug("Copied to clipboard");
				}
			}
		});

		JMenuItem selectAllItem = new JMenuItem("Select All");
		selectAllItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logTextArea.selectAll();
				Logger.debug("Select all");
			}
		});

		toolsPopupMenu.add(copyMenuItem);
		toolsPopupMenu.add(selectAllItem);
		logTextArea.setComponentPopupMenu(toolsPopupMenu);

		JPanel detailsPanel = new JPanel(new GridLayout(4, 2));
		detailsPanel.setSize(500, 400);
		detailsPanel.setMaximumSize(new Dimension(500, 400));
		detailsPanel.setBackground(Shared.UI_COLOR);

		CustomJTextField currentCmdTextTitle = new CustomJTextField();
		CustomJTextField currentFeedRateTextTitle = new CustomJTextField();
		CustomJTextField currentSpindleSpeedTextTitle = new CustomJTextField();
		CustomJTextField boundsTextTitle = new CustomJTextField();
		CustomJTextField measurementModeTextTitle = new CustomJTextField();

		currentCmdText = new CustomJTextField();
		currentFeedRateText = new CustomJTextField();
		currentSpindleSpeedText = new CustomJTextField();
		measurementModeText = new CustomJTextField();

		boundsText = new JTextPane();
		boundsText.setBackground(Shared.UI_COLOR);
		boundsText.setEditable(false);
		boundsText.getStyledDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
		boundsText.setHighlighter(null);

		JPanel noWrapPanel2 = new JPanel(new BorderLayout());
		noWrapPanel2.add(boundsText);
		JScrollPane boundsTextScroller = new JScrollPane(noWrapPanel2, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		boundsTextScroller.setVerticalScrollBar(new WebScrollBar(WebScrollBar.VERTICAL));
		boundsTextScroller.setHorizontalScrollBar(new WebScrollBar(WebScrollBar.HORIZONTAL));

		currentCmdTextTitle.setText("Command");
		currentCmdText.setText("");
		currentFeedRateTextTitle.setText("Feed Rate");
		currentFeedRateText.setText("");
		currentSpindleSpeedTextTitle.setText("Spindle Speed");
		currentSpindleSpeedText.setText("");
		measurementModeTextTitle.setText("Measurement Mode");
		measurementModeText.setText("");
		boundsTextTitle.setText("Bounds");
		boundsText.setText("");

		detailsPanel.add(currentCmdTextTitle);
		detailsPanel.add(currentCmdText);
		detailsPanel.add(currentFeedRateTextTitle);
		detailsPanel.add(currentFeedRateText);
		detailsPanel.add(currentSpindleSpeedTextTitle);
		detailsPanel.add(currentSpindleSpeedText);
		detailsPanel.add(measurementModeTextTitle);
		detailsPanel.add(measurementModeText);

		JPanel boundsPanel = new JPanel(new GridLayout(1, 0));
		boundsPanel.add(boundsTextTitle);
		boundsPanel.add(boundsTextScroller);

		g.gridy = 1;
		g.weighty = 30;
		g.anchor = GridBagConstraints.NORTHWEST;
		detailsFrame.add(detailsPanel, g);

		g.gridy = 2;
		g.weighty = 15;
		g.weightx = 50;
		g.anchor = GridBagConstraints.NORTHWEST;
		detailsFrame.add(boundsPanel, g);

		controlFrame = new JFrame("Controls");
		controlFrame.setSize(400, 800);
		controlFrame.setMinimumSize(new Dimension(200, 400));
		controlFrame.setLocation((int) frame.getLocation().getX() - 400, (int) frame.getLocation().getY());
		controlFrame.setResizable(true);
		controlFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		controlFrame.setIconImage(
				new ImageIcon(getClass().getClassLoader().getResource("res/controls_ico.png")).getImage());
		controlFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowIconified(WindowEvent e) {
				frame.setState(JFrame.ICONIFIED);
				detailsFrame.setState(JFrame.ICONIFIED);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				frame.setState(JFrame.NORMAL);
				detailsFrame.setState(JFrame.NORMAL);
				frame.requestFocus();
			}
		});

		JPanel controlPanel = new JPanel(new GridLayout(10, 0));
		playButton = new CustomJButton("Play");
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!isPlaying) {
					if (currentTimePercent >= 1.0) {
						Logger.debug("Reset time slider to start");
						currentTimePercent = 0.0;
					}

					startPlayback();
				} else {
					stopPlayback();
				}
			}
		});

		timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 1000);
		timeSlider.setUI(new CustomSliderUI(timeSlider));
		timeSlider.setBackground(Shared.UI_COLOR);
		timeSlider.setPaintTicks(false);
		timeSlider.setBorder(new EmptyBorder(0, 20, 0, 20));
		timeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				currentTimePercent = (double) timeSlider.getValue() / (double) timeSlider.getMaximum();
				Logger.debug("Time slider value changed {}", currentTimePercent);
			}
		});

		JPanel timePanel = new JPanel(new BorderLayout());
		JLabel timeDes = new JLabel("", new ImageIcon(getClass().getClassLoader().getResource("res/time_ico.png")),
				JLabel.CENTER);

		timeDes.setBorder(new EmptyBorder(0, 20, 0, 0));
		timePanel.setBackground(Shared.UI_COLOR);
		timePanel.add(timeDes, BorderLayout.WEST);
		timePanel.add(timeSlider, BorderLayout.CENTER);

		scaleSlider = new JSlider(JSlider.HORIZONTAL, 1, 2000, 1000);
		scaleSlider.setEnabled(false);
		scaleSlider.setUI(new CustomSliderUI(scaleSlider));
		scaleSlider.setBackground(Shared.UI_COLOR);
		scaleSlider.setPaintTicks(false);
		scaleSlider.setBorder(new EmptyBorder(0, 20, 0, 20));
		scaleSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				Shared.SEGMENT_SCALE_MULTIPLIER = (scaleSlider.getValue() / 2000f) * 20;
				Logger.debug("Scale slider value changed {}, {}", scaleSlider.getValue(),
						Shared.SEGMENT_SCALE_MULTIPLIER);
			}
		});

		JPanel scalePanel = new JPanel(new BorderLayout());
		JLabel scaleDes = new JLabel("", new ImageIcon(getClass().getClassLoader().getResource("res/scale_ico.png")),
				JLabel.CENTER);

		scaleDes.setBorder(new EmptyBorder(0, 20, 0, 0));
		scalePanel.setBackground(Shared.UI_COLOR);
		scalePanel.add(scaleDes, BorderLayout.WEST);
		scalePanel.add(scaleSlider, BorderLayout.CENTER);

		generationMultiplierSlider = new JSlider(JSlider.HORIZONTAL, 75, 2000, 150);
		generationMultiplierSlider.setEnabled(false);
		generationMultiplierSlider.setUI(new CustomSliderUI(generationMultiplierSlider));
		generationMultiplierSlider.setBackground(Shared.UI_COLOR);
		generationMultiplierSlider.setPaintTicks(false);
		generationMultiplierSlider.setBorder(new EmptyBorder(0, 20, 0, 20));
		generationMultiplierSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				Shared.SEGMENT_GENERATION_MULTIPLIER = (generationMultiplierSlider.getValue() / 1000f) * 200;
				Logger.debug("Generation slider value changed {}, {}", generationMultiplierSlider.getValue(),
						Shared.SEGMENT_GENERATION_MULTIPLIER);
			}
		});

		JPanel genPanel = new JPanel(new BorderLayout());
		JLabel genDes = new JLabel("", new ImageIcon(getClass().getClassLoader().getResource("res/gen_ico.png")),
				JLabel.CENTER);

		genDes.setBorder(new EmptyBorder(0, 20, 0, 0));
		genPanel.setBackground(Shared.UI_COLOR);
		genPanel.add(genDes, BorderLayout.WEST);
		genPanel.add(generationMultiplierSlider, BorderLayout.CENTER);

		CustomJButton defButton = new CustomJButton("Default View");
		defButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Logger.debug("Setting camera to default top view");
				resetCamera();
			}
		});

		CustomJButton isoButton = new CustomJButton("Isometric View");
		isoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Logger.debug("Setting camera to isometric view");
				setCameraToIsometric();
			}
		});

		screenshotButton = new CustomJButton("Screenshot (Clipboard)");
		screenshotButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				takeScreenshot();
			}
		});

		toggleBoundingBoxButton = new CustomJButton("Display Bounding Box");
		toggleBoundingBoxButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleBoundingBox();
			}
		});

		CustomJButton reloadFileButton = new CustomJButton("Reload");
		reloadFileButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Logger.debug("Reloading file");
				checkAndLoadFile(new File(currentFilePath));
			}
		});

		CustomJButton clearButton = new CustomJButton("Clear");
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopPlayback();

				glLock.lock();
				animateLock.lock();

				vertexValues = new ArrayList<float[][]>();
				curLineColor = new ArrayList<Color>();
				spindleSpeedText = new ArrayList<String>();
				feedRateText = new ArrayList<String>();
				currentCommandText = new ArrayList<String>();
				boundsText.setText("");
				measurementModeText.setText("");

				totalTicks = 0;
				loopNum = 0;

				frame.setTitle("GCI");
				boundsExist = false;

				glLock.unlock();
				animateLock.unlock();

				Logger.debug("Cleared screen");
			}
		});

		controlPanel.add(playButton);
		controlPanel.add(timePanel);
		controlPanel.add(defButton);
		controlPanel.add(isoButton);
		controlPanel.add(toggleBoundingBoxButton);
		controlPanel.add(screenshotButton);
		controlPanel.add(reloadFileButton);
		controlPanel.add(clearButton);
		controlPanel.add(scalePanel);
		controlPanel.add(genPanel);
		controlFrame.add(controlPanel);

		this.performPlayback.start();
		this.animateVertexValues.start();

		resetCamera();
		status.countDown();

		detailsFrame.setVisible(true);
		controlFrame.setVisible(true);
		frame.setVisible(true);
		frame.requestFocus();
	}

	private void startPlayback() {
		if (timeSlider.getValue() == timeSlider.getMaximum())
			timeSlider.setValue(timeSlider.getMinimum());
		isPlaying = true;
		playButton.setText("Stop");
		timeSlider.setEnabled(false);
	}

	private void stopPlayback() {
		isPlaying = false;
		playButton.setText("Play");
		timeSlider.setEnabled(true);
	}

	private void unlockScaleAndGenSliders() {
		scaleSlider.setEnabled(true);
		generationMultiplierSlider.setEnabled(true);
		Logger.info("Unlocked sliders, reload after changing values");
	}

	private void switchScreenShotMode() {
		if (screenshotToClipboard) {
			screenshotButton.setText("Screenshot (File)");
			screenshotToClipboard = false;
		} else {
			screenshotButton.setText("Screenshot (Clipboard)");
			screenshotToClipboard = true;
		}
	}

	private void toggleBoundingBox() {
		if (displayBoundingBox) {
			toggleBoundingBoxButton.setText("Display Bounding Box");
			displayBoundingBox = false;
		} else {
			toggleBoundingBoxButton.setText("Hide Bounding Box");
			displayBoundingBox = true;
		}
	}

	@Override
	public void display(GLAutoDrawable glad) {
		GL2 gl = glad.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		setCamera(gl, glu);
		gl.glTranslatef(curX / 100.0f, curY / -100.0f, 0f);
		gl.glRotatef(curAngleY, 1, 0, 0);
		gl.glRotatef(curAngleX, 0, 1, 0);

		// Draw coordinate axis

		if (this.displayAxis) {
			RenderHelpers.renderLine(gl, Color.RED, new float[] { 0f, 0f, 0f }, new float[] { 40f, 0f, 0f }, 6f);
			RenderHelpers.renderLine(gl, Color.RED, new float[] { 0f, 0f, 0f }, new float[] { 0f, 40f, 0f }, 6f);
			RenderHelpers.renderLine(gl, Color.RED, new float[] { 0f, 0f, 0f }, new float[] { 0f, 0f, -40f }, 6f);
		}

		if (glLock.tryLock()) {
			
			try {
				int loop = this.loopNum;
				for (int i = 0; i < loop; i++) {
					RenderHelpers.renderLine(gl, this.curLineColor.get(i), this.vertexValues.get(i)[0],
							this.vertexValues.get(i)[1], 2.5f);
				}
			} catch (Exception e) {
				Logger.error("Fatal Error: {}", e);
				animator.stop();
				runOnNewThread(showCrashDialog);
				return;
			}
			
			if (this.boundsExist && this.displayBoundingBox) {
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { minX, minZ, -minY },
						new float[] { maxX, minZ, -minY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { maxX, minZ, -minY },
						new float[] { maxX, maxZ, -minY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { maxX, maxZ, -minY },
						new float[] { minX, maxZ, -minY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { minX, maxZ, -minY },
						new float[] { minX, minZ, -minY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { minX, minZ, -maxY },
						new float[] { maxX, minZ, -maxY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { maxX, minZ, -maxY },
						new float[] { maxX, maxZ, -maxY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { maxX, maxZ, -maxY },
						new float[] { minX, maxZ, -maxY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { minX, maxZ, -maxY },
						new float[] { minX, minZ, -maxY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { minX, minZ, -minY },
						new float[] { minX, minZ, -maxY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { maxX, minZ, -minY },
						new float[] { maxX, minZ, -maxY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { minX, maxZ, -minY },
						new float[] { minX, maxZ, -maxY }, 4.5f);
				RenderHelpers.renderLine(gl, Color.YELLOW, new float[] { maxX, maxZ, -minY },
						new float[] { maxX, maxZ, -maxY }, 4.5f);
			}

			glLock.unlock();
		}

		if (this.displayAxisLabels) {
			RenderHelpers.render3DText(textRenderer3D, "X", 41, 0, 0, 0.05f);
			RenderHelpers.render3DText(textRenderer3D, "Z", -1, 41, 0, 0.05f);
			RenderHelpers.render3DText(textRenderer3D, "Y", -1, 0, -41, 0.05f);
		}

		if (this.displayFileDropMessage) {
			RenderHelpers.renderText(textRenderer, "Drop file here.", 10, 10, this.glWidth, this.glHeight);
		}

		if (this.takeScreenshot) {
			Logger.info("Taking screenshot...");
			this.takeScreenshot = false;
			this.screenshotBuffer = ByteBuffer.allocate(this.glHeight * this.glWidth * 3);
			this.screenshotBuffer.clear();
			gl.glReadPixels(0, 0, this.glWidth, this.glHeight, GL2.GL_RGB, GL2.GL_BYTE, this.screenshotBuffer);
			new Screenshot(this.screenshotBuffer, this.glHeight, this.glWidth, this.screenshotToClipboard).start();
		}

		gl.glFlush();
	}

	@Override
	public void dispose(GLAutoDrawable glad) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(GLAutoDrawable glad) {
		Logger.debug("OpenGL init");

		GL2 gl = glad.getGL().getGL2();
		textRenderer = new TextRenderer(new Font("Roboto", Font.PLAIN, 20));
		textRenderer3D = new TextRenderer(new Font("Roboto", Font.PLAIN, 100));
		glu = new GLU();

		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glShadeModel(GL2.GL_SMOOTH);
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

		if (Shared.ENABLE_ANTIALIAS) {
			gl.glEnable(GL2.GL_LINE_SMOOTH);
			gl.glEnable(GL2.GL_BLEND);
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
			gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
		}

		gl.glClearColor(0f, 0f, 0f, 1f);
		gl.setSwapInterval((Shared.VSYNC) ? 1 : 0);

		setCamera(gl, glu);

		animator = new Animator(glcanvas);
		animator.start();
	}

	@Override
	public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
		this.glHeight = height;
		this.glWidth = width;

		GL2 gl = glad.getGL().getGL2();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glViewport(x, y, width, height);
		glu.gluPerspective(45, (double) width / (double) height, 1, 1000);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	private void setCamera(GL2 gl, GLU glu) {
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		float widthHeightRatio = (float) glcanvas.getWidth() / (float) glcanvas.getHeight();
		glu.gluPerspective(45, widthHeightRatio, 1, 1000);
		glu.gluLookAt(0, 0, zoomDistance, 0, 0, 0, 0, 1, 0);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	private void runOnNewThread(Runnable r) {
		new Thread() {
			@Override
			public void run() {
				r.run();
			}
		}.start();
	}

	private void checkAndLoadFile(File file) {
		String fileExtension = "";
		String filePath = file.getAbsolutePath();
		int extensionIndex = filePath.lastIndexOf(".");

		if (extensionIndex != -1)
			fileExtension = filePath.substring(extensionIndex);

		if (fileExtension.equals(".nc") || fileExtension.equals(".txt")) {
			RenderUI.this.currentFilePath = filePath;
			FileLoader fileLoader = new FileLoader(filePath);
			fileLoader.start();
		} else {
			Logger.warn("Invalid file type {}", fileExtension);
			runOnNewThread(showInvalidExtensionDialog);
		}
	}

	private void exit(int statusCode) {
		Logger.debug("Window closing...");

		if (animator != null)
			animator.stop();

		frame.remove(glcanvas);
		frame.dispose();
		detailsFrame.dispose();
		controlFrame.dispose();
		System.exit(statusCode);
	}

	private void resetCamera() {
		this.zoomDistance = 80f;
		this.curAngleX = 0f;
		this.curAngleY = 90f;
		this.curX = -1360f;
		this.curY = 1240f;
	}

	private void setCameraToIsometric() {
		this.zoomDistance = 80f;
		this.curAngleX = -10f;
		this.curAngleY = 40f;
		this.curX = -1720f;
		this.curY = 1810f;
	}

	private void resetWindowLayout() {
		frame.setSize(800, 800);
		frame.setLocationRelativeTo(null);
		detailsFrame.setSize(400, 800);
		detailsFrame.setLocation((int) frame.getLocation().getX() + 800, (int) frame.getLocation().getY());
		controlFrame.setSize(400, 800);
		controlFrame.setLocation((int) frame.getLocation().getX() - 400, (int) frame.getLocation().getY());
	}

	private void setFullscreenWindowLayout() {
		Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int width = (int) screenSize.getWidth();
		int height = (int) screenSize.getHeight();
		int qWidth = width / 4;

		controlFrame.setSize(qWidth, height);
		controlFrame.setLocation(0, 0);
		frame.setSize(qWidth * 2, height);
		frame.setLocation(qWidth, 0);
		detailsFrame.setSize(qWidth, height);
		detailsFrame.setLocation(qWidth * 3, 0);
	}

	private double getDecreaseSensitivityMultiplier() {
		return ((decreaseSensitivity) ? Shared.DECREASE_SENSITIVITY_MULTIPLIER : 1);
	}

	private void takeScreenshot() {
		this.takeScreenshot = true;
	}

	private void reloadDebug() {
		if (Shared.DEBUG_MODE) {
			logTextArea.setText("");
			Shared.SEGMENT_SCALE_MULTIPLIER = (int) (30 * (Math.random() + 0.4));
			checkAndLoadFile(new File(currentFilePath));
		}
	}
	
	private void crashDebug() {
		Logger.debug("Crashing gl event loop...");
		animateVertexValues.interrupt();
		loopNum = Integer.MAX_VALUE;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		if (e.getWheelRotation() == -1) {
			if (this.zoomDistance > 10f)
				this.zoomDistance -= Shared.ZOOM_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier();
			else
				this.zoomDistance = 10f;
		} else {
			if (this.zoomDistance < 300f)
				this.zoomDistance += Shared.ZOOM_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier();
			else
				this.zoomDistance = 300f;
		}

		Logger.debug("Zoom - {}", this.zoomDistance);
	}

	@Override
	public void mouseDragged(MouseEvent e) {

		float dx = (float) ((e.getX() - lastX) * Shared.PAN_SENSITIVITY_MULTIPLIER);
		float dy = (float) ((e.getY() - lastY) * Shared.PAN_SENSITIVITY_MULTIPLIER);

		if (SwingUtilities.isMiddleMouseButton(e)) {

			// Camera panning

			if (lockHorizAxis) {
				curX += (dx / getDecreaseSensitivityMultiplier());
			} else if (lockVertAxis) {
				curY += (dy / getDecreaseSensitivityMultiplier());
			} else {
				curX += (dx / getDecreaseSensitivityMultiplier());
				curY += (dy / getDecreaseSensitivityMultiplier());
			}

			Logger.debug("Drag - curX: {} curY: {}", curX, curY);

		} else if (SwingUtilities.isLeftMouseButton(e)) {

			// Rotate camera here

			float thetadx = (float) (Math.atan(dx / 100.0)
					* (Shared.ROTATE_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier()));
			float thetady = (float) (Math.atan(dy / 100.0)
					* (Shared.ROTATE_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier()));

			if (lockHorizAxis) {
				curAngleX = (curAngleX + thetadx) % 360f;
			} else if (lockVertAxis) {
				curAngleY = (curAngleY + thetady) % 360f;
			} else {
				curAngleX = (curAngleX + thetadx) % 360f;
				curAngleY = (curAngleY + thetady) % 360f;
			}

			Logger.debug("Rotate - curAngleX: {} curAngleY: {}", curAngleX, curAngleY);
		}

		lastX = e.getX();
		lastY = e.getY();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isMiddleMouseButton(e)) {
			Logger.debug("Drag started");
			frame.setCursor(this.panCursor);
		} else if (SwingUtilities.isLeftMouseButton(e)) {
			Logger.debug("Rotate started");
			frame.setCursor(this.rotateCursor);
		} else {
			return;
		}

		lastX = e.getX();
		lastY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isMiddleMouseButton(e)) {
			Logger.debug("Drag ended");
			frame.setCursor(Cursor.getDefaultCursor());
		} else if (SwingUtilities.isLeftMouseButton(e)) {
			Logger.debug("Rotate ended");
			frame.setCursor(Cursor.getDefaultCursor());
		} else {
			return;
		}

	}

	@Override
	public void keyPressed(KeyEvent e) {

		Logger.debug("Key pressed: {}", e.getKeyChar());

		if (!lockHorizAxis && e.getKeyCode() == KeyEvent.VK_Z) {
			lockHorizAxis = true;
		} else if (!lockVertAxis && e.getKeyCode() == KeyEvent.VK_X && !lockHorizAxis) {
			lockVertAxis = true;
		} else if (!decreaseSensitivity && e.getKeyCode() == KeyEvent.VK_C) {
			decreaseSensitivity = true;
		} else if (e.getKeyCode() == KeyEvent.VK_V) {
			resetCamera();
		} else if (e.getKeyCode() == KeyEvent.VK_B) {
			setCameraToIsometric();
		} else if (e.getKeyCode() == KeyEvent.VK_L) {
			unlockScaleAndGenSliders();
		} else if (e.getKeyCode() == KeyEvent.VK_O) {
			reloadDebug();
		} else if (e.getKeyCode() == KeyEvent.VK_Q) {
			toggleBoundingBox();
			Logger.debug("Bounding box state: {}", displayBoundingBox);
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			if (this.isPlaying)
				stopPlayback();
			else
				startPlayback();
		} else if (e.getKeyCode() == KeyEvent.VK_W) {
			this.displayAxis = !this.displayAxis;
		} else if (e.getKeyCode() == KeyEvent.VK_E) {
			this.displayAxisLabels = !this.displayAxisLabels;
		} else if (e.getKeyCode() == KeyEvent.VK_T) {
			switchScreenShotMode();
		} else if (e.getKeyCode() == KeyEvent.VK_Y && Shared.DEBUG_MODE) {
			crashDebug();
		} else {
			return;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (lockHorizAxis && e.getKeyCode() == KeyEvent.VK_Z) {
			lockHorizAxis = false;
		} else if (lockVertAxis && e.getKeyCode() == KeyEvent.VK_X) {
			lockVertAxis = false;
		} else if (decreaseSensitivity && e.getKeyCode() == KeyEvent.VK_C) {
			decreaseSensitivity = false;
		} else {
			return;
		}

		Logger.debug("Key released: {}", e.getKeyChar());
	}

	@Override
	public void appendToLog(String entry, Level level) {
		if (logTextArea != null) {
			if (level == Level.DEBUG || level == Level.INFO)
				appendToLogPane(entry, Color.BLACK, false);
			else if (level == Level.WARNING)
				appendToLogPane(entry, Color.ORANGE, false);
			else if (level == Level.ERROR)
				appendToLogPane(entry, Color.RED, true);
			else
				appendToLogPane(entry, Color.BLUE, true);
		}
	}

	private void appendToLogPane(String entry, Color color, boolean setBold) {
		StyleConstants.setForeground(this.logFormat, color);
		StyleConstants.setBold(this.logFormat, setBold);

		// DO NOT PUT LOG STATEMENTS IN THIS METHOD

		try {
			StyledDocument doc = logTextArea.getStyledDocument();
			doc.insertString(doc.getLength(), entry + "\n", this.logFormat);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void drop(DropTargetDropEvent e) {
		Logger.debug("File dropped");
		this.displayFileDropMessage = false;
		e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		Transferable t = e.getTransferable();

		try {
			@SuppressWarnings("unchecked")
			List<File> fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
			File file = (File) fileList.get(0);
			checkAndLoadFile(file);
		} catch (UnsupportedFlavorException e1) {
			Logger.error("Unsupported flavor: {}", e1);
		} catch (IOException e2) {
			Logger.error("IOException: {}", e2);
		}
	}

	@Override
	public void dragEnter(DropTargetDragEvent e) {
		Logger.debug("Drag entered");
		this.displayFileDropMessage = true;
	}

	@Override
	public void dragExit(DropTargetEvent e) {
		Logger.debug("Drag exited");
		this.displayFileDropMessage = false;
	}

	@Override
	public void dragOver(DropTargetDragEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dropActionChanged(DropTargetDragEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}
}
