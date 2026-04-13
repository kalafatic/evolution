/*******************************************************************************
 * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.media;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

//import javax.media.Manager;
//import javax.media.MediaLocator;
//import javax.media.Player;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eclipse.swt.widgets.Display;

import eu.kalafatic.utils.constants.FConstants;
import eu.kalafatic.utils.log.Log;
import eu.kalafatic.utils.preferences.ECorePreferences;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * The Class class SoundPlayer.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings("restriction")
public class SoundPlayer implements LineListener {

	/** The sound files. */
	private File[] soundFiles = new File[0];

	/** The sound map. */
	private final Map<String, URL> soundMap = new HashMap<String, URL>();

	/** The sound tasks. */
	private final Queue<URL> soundTasks = new LinkedBlockingQueue<URL>();

	/** The inited. */
	private AtomicBoolean inited = new AtomicBoolean(false);

	/** The playing. */
	private AtomicBoolean playing = new AtomicBoolean(false);

	/** The clip. */
	private Clip clip;

//	/** The mp3 player. */
//	private Player mp3Player;
//
//	/** The media locator. */
//	private MediaLocator mediaLocator;
	
	private MediaPlayer mediaPlayer;
	
	/** The INSTANCE. */
	private static SoundPlayer INSTANCE;

	/**
	 * Gets the single instance of SoundPlayer.
	 * @return single instance of SoundPlayer
	 */
	// public SoundPlayer() {
	// // initSoundPlayer();
	// }

	/**
	 * Gets the single instance of SoundPlayer.
	 * @return single instance of SoundPlayer
	 */
	public static SoundPlayer getInstance() {
		if (INSTANCE == null) {
			synchronized (SoundPlayer.class) {
				INSTANCE = new SoundPlayer();
			}
		}
		return INSTANCE;
	}

	// Nested class for specifying volume
	/**
	 * The Enum enum Volume.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	public static enum Volume {

		/** The MUTE. */
		MUTE,

		/** The LOW. */
		LOW,

		/** The MEDIUM. */
		MEDIUM,

		/** The HIGH. */
		HIGH
	}

	/** The volume. */
	public static Volume volume = Volume.LOW;

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Inits the sound player.
	 */
	private void initSoundPlayer() {
		try {
			clip = AudioSystem.getClip();
			File soundsDir = new File(FConstants.SOUNDS_LOC);
			soundFiles = soundsDir.listFiles();

			for (int i = 0; i < soundFiles.length; i++) {
				URL clipURL = new URL("file:///" + soundFiles[i].getPath());
				soundMap.put(soundFiles[i].getName(), clipURL);
			}
			inited.set(true);

		} catch (Exception e) {
			Log.log(ECorePreferences.MODULE, e);
			Log.log("e-" + e.getCause().toString());
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Play.
	 * @param fileName the file name
	 */
	public void play(String fileName) {
		try {
			boolean enabled = FConstants.PREFERENCES.getBoolean(ECorePreferences.SOUND.getName(), (Boolean) ECorePreferences.SOUND.getDef());

			if (!enabled) {
				return;
			}

			// lazy
			if (!inited.get()) {
				initSoundPlayer();
			}

			URL clipURL = soundMap.get(fileName);

			soundTasks.offer(clipURL);

			while (!soundTasks.isEmpty()) {
				if (playing.compareAndSet(false, true)) {
					play(soundTasks.poll());
				} else {
					Thread.sleep(100);
				}
			}

		} catch (Exception e) {
			Log.log(ECorePreferences.MODULE, e);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Play.
	 * @param clipURL the clip url
	 */
	private void play(final URL clipURL) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				AudioInputStream ais = null;
				try {

					ais = AudioSystem.getAudioInputStream(clipURL);

					if (volume != Volume.MUTE) {
						if (clip.isRunning() || clip.isOpen()) {
							clip.stop();
							clip.close();
						}
						clip.setFramePosition(0);
						/* Put out audio input stream into our clip */
						clip.open(ais);
						/* Play the clip */
						clip.start();
					}
				} catch (UnsupportedAudioFileException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (LineUnavailableException e) {
					e.printStackTrace();
				} finally {
					try {
						playing.set(false);
						ais.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	// ---------------------------------------------------------------

	/**
	 * Play m p3.
	 * @param clipURL the clip url
	 */
//	private void playMP3(final URL clipURL) {
//		Display.getDefault().asyncExec(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					mediaLocator = new MediaLocator(clipURL);
//					mp3Player = Manager.createPlayer(mediaLocator);
//
//					mp3Player.realize();
//					mp3Player.start();
//
//				} catch (Exception e) {
//					System.out.println(e.getMessage());
//				} finally {
//					mp3Player.stop();
//					mp3Player.close();
//				}
//			}
//		});
//	}
	
	

	private void playMP3(final URL clipURL) {
	    FxInit.init();

	    Platform.runLater(() -> {
	        try {
	            Media media = new Media(clipURL.toExternalForm());
	            mediaPlayer = new MediaPlayer(media);

	            mediaPlayer.setOnEndOfMedia(() -> {
	                mediaPlayer.stop();
	                mediaPlayer.dispose();
	            });

	            mediaPlayer.play();

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    });
	}

	// ---------------------------------------------------------------

	/**
	 * Play format.
	 * @param clipURL the clip url
	 * @param fileName the file name
	 */
	private void playFormat(final URL clipURL, final String fileName) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				AudioInputStream din = null;
				try {
					URL url = this.getClass().getClassLoader().getResource("sounds/" + fileName);
					// File file = new File(SOUNDS_LOC+"/"+fileName);
					// AudioInputStream in =
					// AudioSystem.getAudioInputStream(file);
					AudioInputStream in = AudioSystem.getAudioInputStream(url);

					AudioFormat baseFormat = in.getFormat();
					AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat
							.getSampleRate(), false);
					din = AudioSystem.getAudioInputStream(decodedFormat, in);
					DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
					SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
					if (line != null) {
						line.open(decodedFormat);
						byte[] data = new byte[4096];
						// Start
						line.start();

						int nBytesRead;
						while ((nBytesRead = din.read(data, 0, data.length)) != -1) {
							line.write(data, 0, nBytesRead);
						}
						// Stop
						line.drain();
						line.stop();
						line.close();
						din.close();
					}

				} catch (Exception e) {
					Log.log(ECorePreferences.MODULE, e);
				} finally {
					if (din != null) {
						try {
							din.close();
						} catch (IOException e) {}
					}
				}

			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see javax.sound.sampled.LineListener#update(javax.sound.sampled.LineEvent)
	 */
	@Override
	public void update(LineEvent le) {
		LineEvent.Type type = le.getType();
		if (type == LineEvent.Type.OPEN) {
			System.out.println("OPEN");
		} else if (type == LineEvent.Type.CLOSE) {
			System.out.println("CLOSE");
			System.exit(0);
		} else if (type == LineEvent.Type.START) {
			System.out.println("START");
			// playingDialog.setVisible(true);
		} else if (type == LineEvent.Type.STOP) {
			System.out.println("STOP");
			// playingDialog.setVisible(false);
			clip.close();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * The main method.
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		try {
			SoundPlayer.getInstance().play("sounds/FileTransferCompleted.wav");
			// String path = new File(".").getCanonicalPath()
			// + "/sounds/ContactOnline.wav";
			//
			// final URL clipURL = new File(path).toURI().toURL();
			//
			// // URL clipURL = new URL("file:///" + path);
			//
			// SafeRunner.run(new ISafeRunnable() {
			//
			// @Override
			// public void run() throws Exception {
			// AudioInputStream ais;
			// try {
			// ais = AudioSystem.getAudioInputStream(clipURL);
			// Clip clip2 = AudioSystem.getClip();
			// clip2.open(ais);
			// clip2.start();
			//
			// } catch (UnsupportedAudioFileException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (LineUnavailableException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			//
			// }
			//
			// @Override
			// public void handleException(Throwable exception) {
			// // TODO Auto-generated method stub
			//
			// }
			// });
			//
			// while (true) {
			// Thread.sleep(1000);
			//
			// }

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.log(ECorePreferences.MODULE, e);
			e.printStackTrace();
			System.err.println();
		}
	}
}
