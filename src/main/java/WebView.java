import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;

public class WebView {
	private IWebViewCallback callback;
	
	private JFrame frame;
	private String title;
	private int width;
	private int height;

	private WebEngine engine;
	private String location;

	public WebView(IWebViewCallback callback, String title, int width, int height) {
		this.callback = callback;
		this.title = title;
		this.width = width;
		this.height = height;

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					initAndShowGUI();
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void initAndShowGUI() {
		frame = new JFrame();
		frame.setTitle(title);
		final JFXPanel fxPanel = new JFXPanel();
		frame.setSize(width, height);
		frame.setVisible(true);
		frame.add(fxPanel);
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				callback.onWebViewClosed();
			}
		});
		
		try {
			FXUtilities.runAndWait(new Runnable() {
				public void run() {
					initFX(fxPanel);
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	private void initFX(JFXPanel fxPanel) {
		javafx.scene.web.WebView fxWebView = new javafx.scene.web.WebView();
		fxPanel.setScene(new Scene(fxWebView));
		engine = fxWebView.getEngine();
	}

	public void openURL(final String url) {
		try {
			FXUtilities.runAndWait(new Runnable() {
				public void run() {
					engine.load(url);
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public String getURL() {
		try {
			FXUtilities.runAndWait(new Runnable() {
				public void run() {
					location = engine.getLocation();
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return location;
	}
}
