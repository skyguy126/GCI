package com.skyguy126.gci;

import java.awt.Frame;
import java.awt.MenuBar;
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
import com.jogamp.opengl.util.Animator;

public class RenderUI implements WindowListener, GLEventListener {
	
	Frame frame;
	MenuBar menu;
	
	GLCapabilities glcaps;
	GLCanvas glcanvas;
	Animator painter;

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
        frame.setResizable(false);
        frame.addWindowListener(this);        
		frame.add(glcanvas);
		
		painter = new Animator(glcanvas);
		painter.start();
		
		frame.setVisible(true);
	}
	
	@Override
	public void display(GLAutoDrawable glad) {
		
	}

	@Override
	public void dispose(GLAutoDrawable glad) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(GLAutoDrawable glad) {
		Logger.debug("OpenGL init");
		
	}

	@Override
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
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
	public void windowClosing(WindowEvent evt) {
		Logger.debug("Window closing...");
		painter.stop();
		frame.remove(glcanvas);
		evt.getWindow().dispose();
		System.exit(0);
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
