package br.com.cpqd.tts.examples;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import br.com.cpqd.tts.AudioInfo;
import br.com.cpqd.tts.EngineInfo;
import br.com.cpqd.tts.SynthesisEngine;
import br.com.cpqd.tts.SynthesisException;
import br.com.cpqd.tts.SynthesisListener;
import br.com.cpqd.tts.SynthesisSession;

public class StreamingSynthesis
{

	public static String readFile( String fileName ) throws IOException
	{
		byte[] data = Files.readAllBytes(Paths.get(fileName));
		return new String(data, StandardCharsets.UTF_8);
	}

	public static void main( String[] args ) throws SynthesisException, IOException
	{
		String jarName = new java.io.File(StreamingSynthesis.class.getProtectionDomain()
				  .getCodeSource()
				  .getLocation()
				  .getPath())
				.getName();
		if (args.length != 2)
		{
			System.out.println("Usage: java -jar " + jarName + " <product path> <text>");
			System.out.println("       java -jar " + jarName + " <product path> <SSML file>");
			return;
		}

		boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;

		String libraryPath = args[0] + File.separator + "libcpqdtts.";
		if (isWindows)
			libraryPath += "dll";
		else
			libraryPath += "so";

		// initialize the CPqD TTS engine
		SynthesisEngine engine = new SynthesisEngine(libraryPath, args[0], null);

		// the second argument can be an file name or an UTF-8 text
		String text;
		File file = new File(args[1]);
		if (file.exists())
			text = readFile(args[1]);
		else
			text = args[1];

		try {
			// create a synthesis session using the default voice
			SynthesisSession session = engine.createSession(null);
			// make sure we're using "wav/16000/16/1" encoder (Waveform 16kHz 16bps mono)
			session.setEncoderName("wav/16000/16/1");

			// synthesize the input text
			System.out.println("Synthesizing text...");
			MyListener listener = new MyListener();
			session.textToSpeechStream(text, listener, null);

			// show some information about the generated audio
			AudioInfo audioInfo = session.getAudioInfo();
			System.out.println();
			System.out.println( "    Sample rate: " + audioInfo.getSampleRate());
			System.out.println( "Bits per sample: " + audioInfo.getBitsPerSample());
			System.out.println( "       Channels: " + audioInfo.getChannels());
			System.out.println( " Audio duration: " + audioInfo.getDuration() + " ms");
			System.out.println( "    Data length: " + audioInfo.getLength() + " bytes");

			// the generated audio is stored in the session memory and can be saved to disk
			session.saveAudio("StreamingSynthesis.wav");
			System.out.println();
			System.out.println("Audio saved to 'StreamingSynthesis.wav'");

			// always release the session when it was no longer necessary
			session.release();

			// print some engine information
			EngineInfo engineInfo = engine.getEngineInfo();
			System.out.println("CPqD Texto Fala   " + engineInfo.getVersion());
			System.out.println("        Max sessions: " + engineInfo.getMaxSessions());
			System.out.println("            Customer: " + engineInfo.getCustomerName());
			System.out.println("    Current sessions: " + engineInfo.getCurrentSessions());
			System.out.println("     Operation state: " + engineInfo.getOperationState());
			System.out.println("      Remaining time: " + engineInfo.getRemainingTime());
			System.out.println("Communication errors: " + engineInfo.getCommunicationErrors());
			System.out.println("     Synthesis count: " + engineInfo.getSynthesisCount());
			System.out.println("      Synthesis time: " + engineInfo.getSynthesisTime());
			System.out.println("      License server: " + engineInfo.getLicenseServerURI());
			System.out.println();
		} catch (SynthesisException ex)
		{
			System.out.println(ex);
		}

		engine.terminate();
	}

	public static class MyListener implements SynthesisListener
	{

		private long total = 0;

		@Override
		public boolean receivedAudio(byte[] buffer, Order order, Object object)
		{
			if (order == Order.FIRST)
				System.out.println("First call!");
			else
				if (order == Order.LAST)
					System.out.println("Last call!");

			if (buffer.length > 0)
			{
				total += buffer.length;
				System.out.println("Received: " + buffer.length + " bytes");
				System.out.println("   Total: " + total + " bytes");
				System.out.println();
			}

			// Note: we're discarding the audio here!

			// return 'true' to keep synthesizing or 'false' to abort
			return true;
		}

	}
}
