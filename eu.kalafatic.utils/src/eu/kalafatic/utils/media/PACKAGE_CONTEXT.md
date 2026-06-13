# PACKAGE CONTEXT

## Directory: eu.kalafatic.utils/src/eu/kalafatic/utils/media/

## Domain: general

## Components
* `SoundPlayer.java`: package eu.kalafatic.utils.media; import java.io.File; import java.io.IOException; import java.net.URL; import java.util.HashMap; import java.util.Map; import java.util.Queue; import java.util.concurrent.LinkedBlockingQueue; import java.util.concurrent.atomic.AtomicBoolean; import javax.sound.sampled.AudioFormat; import javax.sound.sampled.AudioInputStream; import javax.sound.sampled.AudioSystem; import javax.sound.sampled.Clip; import javax.sound.sampled.DataLine; import javax.sound.sampled.LineEvent; import javax.sound.sampled.LineListener; import javax.sound.sampled.LineUnavailableException; import javax.sound.sampled.SourceDataLine; import javax.sound.sampled.UnsupportedAudioFileException; import org.eclipse.swt.widgets.Display;
* `FxInit.java`: package eu.kalafatic.utils.media; public class FxInit { private static boolean initialized = false; public static synchronized void init() { if (!initialized) { initialized = true; } } }
