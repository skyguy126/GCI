package com.skyguy126.gci;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
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

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.awt.TextRenderer;

// TODO
// Add settings to change mouse sensitivity
// Switch to float values in gl loop
// Fix screenshot aspect ratio

public class RenderUI implements GLEventListener, MouseWheelListener, MouseMotionListener, MouseListener, KeyListener,
		LogWriter, DropTargetListener {

	private Frame frame;
	private MenuBar menuBar;

	private JFrame logFrame;
	private JFrame controlFrame;
	private JSlider timeSlider;
	private CustomJButton playButton;
	private JDialog loadingDialog;
	private JDialog parseErrorDialog;
	private JDialog informationDialog;
	private JTextPane logTextArea;

	private TextRenderer textRenderer;
	private TextRenderer textRenderer3D;
	private GLCapabilities glcaps;
	private GLCanvas glcanvas;
	private Animator animator;
	private GLU glu;

	private SimpleAttributeSet logFormat;

	private double lastX;
	private double lastY;

	private int glHeight;
	private int glWidth;

	private ReentrantLock lock;

	private volatile String axisLockText;
	private volatile String decreaseSensitivityText;
	private volatile String currentFilePath;

	private volatile double zoomDistance;
	private volatile double curX;
	private volatile double curY;
	private volatile double curAngleX;
	private volatile double curAngleY;
	private volatile double currentTimePercent;

	private volatile boolean lockHorizAxis;
	private volatile boolean lockVertAxis;
	private volatile boolean decreaseSensitivity;
	private volatile boolean takeScreenshot;
	private volatile boolean screenshotToClipboard;
	private volatile boolean isPlaying;
	private volatile boolean ready;
	private volatile boolean displayFileDropMessage;

	private volatile ByteBuffer screenshotBuffer;
	private volatile ArrayList<float[][]> vertexValuesGL;
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

	private Runnable showInformationDialog = new Runnable() {
		@Override
		public void run() {
			informationDialog.setVisible(true);
		}
	};

	private Runnable dismissInformationDialog = new Runnable() {
		@Override
		public void run() {
			informationDialog.setVisible(false);
		}
	};

	private Thread animateVertexValues = new Thread() {
		@Override
		public void run() {
			Logger.debug("Vertex animation thread started");

			while (true) {
				int loopNum = (int) (vertexValues.size() * currentTimePercent);
				ArrayList<float[][]> temp = new ArrayList<float[][]>();

				for (int i = 0; i < loopNum; i++) {
					if (ready)
						temp.add(vertexValues.get(i));
					else
						break;
				}

				if (ready)
					vertexValuesGL = temp;

				try {
					Thread.sleep(Shared.TIME_SCALE);
				} catch (InterruptedException e) {
					Logger.error(e);
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

				if ((int) currentTimePercent == 0 && currentTimeScale.size() > 0)
					timeScale = (long) currentTimeScale.get(0);
				else if (currentTimePercent > 0 && currentTimeScale.size() > 0)
					timeScale = (long) currentTimeScale.get((int) (currentTimePercent * currentTimeScale.size()) - 1);
				else
					timeScale = 1;

				try {
					Thread.sleep(Shared.TIME_SCALE / timeScale);
				} catch (InterruptedException e) {
					Logger.error(e);
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

			Logger.info("Attempting to load: {}", filePath);

			if (Shared.DEBUG_MODE) {
				try {
					Thread.sleep(750);
				} catch (InterruptedException e) {
					Logger.error(e);
				}
			}

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

					ready = false;

					vertexValues = interpreter.getVertexValues();
					curLineColor = interpreter.getCurrentLineColor();
					feedRateText = interpreter.getFeedRateText();
					currentCommandText = interpreter.getCurrentCommandText();
					spindleSpeedText = interpreter.getSpindleSpeedText();
					currentTimeScale = interpreter.getCurrentTimeScale();

					ready = true;

					Logger.info("Loaded file!");
					runOnNewThread(dismissLoadingDialog);
				}
			}

			runOnNewThread(dismissLoadingDialog);
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

	public RenderUI() {
		Configurator.defaultConfig().writer(new LogController(this)).formatPattern(Shared.LOG_FORMAT)
				.level(Shared.LOG_LEVEL).activate();

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
		this.screenshotToClipboard = false;
		this.isPlaying = false;
		this.ready = false;
		this.displayFileDropMessage = false;

		this.axisLockText = "Axis Lock: NA";
		this.decreaseSensitivityText = "Dec Sensitivity: false";
		this.currentFilePath = "";

		this.vertexValues = new ArrayList<float[][]>();
		this.vertexValuesGL = new ArrayList<float[][]>();
		this.curLineColor = new ArrayList<Color>();
		this.spindleSpeedText = new ArrayList<String>();
		this.feedRateText = new ArrayList<String>();
		this.currentCommandText = new ArrayList<String>();
		this.currentTimeScale = new ArrayList<Integer>();

		this.currentTimePercent = 1.0;
		this.logFormat = new SimpleAttributeSet();
		this.lock = new ReentrantLock();

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
		this.screenshotBuffer = ByteBuffer.allocate(this.glHeight * this.glWidth * 3);

		frame = new Frame("GCI - " + Shared.VERSION);
		frame.setSize(800, 800);
		frame.setResizable(false);
		frame.add(glcanvas);
		frame.setLocationRelativeTo(null);
		frame.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("res/main_ico.png")).getImage());
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				exit();
			}

			@Override
			public void windowActivated(WindowEvent e) {
				Logger.debug("Main frame gained focus");

				controlFrame.setFocusableWindowState(false);
				logFrame.setFocusableWindowState(false);
				controlFrame.toFront();
				logFrame.toFront();
				frame.requestFocus();
				controlFrame.setFocusableWindowState(true);
				logFrame.setFocusableWindowState(true);
			}

			@Override
			public void windowIconified(WindowEvent e) {
				logFrame.setState(JFrame.ICONIFIED);
				controlFrame.setState(JFrame.ICONIFIED);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				logFrame.setState(JFrame.NORMAL);
				controlFrame.setState(JFrame.NORMAL);
				frame.requestFocus();
			}
		});

		loadingDialog = new JDialog(frame, "Please Wait...", true);
		loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		loadingDialog.add(new JLabel("Loading...",
				new ImageIcon(getClass().getClassLoader().getResource("res/loader.gif")), JLabel.CENTER));
		loadingDialog.setSize(new Dimension(400, 200));
		loadingDialog
				.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("res/loading_ico.png")).getImage());
		loadingDialog.setLocationRelativeTo(frame);

		informationDialog = new JDialog(frame, "Information", true);
		informationDialog.setSize(600, 600);
		informationDialog.setResizable(false);
		informationDialog.setLocationRelativeTo(frame);
		informationDialog
				.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("res/main_ico.png")).getImage());
		informationDialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				runOnNewThread(dismissInformationDialog);
			}
		});

		try {
			BufferedReader infoHtmlReader = new BufferedReader(
					new InputStreamReader(getClass().getClassLoader().getResourceAsStream("res/info.html"), "UTF-8"));

			infoHtmlReader.mark(1);
			if (infoHtmlReader.read() != 0xFEFF)
				infoHtmlReader.reset();
			else
				Logger.debug("Removed byte order mark from filestream");

			StringBuffer infoHtml = new StringBuffer();
			String currentLine;

			while ((currentLine = infoHtmlReader.readLine()) != null) {
				infoHtml.append(currentLine);
			}

			infoHtmlReader.close();

			JEditorPane infoPane = new JEditorPane("text/html", infoHtml.toString());
			infoPane.setSize(600, 600);
			BufferedImage infoImg = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
					.getDefaultConfiguration().createCompatibleImage(600, 600);
			infoPane.print(infoImg.getGraphics());

			@SuppressWarnings("serial")
			JPanel infoJPanel = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					g.drawImage(infoImg, 0, 0, null);
				}
			};

			informationDialog.add(infoJPanel);

			Logger.debug("Loaded information html");
		} catch (Exception e) {
			Logger.error("Error loading information html: {}", e);
			System.exit(-1);
		}

		parseErrorDialog = new JDialog(frame, "Error", true);
		JPanel parseErrorDialogMessagePane = new JPanel();
		JPanel parseErrorDialogButtonPane = new JPanel();

		parseErrorDialogMessagePane.add(new JLabel("Error loading file. See log for more details.",
				new ImageIcon(getClass().getClassLoader().getResource("res/error.png")), JLabel.CENTER));

		JButton parseErrorDialogButton = new JButton("Ok");
		parseErrorDialogButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				parseErrorDialog.setVisible(false);
			}
		});
		parseErrorDialogButtonPane.add(parseErrorDialogButton);

		parseErrorDialog.add(parseErrorDialogMessagePane);
		parseErrorDialog.add(parseErrorDialogButtonPane, BorderLayout.SOUTH);
		parseErrorDialog.setSize(new Dimension(400, 200));
		parseErrorDialog
				.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("res/error.png")).getImage());
		parseErrorDialog.setLocationRelativeTo(frame);
		parseErrorDialog.pack();

		menuBar = new MenuBar();
		Menu file = new Menu("File");
		MenuItem exitMenuItem = new MenuItem("Exit");
		exitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit();
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

				if (!RenderUI.this.currentFilePath.equals("")) {
					fileChooser.setCurrentDirectory(new File(RenderUI.this.currentFilePath).getParentFile());
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
		file.add(exitMenuItem);

		Menu about = new Menu("About");
		MenuItem sourceMenuItem = new MenuItem("Source Code");
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
		about.add(sourceMenuItem);

		MenuItem infoMenuItem = new MenuItem("Information");
		infoMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				runOnNewThread(showInformationDialog);
			}
		});
		about.add(infoMenuItem);

		menuBar.add(file);
		menuBar.add(about);
		frame.setMenuBar(menuBar);

		logFrame = new JFrame("Log");
		logFrame.setSize(400, 800);
		logFrame.setResizable(false);
		logFrame.setLocation((int) frame.getLocation().getX() + 800, (int) frame.getLocation().getY());
		logFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		logFrame.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("res/log_ico.png")).getImage());
		logFrame.addWindowListener(new WindowAdapter() {
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

		// TODO create a log buffer so memory doesn't run out

		logTextArea = new JTextPane();
		logTextArea.setEditable(false);
		logTextArea.getStyledDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
		JPanel noWrapPanel = new JPanel(new BorderLayout());
		noWrapPanel.add(logTextArea);
		JScrollPane logScroller = new JScrollPane(noWrapPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		logScroller.getVerticalScrollBar().setUnitIncrement(16);
		new SmartScroller(logScroller);
		logFrame.add(logScroller);

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
		logFrame.setMenuBar(logMenuBar);

		controlFrame = new JFrame("Controls");
		controlFrame.setSize(400, 800);
		controlFrame.setResizable(false);
		controlFrame.setLocation((int) frame.getLocation().getX() - 400, (int) frame.getLocation().getY());
		controlFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		controlFrame.setIconImage(
				new ImageIcon(getClass().getClassLoader().getResource("res/controls_ico.png")).getImage());
		controlFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowIconified(WindowEvent e) {
				frame.setState(JFrame.ICONIFIED);
				logFrame.setState(JFrame.ICONIFIED);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				frame.setState(JFrame.NORMAL);
				logFrame.setState(JFrame.NORMAL);
				frame.requestFocus();
			}
		});

		JPanel controlPanel = new JPanel(new GridLayout(7, 0));
		playButton = new CustomJButton("Play");
		playButton.setFont(Shared.BUTTON_FONT);
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

		timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);

		// TODO new ui
		// timeSlider.setUI(new CustomSliderUI(timeSlider));

		timeSlider.setBackground(Shared.UI_COLOR);
		timeSlider.setMajorTickSpacing(100);
		timeSlider.setMinorTickSpacing(10);
		timeSlider.setPaintTicks(true);
		timeSlider.setBorder(new EmptyBorder(0, 10, 0, 10));
		timeSlider.setValue(timeSlider.getMaximum() - 1);
		timeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				currentTimePercent = (double) timeSlider.getValue() / (double) timeSlider.getMaximum();
				Logger.debug("Time slider value changed {}", currentTimePercent);
			}
		});

		CustomJButton isoButton = new CustomJButton("Isometric View");
		isoButton.setFont(Shared.BUTTON_FONT);
		isoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Logger.debug("Setting camera to isometric view");
				setCameraToIsometric();
			}
		});

		CustomJButton screenshotButton = new CustomJButton("Screenshot (File)");
		screenshotButton.setFont(Shared.BUTTON_FONT);
		screenshotButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				takeScreenshot();
			}
		});

		CustomJButton switchScreenshotModeButton = new CustomJButton("Switch Screenshot Mode");
		switchScreenshotModeButton.setFont(Shared.BUTTON_FONT);
		switchScreenshotModeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (RenderUI.this.screenshotToClipboard) {
					screenshotButton.setText("Screenshot (File)");
					RenderUI.this.screenshotToClipboard = false;
				} else {
					screenshotButton.setText("Screenshot (Clipboard)");
					RenderUI.this.screenshotToClipboard = true;
				}
			}
		});

		CustomJButton reloadFileButton = new CustomJButton("Reload File");
		reloadFileButton.setFont(Shared.BUTTON_FONT);
		reloadFileButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Logger.debug("Reloading file");
				checkAndLoadFile(new File(RenderUI.this.currentFilePath));
			}
		});

		CustomJButton clearButton = new CustomJButton("Clear");
		clearButton.setFont(Shared.BUTTON_FONT);
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ready = false;
				vertexValues = new ArrayList<float[][]>();
				vertexValuesGL = new ArrayList<float[][]>();
				curLineColor = new ArrayList<Color>();
				spindleSpeedText = new ArrayList<String>();
				feedRateText = new ArrayList<String>();
				currentCommandText = new ArrayList<String>();
				ready = true;
				Logger.debug("Clearing screen");
			}
		});

		controlPanel.add(playButton);
		controlPanel.add(timeSlider);
		controlPanel.add(isoButton);
		controlPanel.add(screenshotButton);
		controlPanel.add(switchScreenshotModeButton);
		controlPanel.add(reloadFileButton);
		controlPanel.add(clearButton);
		controlFrame.add(controlPanel);

		this.performPlayback.start();
		this.animateVertexValues.start();

		resetCamera();

		logFrame.setVisible(true);
		controlFrame.setVisible(true);
		frame.setVisible(true);
		frame.requestFocus();
	}

	private void startPlayback() {
		isPlaying = true;
		playButton.setText("Stop");
		timeSlider.setEnabled(false);
	}

	private void stopPlayback() {
		isPlaying = false;
		playButton.setText("Play");
		timeSlider.setEnabled(true);
	}

	@Override
	public void display(GLAutoDrawable glad) {
		GL2 gl = glad.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		setCamera(gl, glu);
		gl.glTranslated(curX / 100.0, curY / -100.0, 0);
		gl.glRotated(curAngleY, 1, 0, 0);
		gl.glRotated(curAngleX, 0, 1, 0);

		// Draw coordinate axis
		RenderHelpers.renderLine(gl, Color.RED, new float[] { 0f, 0f, 0f }, new float[] { 40f, 0f, 0f }, 5f);
		RenderHelpers.renderLine(gl, Color.RED, new float[] { 0f, 0f, 0f }, new float[] { 0f, 40f, 0f }, 5f);
		RenderHelpers.renderLine(gl, Color.RED, new float[] { 0f, 0f, 0f }, new float[] { 0f, 0f, -40f }, 5f);

		RenderHelpers.render3DText(textRenderer3D, "X", 41, 0, 0, 0.05f);
		RenderHelpers.render3DText(textRenderer3D, "Z", -1, 41, 0, 0.05f);
		RenderHelpers.render3DText(textRenderer3D, "Y", -1, 0, -41, 0.05f);

		// TODO change gl line method to connected line not individual segments
		// TODO Critical thread sync issues

		if (lock.tryLock()) {
			for (int i = 0; i < this.vertexValuesGL.size(); i++) {
				RenderHelpers.renderLine(gl, this.curLineColor.get(i), this.vertexValuesGL.get(i)[0],
						this.vertexValuesGL.get(i)[1], 2.5f);
			}

			lock.unlock();
		}

		if (this.displayFileDropMessage) {
			RenderHelpers.renderText(textRenderer, "Drop file here.", 10, 10, this.glWidth, this.glHeight);
		}

		if (this.takeScreenshot) {
			Logger.info("Taking screenshot...");
			this.takeScreenshot = false;
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
		GL2 gl = glad.getGL().getGL2();
		gl.glViewport(x, y, width, height);
		Logger.debug("GL reshape");
	}

	public void setCamera(GL2 gl, GLU glu) {
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		float widthHeightRatio = (float) glcanvas.getWidth() / (float) glcanvas.getHeight();
		glu.gluPerspective(45, widthHeightRatio, 1, 1000);
		glu.gluLookAt(0, 0, zoomDistance, 0, 0, 0, 0, 1, 0);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	public void runOnNewThread(Runnable r) {
		(new Thread() {
			@Override
			public void run() {
				r.run();
			}
		}).start();
	}

	public void checkAndLoadFile(File file) {
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
			JOptionPane.showMessageDialog(this.frame, "File must be of extenstion *.nc", "Invalid File Type",
					JOptionPane.WARNING_MESSAGE);
		}
	}

	public void exit() {
		Logger.debug("Window closing...");

		if (animator != null)
			animator.stop();

		frame.remove(glcanvas);
		frame.dispose();
		System.exit(0);
	}

	public void resetCamera() {
		this.zoomDistance = 80;
		this.curAngleX = 0;
		this.curAngleY = 90;
		this.curX = -1500;
		this.curY = 1000;
	}

	public void setCameraToIsometric() {
		this.zoomDistance = 90;
		this.curAngleX = -10f;
		this.curAngleY = 40f;
		this.curY = 1500;
		this.curX = -1500;
	}

	public double getDecreaseSensitivityMultiplier() {
		return ((decreaseSensitivity) ? Shared.DECREASE_SENSITIVITY_MULTIPLIER : 1);
	}

	public void takeScreenshot() {
		this.takeScreenshot = true;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		if (e.getWheelRotation() == -1) {
			if (this.zoomDistance > 10)
				this.zoomDistance -= (Shared.ZOOM_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier());
			else
				this.zoomDistance = 10;
		} else {
			if (this.zoomDistance < 300)
				this.zoomDistance += (Shared.ZOOM_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier());
			else
				this.zoomDistance = 300;
		}

		Logger.debug("Zoom - {}", this.zoomDistance);
	}

	@Override
	public void mouseDragged(MouseEvent e) {

		double dx = (e.getX() - lastX) * Shared.PAN_SENSITIVITY_MULTIPLIER;
		double dy = (e.getY() - lastY) * Shared.PAN_SENSITIVITY_MULTIPLIER;

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

			Logger.debug("Drag - dx: {} dy: {}", dx, dy);

		} else if (SwingUtilities.isLeftMouseButton(e)) {

			// Rotate camera here

			double thetadx = Math.atan((double) dx / 100.0)
					* (Shared.ROTATE_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier());
			double thetady = Math.atan((double) dy / 100.0)
					* (Shared.ROTATE_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier());

			// TODO reset angle if over 2pi

			if (lockHorizAxis) {
				curAngleX += thetadx;
			} else if (lockVertAxis) {
				curAngleY += thetady;
			} else {
				curAngleX += thetadx;
				curAngleY += thetady;
			}

			Logger.debug("Rotate - curAngleX: {} curAngleY: {}", curAngleX, curAngleY);
		}

		lastX = e.getX();
		lastY = e.getY();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isMiddleMouseButton(e))
			Logger.debug("Drag started");
		else if (SwingUtilities.isLeftMouseButton(e))
			Logger.debug("Rotate started");
		else
			return;

		lastX = e.getX();
		lastY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isMiddleMouseButton(e))
			Logger.debug("Drag ended");
		else if (SwingUtilities.isLeftMouseButton(e))
			Logger.debug("Rotate ended");
		else
			return;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (!lockHorizAxis && e.getKeyCode() == KeyEvent.VK_Z) {
			lockHorizAxis = true;
			this.axisLockText = "Axis Lock: X";
		} else if (!lockVertAxis && e.getKeyCode() == KeyEvent.VK_X && !lockHorizAxis) {
			lockVertAxis = true;
			this.axisLockText = "Axis Lock: Y";
		} else if (!decreaseSensitivity && e.getKeyCode() == KeyEvent.VK_C) {
			decreaseSensitivity = true;
			this.decreaseSensitivityText = "Dec Sensitivity: true";
		} else if (e.getKeyCode() == KeyEvent.VK_V) {
			resetCamera();
		} else if (e.getKeyCode() == KeyEvent.VK_B) {
			setCameraToIsometric();
		} else if (e.getKeyCode() == KeyEvent.VK_N) {
			this.screenshotToClipboard = !this.screenshotToClipboard;
			Logger.debug("Screenshot to clipboard: {}", this.screenshotToClipboard);
		} else if (e.getKeyCode() == KeyEvent.VK_M) {
			takeScreenshot();
		} else {
			return;
		}

		Logger.debug("Key pressed: {}", e.getKeyChar());
	}

	@Override
	public void keyReleased(KeyEvent e) {

		// TODO axis lock text resets if both buttons are pressed and released

		if (lockHorizAxis && e.getKeyCode() == KeyEvent.VK_Z) {
			lockHorizAxis = false;
			this.axisLockText = "Axis Lock: N/A";
		} else if (lockVertAxis && e.getKeyCode() == KeyEvent.VK_X) {
			lockVertAxis = false;
			this.axisLockText = "Axis Lock: N/A";
		} else if (decreaseSensitivity && e.getKeyCode() == KeyEvent.VK_C) {
			decreaseSensitivity = false;
			this.decreaseSensitivityText = "Dec Sensitivity: false";
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
	public void dragEnter(DropTargetDragEvent arg0) {
		Logger.debug("Drag entered");
		this.displayFileDropMessage = true;
	}

	@Override
	public void dragExit(DropTargetEvent arg0) {
		Logger.debug("Drag exited");
		this.displayFileDropMessage = false;
	}

	@Override
	public void dragOver(DropTargetDragEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dropActionChanged(DropTargetDragEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
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

	public static void main(String[] args) {
		new RenderUI();
	}
}
