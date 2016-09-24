package com.skyguy126.gci;

import java.awt.Frame;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.io.File;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

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

public class RenderUI
		implements WindowListener, GLEventListener, MouseWheelListener, MouseMotionListener, MouseListener {

	private Frame frame;
	private MenuBar menuBar;

	private GLCapabilities glcaps;
	private GLCanvas glcanvas;
	private Animator animator;
	private GLU glu;

	private volatile int zoomDistance;
	private volatile int lastX;
	private volatile int lastY;
	private volatile int curX;
	private volatile int curY;

	public RenderUI() {
		Configurator.defaultConfig().formatPattern(Shared.LOG_FORMAT).level(Shared.LOG_LEVEL).activate();

		Logger.debug("RenderUI initiated");

		glcaps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		glcaps.setDoubleBuffered(true);
		glcaps.setHardwareAccelerated(true);

		glcanvas = new GLCanvas(glcaps);
		glcanvas.setSize(720, 720);
		glcanvas.addMouseListener(this);
		glcanvas.addGLEventListener(this);
		glcanvas.addMouseWheelListener(this);
		glcanvas.addMouseMotionListener(this);

		frame = new Frame("GCODE Interpreter - " + Shared.VERSION);
		frame.setSize(800, 800);
		frame.setResizable(false);
		frame.addWindowListener(this);
		frame.add(glcanvas);

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
					} else {
						Logger.warn("Invalid file type {}", fileExtension);
						JOptionPane.showMessageDialog(frame, "File must be of extenstion *.nc", "Invalid File Type",
								JOptionPane.WARNING_MESSAGE);
					}

				}
			}
		});

		file.add(openMenuItem);
		file.add(exitMenuItem);
		menuBar.add(file);

		this.zoomDistance = 100;

		frame.setMenuBar(menuBar);
		frame.setVisible(true);
	}

	@Override
	public void display(GLAutoDrawable glad) {
		GL2 gl = glad.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		setCamera(gl, glu, zoomDistance);
		gl.glTranslated(curX/100.0, curY/-100.0, 0);

		gl.glColor3f(0.9f, 0.5f, 0.2f);
		gl.glBegin(GL2.GL_TRIANGLE_FAN);

		gl.glVertex3f(-20, -20, 0);
		gl.glVertex3f(+20, -20, 0);
		gl.glVertex3f(0, 20, 0);
		gl.glEnd();
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
		gl.setSwapInterval(1);

		setCamera(gl, glu, zoomDistance);

		animator = new Animator(glcanvas);
		animator.start();
	}

	@Override
	public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
		GL2 gl = glad.getGL().getGL2();
		gl.glViewport(x, y, width, height);

		Logger.debug("GL reshape");
	}

	public void setCamera(GL2 gl, GLU glu, float distance) {
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		float widthHeightRatio = (float) glcanvas.getWidth() / (float) glcanvas.getHeight();
		glu.gluPerspective(45, widthHeightRatio, 1, 1000);
		glu.gluLookAt(0, 0, distance, 0, 0, 0, 0, 1, 0);

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

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		if (e.getWheelRotation() == -1) {
			if (this.zoomDistance > 50)
				this.zoomDistance -= 10;
			else
				this.zoomDistance = 50;
		} else {
			if (this.zoomDistance < 300)
				this.zoomDistance += 10;
			else
				this.zoomDistance = 300;
		}

		Logger.debug("Mouse wheel pos {}", this.zoomDistance);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isMiddleMouseButton(e)) {
			
			int dx = (e.getX() - lastX) * Shared.SENSITIVITY_MULTIPLIER;
			int dy = (e.getY() - lastY) * Shared.SENSITIVITY_MULTIPLIER;
			
			lastX = e.getX();
			lastY = e.getY();
			
			curX += dx;
			curY += dy;
			
			Logger.debug("Drag - dX: {} dY: {}", dx, dy);
		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isMiddleMouseButton(e)) {
			Logger.debug("Drag started");
			
			lastX = e.getX();
			lastY = e.getY();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isMiddleMouseButton(e)) {
			Logger.debug("Drag ended");
		}
	}
	
	@Override
	public void windowClosing(WindowEvent evt) {
		exit();
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

	public static void main(String[] args) {
		RenderUI gui = new RenderUI();
	}

}
