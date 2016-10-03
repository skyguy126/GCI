package com.skyguy126.gci;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
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
// Add console writer to log in JFrame
// Add settings to change mouse sensitivity
// Switch to float values in gl loop
// Change zoom method to use FOV not glTranslate
// Fix screenshot aspect ratio
// Add anti-aliasing

public class RenderUI implements GLEventListener, MouseWheelListener, MouseMotionListener, MouseListener, KeyListener {

	private Frame frame;
	private MenuBar menuBar;

	private JFrame logFrame;
	private JFrame controlFrame;
	private JSlider timeSlider;
	private JButton playButton;
	private JDialog loadingDialog;

	private TextRenderer textRenderer;
	private GLCapabilities glcaps;
	private GLCanvas glcanvas;
	private Animator animator;
	private GLU glu;

	private Color lineColor;

	private double lastX;
	private double lastY;

	private int glHeight;
	private int glWidth;

	private volatile String axisLockText;
	private volatile String decreaseSensitivityText;

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
	private volatile boolean isPlaying;

	private volatile ByteBuffer screenshotBuffer;
	private volatile ArrayList<float[][]> vertexValuesGL;
	private volatile ArrayList<float[][]> vertexValues;
	
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

	private Thread animateVertexValues = new Thread() {
		@Override
		public void run() {
			Logger.debug("Vertex animation thread started");

			while (true) {
				int loopNum = (int) (vertexValues.size() * currentTimePercent);
				ArrayList<float[][]> temp = new ArrayList<float[][]>();

				for (int i = 0; i < loopNum; i++) {
					temp.add(vertexValues.get(i));
				}

				vertexValuesGL = temp;

				try {
					Thread.sleep(17);
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

			while (true) {

				if (isPlaying) {
					Logger.debug("Playing: {}", currentTimePercent);

					if (currentTimePercent < 1.0) {
						currentTimePercent += 0.001;
						timeSlider.setValue((int) (currentTimePercent * timeSlider.getMaximum()));
					} else {
						isPlaying = false;
						playButton.setText("Play");
						timeSlider.setEnabled(true);
					}
				}

				try {
					Thread.sleep(17);
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
			
			Parser parser = new Parser(filePath);
			boolean parseSuccess = parser.parse();
			
			if (parseSuccess) {
				Interpreter interpreter = new Interpreter(parser.getGCodeArray());
				boolean interpSuccess = interpreter.generateAbsolute();
				
				if (interpSuccess) {
					vertexValues = interpreter.getVertexValues();
					Logger.info("Loaded file!");
					runOnNewThread(dismissLoadingDialog);
				}
			}
		}
	}

	private class Screenshot extends Thread {

		private ByteBuffer buffer;
		private int height;
		private int width;

		public Screenshot(ByteBuffer b, int h, int w) {
			this.buffer = b;
			this.height = h;
			this.width = w;
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

				Logger.info("Screenshot saved");

			} else {
				Logger.debug("Screenshot save cancelled");
				return;
			}

		}
	}

	public RenderUI() {
		Configurator.defaultConfig().formatPattern(Shared.LOG_FORMAT).level(Shared.LOG_LEVEL).activate();

		Logger.debug("RenderUI initiated");

		assert Shared.PAN_SENSITIVITY_MULTIPLIER > 0 : "Pan sensitivity must be greater than 0";
		assert Shared.ROTATE_SENSITIVITY_MULTIPLIER > 0 : "Rotate sensitivity must be greater than 0";
		assert Shared.ZOOM_SENSITIVITY_MULTIPLIER > 0 : "Zoom sensitivity must be greater than 0";

		this.zoomDistance = 100;
		this.curX = 0;
		this.curY = 0;
		this.curAngleX = 0;
		this.curAngleY = 0;

		this.lockHorizAxis = false;
		this.lockVertAxis = false;
		this.decreaseSensitivity = false;
		this.takeScreenshot = false;
		this.isPlaying = false;

		this.axisLockText = "Axis Lock: NA";
		this.decreaseSensitivityText = "Dec Sensitivity: false";

		this.vertexValues = new ArrayList<float[][]>();
		this.vertexValuesGL = new ArrayList<float[][]>();
		this.lineColor = new Color(0f, 0.4f, 1.0f);

		this.currentTimePercent = 1.0;

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

		this.glHeight = glcanvas.getHeight();
		this.glWidth = glcanvas.getWidth();
		this.screenshotBuffer = ByteBuffer.allocate(this.glHeight * this.glWidth * 3);

		frame = new Frame("GCODE Interpreter - " + Shared.VERSION);
		frame.setSize(800, 800);
		frame.setResizable(false);
		frame.add(glcanvas);
		frame.setLocationRelativeTo(null);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				exit();
			}
		});
		
		loadingDialog = new JDialog(frame, "Please Wait...", true);
		loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		loadingDialog.add(new JLabel("Loading...", new ImageIcon("res/ajax-loader.gif"), JLabel.CENTER));
		loadingDialog.setSize(new Dimension(400, 200));
		loadingDialog.setLocationRelativeTo(null);

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

				JFileChooser fileChooser = new JFileChooser();

				if (fileChooser.showOpenDialog(frame.getOwner()) == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					if (file != null) {

						String fileExtension = "";
						String filePath = file.getAbsolutePath();
						int extensionIndex = filePath.lastIndexOf(".");

						if (extensionIndex != -1)
							fileExtension = filePath.substring(extensionIndex);

						if (fileExtension.equals(".nc") || fileExtension.equals(".txt")) {
							FileLoader fileLoader = new FileLoader(filePath);
							fileLoader.start();
						} else {
							Logger.warn("Invalid file type {}", fileExtension);
							JOptionPane.showMessageDialog(frame, "File must be of extenstion *.nc", "Invalid File Type",
									JOptionPane.WARNING_MESSAGE);
						}

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
		about.add(infoMenuItem);
		
		menuBar.add(file);
		menuBar.add(about);
		frame.setMenuBar(menuBar);

		logFrame = new JFrame("Log");
		logFrame.setSize(400, 800);
		logFrame.setResizable(false);
		logFrame.setLocation((int) frame.getLocation().getX() + 800, (int) frame.getLocation().getY());
		logFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		controlFrame = new JFrame("Controls");
		controlFrame.setSize(400, 800);
		controlFrame.setResizable(false);
		controlFrame.setLocation((int) frame.getLocation().getX() - 400, (int) frame.getLocation().getY());
		controlFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		JPanel controlPanel = new JPanel(new GridLayout(6, 0));
		playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!isPlaying) {
					if (currentTimePercent >= 1.0) {
						currentTimePercent = 0.0;
					}
					isPlaying = true;
					playButton.setText("Stop");
					timeSlider.setEnabled(false);
				} else {
					isPlaying = false;
					playButton.setText("Play");
					timeSlider.setEnabled(true);
				}
			}
		});
		controlPanel.add(playButton);

		timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
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
		controlPanel.add(timeSlider);

		JButton screenshotButton = new JButton("Screenshot");
		screenshotButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				takeScreenshot();
			}
		});
		controlPanel.add(screenshotButton);

		this.performPlayback.start();
		this.animateVertexValues.start();

		controlFrame.add(controlPanel);
		logFrame.setVisible(true);
		controlFrame.setVisible(true);
		frame.setVisible(true);
		frame.requestFocus();
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

		for (float[][] vertex : this.vertexValuesGL) {
			RenderHelpers.renderLine(gl, lineColor, vertex[0], vertex[1], 2.5f);
		}

		RenderHelpers.renderText(this.textRenderer, this.axisLockText, 5, 5, this.glWidth, this.glHeight);
		RenderHelpers.renderText(this.textRenderer, this.decreaseSensitivityText, 5, 35, this.glWidth, this.glHeight);

		if (this.takeScreenshot) {
			Logger.info("Taking screenshot...");
			this.takeScreenshot = false;
			this.screenshotBuffer.clear();
			gl.glReadPixels(0, 0, this.glWidth, this.glHeight, GL2.GL_RGB, GL2.GL_BYTE, this.screenshotBuffer);
			Screenshot s = new Screenshot(this.screenshotBuffer, this.glHeight, this.glWidth);
			s.start();
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
		textRenderer = new TextRenderer(new Font("Roboto", Font.PLAIN, 30));
		glu = new GLU();

		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glShadeModel(GL2.GL_SMOOTH);
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
		gl.glEnable(GL2.GL_LINE_SMOOTH);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glHint (GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
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

	public void exit() {
		Logger.debug("Window closing...");

		if (animator != null)
			animator.stop();

		frame.remove(glcanvas);
		frame.dispose();
		System.exit(0);
	}

	public void resetCamera() {
		this.zoomDistance = 100;
		this.curAngleX = 0;
		this.curAngleY = 0;
		this.curX = 0;
		this.curY = 0;
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

			// TODO use robot to move mouse back to original position

			Logger.debug("Drag - dx: {} dy: {}", dx, dy);

		} else if (SwingUtilities.isLeftMouseButton(e)) {

			// Rotate camera here

			double thetadx = Math.atan((double) dx / 100.0);
			double thetady = Math.atan((double) dy / 100.0);

			// TODO reset angle if over 2pi

			if (lockHorizAxis) {
				curAngleX += (thetadx) * (Shared.ROTATE_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier());
			} else if (lockVertAxis) {
				curAngleY += (thetady) * (Shared.ROTATE_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier());
			} else {
				curAngleX += (thetadx) * (Shared.ROTATE_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier());

				curAngleY += (thetady) * (Shared.ROTATE_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier());
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
