import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;

public class WebView {

	private JFrame frame;
	private String title;
	private int width;
	private int height;

	private javafx.scene.web.WebView fxWebView;
	private WebEngine engine;
	private String location;
	private String newLocation;

	private Object lock = new Object();

	public WebView(String title, int width, int height) {
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
		frame.setSize(width, height);
		final JFXPanel fxPanel = new JFXPanel();
		frame.add(fxPanel);

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				synchronized (lock) {
					// unlock when window is closing
					lock.notify();
				}
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

		frame.setVisible(true);
	}

	private void initFX(JFXPanel fxPanel) {
		fxWebView = new javafx.scene.web.WebView();
		fxPanel.setScene(new Scene(fxWebView));
		engine = fxWebView.getEngine();
		engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {

			public void changed(ObservableValue<? extends State> observable, State oldState, State newState) {
				if (newState == Worker.State.SUCCEEDED) {
					newLocation = getURL();
					System.out.println("New location: " + newLocation);
					if (newLocation.startsWith("https://oauth.vk.com/blank.html#code=")) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
							}
						});
					}
				}
			}
		});
	}

	public String openURL(final String url) {
		try {
			FXUtilities.runAndWait(new Runnable() {
				public void run() {
					engine.load(url);
				}
			});

			synchronized (lock) {
				System.out.println("Before lock.wait()");
				lock.wait();
				System.out.println("After lock.wait()");
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return newLocation;
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
