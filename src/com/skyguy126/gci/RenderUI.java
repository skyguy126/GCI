package com.skyguy126.gci;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JPanel;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
import com.sun.prism.impl.BufferUtil;

// TODO
// Add console writer to log in JFrame
// Add method to reset camera position
// Add settings to change mouse sensitivity
// Switch to float values gl

public class RenderUI extends JFrame implements GLEventListener, MouseWheelListener, MouseMotionListener, MouseListener, KeyListener {

	private Frame frame;
	private MenuBar menuBar;

	private JFrame logFrame;
	
	
	private JFrame controlFrame;
	private JPanel controlPanel;
	private JButton playButton;
	private JButton pauseButton;

	private GLCapabilities glcaps;
	private GLCanvas glcanvas;
	private Animator animator;
	private GLU glu;

	private double lastX;
	private double lastY;

	private volatile double zoomDistance;
	private volatile double curX;
	private volatile double curY;
	private volatile double curAngleX;
	private volatile double curAngleY;

	private volatile boolean lockHorizAxis;
	private volatile boolean lockVertAxis;
	private volatile boolean decreaseSensitivity;
	private volatile boolean isValid;

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

		frame = new Frame("GCODE Interpreter - " + Shared.VERSION);
		frame.setSize(800, 800);
		frame.setResizable(false);
		frame.add(glcanvas);
		frame.setLocationRelativeTo(null);
		frame.addWindowListener(new WindowListener() {
			@Override
			public void windowClosing(WindowEvent evt) {
				exit();
			}

			@Override
			public void windowActivated(WindowEvent evt) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosed(WindowEvent evt) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeactivated(WindowEvent evt) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent evt) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowIconified(WindowEvent evt) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowOpened(WindowEvent evt) {
				// TODO Auto-generated method stub

			}
		});

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
				fileChooser.showOpenDialog(frame.getOwner());
				File file = fileChooser.getSelectedFile();
				if (file != null) {

					String fileExtension = "";
					String filePath = file.getAbsolutePath();
					int extensionIndex = filePath.lastIndexOf(".");

					if (extensionIndex != -1)
						fileExtension = filePath.substring(extensionIndex);

					if (fileExtension.equals(".nc") || fileExtension.equals(".txt")) {
						Logger.debug("Success");
						isValid = true;
						
					} else {
						Logger.warn("Invalid file type {}", fileExtension);
						JOptionPane.showMessageDialog(frame, "File must be of extenstion *.nc", "Invalid File Type",
								JOptionPane.WARNING_MESSAGE);
						isValid = false;
					}

				}
			}
		});
		

		file.add(openMenuItem);
		file.add(exitMenuItem);

		menuBar.add(file);
		frame.setMenuBar(menuBar);

		logFrame = new JFrame("Log");
		logFrame.setSize(400, 800);
		logFrame.setResizable(true);
		logFrame.setLocation((int) frame.getLocation().getX() + 800, (int) frame.getLocation().getY());
		logFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		logFrame.setVisible(true);
		
		controlFrame = new JFrame("Controls");
		controlFrame.setSize(400, 800);
		controlFrame.setResizable(true);
		controlFrame.setLocation((int) frame.getLocation().getX() - 400, (int) frame.getLocation().getY());
		controlFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		controlFrame.setVisible(true);
		JPanel controlPanel = new JPanel();
		controlFrame.add(controlPanel);
		JButton playButton = new JButton("Play");
		JButton pauseButton = new JButton("Pause");
		controlPanel.add(playButton);
		controlPanel.add(pauseButton);
		
		
		

		frame.setVisible(true);
		frame.requestFocus();
		controlPanel.setVisible(true);
		playButton.setVisible(true);
		pauseButton.setVisible(true);
	Logger.debug(controlFrame);
	}
	
	@Override
	public void display(GLAutoDrawable glad) {
		GL2 gl = glad.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		setCamera(gl, glu);

		gl.glTranslated(curX / 100.0, curY / -100.0, 0);
		gl.glRotated(curAngleY, 1, 0, 0);
		gl.glRotated(curAngleX, 0, 1, 0);

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 0.0f, 0.0f);
		gl.glVertex3f(10f, -10f, -10f);
		gl.glColor3f(0.0f, 1.0f, 0.0f);
		gl.glVertex3f(10f, 10f, -10f);
		gl.glColor3f(0.0f, 0.0f, 1.0f);
		gl.glVertex3f(-10f, 10f, -10f);
		gl.glColor3f(1.0f, 0.0f, 1.0f);
		gl.glVertex3f(-10f, -10f, -10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glVertex3f(10f, -10f, 10);
		gl.glVertex3f(10f, 10, 10f);
		gl.glVertex3f(-10f, 10f, 10f);
		gl.glVertex3f(-10f, -10f, 10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 0.0f, 1.0f);
		gl.glVertex3f(10f, -10f, -10f);
		gl.glVertex3f(10f, 10f, -10f);
		gl.glVertex3f(10f, 10f, 10f);
		gl.glVertex3f(10f, -10f, 10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(0.0f, 1.0f, 0.0f);
		gl.glVertex3f(-10f, -10f, 10f);
		gl.glVertex3f(-10f, 10f, 10f);
		gl.glVertex3f(-10f, 10f, -10f);
		gl.glVertex3f(-10f, -10f, -10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(0.0f, 0.0f, 1.0f);
		gl.glVertex3f(10f, 10f, 10f);
		gl.glVertex3f(10f, 10f, -10f);
		gl.glVertex3f(-10f, 10f, -10f);
		gl.glVertex3f(-10f, 10f, 10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 0.0f, 0.0f);
		gl.glVertex3f(10f, -10f, -10f);
		gl.glVertex3f(10f, -10f, 10f);
		gl.glVertex3f(-10f, -10f, 10f);
		gl.glVertex3f(-10f, -10f, -10f);
		gl.glEnd();

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
		glu = new GLU();

		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glShadeModel(GL2.GL_SMOOTH);
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
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

	public void captureCanvas() {
		
		// TODO NOT WORKING
		
		BufferedImage b = new BufferedImage(glcanvas.getWidth(), glcanvas.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = b.createGraphics();
		glcanvas.setupPrint(glcanvas.getWidth(), glcanvas.getWidth(), 50, 50, 50);
		glcanvas.print(g);

		ByteBuffer buffer = ByteBuffer.allocate(4 * glcanvas.getWidth() * glcanvas.getHeight());

		glcanvas.getGL().getGL2().glReadBuffer(GL2.GL_BACK);
		glcanvas.getGL().getGL2().glReadPixels(0, -799, 0, 799, GL2.GL_RGBA,
				GL2.GL_UNSIGNED_BYTE, buffer);

		
		for (int h = 0; h < glcanvas.getHeight(); h++) {
			for (int w = 0; w < glcanvas.getWidth(); w++) {
				if (buffer.get() != 0) {
					System.out.println("found");
				}
			}
		}

		try {

			ImageIO.write(b, "png", new File("test.png"));
		} catch (IOException ex) {
			// Error handling
		}
		glcanvas.releasePrint();
		g.dispose();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		if (e.getWheelRotation() == -1) {
			if (this.zoomDistance > 50)
				this.zoomDistance -= (Shared.ZOOM_SENSITIVITY_MULTIPLIER / getDecreaseSensitivityMultiplier());
			else
				this.zoomDistance = 50;
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

			double thetadx = Math.atan((double) dx / (double) zoomDistance);
			double thetady = Math.atan((double) dy / (double) zoomDistance);

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
		if (!lockHorizAxis && e.getKeyCode() == KeyEvent.VK_Z)
			lockHorizAxis = true;
		else if (!lockVertAxis && e.getKeyCode() == KeyEvent.VK_X)
			lockVertAxis = true;
		else if (!decreaseSensitivity && e.getKeyCode() == KeyEvent.VK_C)
			decreaseSensitivity = true;
		else if (e.getKeyCode() == KeyEvent.VK_V)
			resetCamera();
		else if (e.getKeyCode() == KeyEvent.VK_B)
			captureCanvas();
		else
			return;

		Logger.debug("Key pressed: {}", e.getKeyChar());
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (lockHorizAxis && e.getKeyCode() == KeyEvent.VK_Z)
			lockHorizAxis = false;
		else if (lockVertAxis && e.getKeyCode() == KeyEvent.VK_X)
			lockVertAxis = false;
		else if (decreaseSensitivity && e.getKeyCode() == KeyEvent.VK_C)
			decreaseSensitivity = false;
		else
			return;

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
		RenderUI gui = new RenderUI();
	}
}
