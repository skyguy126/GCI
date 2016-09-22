package com.skyguy126.gci;

import java.awt.Frame;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.io.File;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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


public class RenderUI implements WindowListener, GLEventListener {
	
	private Frame frame;
	private MenuBar menuBar;
	
	private GLCapabilities glcaps;
	private GLCanvas glcanvas;
	private Animator animator;
	
	
	public RenderUI() {
		Configurator.defaultConfig().formatPattern(Shared.LOG_FORMAT).level(Shared.LOG_LEVEL).activate();

		Logger.debug("RenderUI initiated");
        
        glcaps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		glcaps.setDoubleBuffered(true);
		glcaps.setHardwareAccelerated(true);
		
		glcanvas = new GLCanvas(glcaps);
		glcanvas.addGLEventListener(this);
		glcanvas.setSize(720, 720);
		
		frame = new Frame("GCODE Interpreter - " + Shared.VERSION);
        frame.setSize(800, 800);
        frame.setResizable(true);
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
				File fileName = fileChooser.getSelectedFile();
				if(fileName != null) {
					String filePath = fileName.getAbsolutePath();
					int periodIndex = filePath.lastIndexOf(".");
					String fileExtension = filePath.substring(periodIndex);
					if(fileExtension.equals(".nc")){
						Logger.debug("Success");
					}
					else{
						JOptionPane.showMessageDialog(frame,  "Invalid File Type");
					}
				}
				
				
			}
		});
		
		file.add(openMenuItem);
		file.add(exitMenuItem);
		menuBar.add(file);
		
		frame.setMenuBar(menuBar);
		frame.setVisible(true);
	}
	
	@Override
	public void display(GLAutoDrawable glad) {
		GL2 gl = glad.getGL().getGL2();
		
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
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
		GLU glu = new GLU();
		
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glShadeModel(GL2.GL_SMOOTH);
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
		gl.glClearColor(0f, 0f, 0f, 1f);
		
		setCamera(gl, glu, 100);
		
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
		animator.stop();
		frame.remove(glcanvas);
		frame.dispose();
		System.exit(0);
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
	public void windowClosing(WindowEvent evt) {
		exit();
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
