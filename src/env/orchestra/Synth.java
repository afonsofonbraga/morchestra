package orchestra;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Track;

public class Synth {
	public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
	private static List<String> NOTE_NAMES = Arrays.asList("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
	private MidiChannel[] channels;
	private int volume = 80; 					// between 0 et 127
	private Synthesizer synth;
	private Soundbank sb;
	private Instrument instruments[];			//list of instruments available in the default soundbank

	//Initialize with a list of available instruments
	public Synth()
	{
		try{	
			File file = new File("soundbanks/GeneralUser.sf2"); 	// or maybe Wonderful.dls

			try { 
				sb = MidiSystem.getSoundbank(file);
			}catch (Exception e) { 
				e.printStackTrace(); 
			}
			// sb = MidiSystem.getSynthesizer().getDefaultSoundbank();

			if (sb!=null) {
				instruments = sb.getInstruments();
			}
		}catch(Exception ex){
			instruments = new Instrument[0];
		}
		System.out.println("Instruments: " + instruments.length);
	}

	public void playSample()
	{
		play(0, "6D",  1000);
		rest(500);
		
		play(0, "6D",  300);
		play(0, "6C#", 300);
		play(0, "6D",  1000);
		rest(500);
		
		play(0, "6D",  300);
		play(0, "6C#", 300);
		play(0, "6D",  1000);
		play(0, "6E",  300);
		play(0, "6E",  600);
		play(0, "6G",  300);
		play(0, "6G",  600);
		rest(500);
	}

	/* Creates a Midi object containing all the tracks events and their
		respective time durations, for the selected music
	*/
	public Midi parse(String midiFileName)
	{
		try
		{
			Path path = Paths.get(midiFileName);
			
			Midi m = new Midi(path.getFileName().toString());									//Create a Midi object
			Sequence sequence = MidiSystem.getSequence(new File(midiFileName));					//Gets the music accords sequence

			m.tickDuration = (sequence.getMicrosecondLength() / sequence.getTickLength());		//Size of each tick

			int trackNumber = 0;
			for (Track track :  sequence.getTracks()) {
				trackNumber++;
				orchestra.Track t = new orchestra.Track(trackNumber);			//Create an object Track for each track in the sequence

				for (int i=0; i < track.size(); i++) { 
					MidiEvent event = track.get(i);								//Gets an event into the track
					MidiMessage message = event.getMessage();					//Gets the message contained into the event
					if (message instanceof ShortMessage) {
						ShortMessage sm = (ShortMessage) message;				//Massage with at most 2 data bytes
						if (sm.getCommand() == NOTE_ON) {
							int key = sm.getData1();
							int octave = (key / 12)-1;							//Gets the octave
							int note = key % 12;								//Gets the note
							String noteName = NOTE_NAMES.get(note);				//Converts to the respective note name
							int velocity = sm.getData2();						//Gets the note velocity

							String instrument = instruments[sm.getChannel()].getName().trim();		//Gets the current instrument
							//instrument = instrument.replace(" ", "_").replace("'", "").replace(".", "").replace("-", "_").toLowerCase();

							Note n = new Note(NoteType.ON, noteName, octave);
							n.velocity = velocity;
							n.key = key;
							n.channel = sm.getChannel();
							n.instrument = instrument;
							t.notes.put(event.getTick(), n);			//Adds the current Note in the track file
							
						} else if (sm.getCommand() == NOTE_OFF) {
							int key = sm.getData1();
							int octave = (key / 12)-1;							//Gets the octave
							int note = key % 12;								//Gets the note
							String noteName = NOTE_NAMES.get(note);				//Converts to the respective note name
							int velocity = sm.getData2();						//Gets the note velocity

							String instrument = instruments[sm.getChannel()].getName().trim();
							//instrument = instrument.replace(" ", "_").replace("'", "").replace(".", "").replace("-", "_").toLowerCase();

							Note n = new Note(NoteType.OFF, noteName, octave);
							n.velocity = velocity;
							n.key = key;
							n.channel = sm.getChannel();
							n.instrument = instrument;
							t.notes.put(event.getTick(), n);			//Adds the current Note in the track file
							
						}
					}
				}

				m.tracks.add(t);			//Add the track to the Midi
			}

			m.normalize();
			return m;	
		}
		catch(Exception ex)
		{
			//System.out.println(ex.getMessage());
		}
		return null;
	}

	public void open()
	{
		try {
			// * Open a synthesizer
			synth = MidiSystem.getSynthesizer();
			synth.open();									//Open the device, must acquire any system requirments needed
			channels = synth.getChannels();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void close()
	{
		try {
			// * finish up
			synth.close();									//Close the synthesizer
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	//Return a String vector with all instruments needed
	public String[] getInstruments()
	{
		String[] names = new String[instruments.length];
		for(int i = 0; i < names.length; i++)
			//names[i] = instruments[i].getName().trim().replace(" ", "_").replace("'", "").replace(".", "").replace("-", "_").toLowerCase();
			names[i] = instruments[i].getName().trim();
		return names;
	}

	//Return the corresponding number for the instrument name (return 0 if instrument is not available)
	public int getInstrument(String name)
	{
		for(int i = 0; i < instruments.length; i++)
		{
			//String iName = instruments[i].getName().trim().replace(" ", "_").replace("'", "").replace(".", "").replace("-", "_").toLowerCase();
			String iName = instruments[i].getName().trim();
			if(iName.equals(name))
				return i;
		}
		return 0;
	}

	//Return the corresponding name for the instrument index (return "" if instrument is not available)
	public String getInstrument(int index)
	{
		if(index >= instruments.length)
			return null;
		return instruments[index].getName().trim();
	}

	/**
	 * Turn on a note
	 */
	public void on(int instrument, String note)
	{
		channels[instrument].noteOn(id(note), volume );
	}

	/**
	 * Turn off a note
	 */
	public void off(int instrument, String note)
	{
		channels[instrument].noteOff(id(note), volume );
	}

	/**
	 * Turn on a note
	 */
	public void on(int instrument, String note, int volume)
	{
		channels[instrument].noteOn(id(note), volume );
	}

	/**
	 * Turn off a note
	 */
	public void off(int instrument, String note, int volume)
	{
		channels[instrument].noteOff(id(note), volume );
	}

	/**
	 * Plays the given note for the given duration according Instrument NUMBER
	 */
	public void play(int instrument, String note, int duration)
	{
		try
		{
			// * start playing a note
			channels[instrument].noteOn(id(note), volume );
			// * wait
			Thread.sleep( duration );
			// * stop playing a note
			channels[instrument].noteOff(id(note));
		}
		catch(InterruptedException ex)
		{
			System.out.println(ex.getMessage());
		}
	}

	/**
	 * Plays the given note for the given duration according Instrument NAME
	 */
	public void play(String instrument, String note, int duration)
	{
		int i = getInstrument(instrument);
		play(i, note, duration);
	}
	/*
	 * Plays all the midi file
	 */
	public void play(Midi midi)
	{
		if(midi == null)
			return;
		
		long tick = 0;
		ArrayList<Note> notes = midi.getNotes(tick);				// List of notes in the current tick
		int p;
		long startTime, endTime, elapsedTime;
		while(notes != null)
		{
			startTime = System.nanoTime();
			p = (int)((tick / (double)midi.maxTick) * 100);			// Progress of the music in porcentage
			progress(p, tick == 0, false);							// Prints the progress
			play(notes);
			notes = midi.getNotes(tick);
			endTime = System.nanoTime();
			elapsedTime = (endTime - startTime) / 1000;
			busyWaitMicros(midi.tickDuration - elapsedTime);
			tick++;
		}
		progress(100, false, true);
	}

	/**
	 * Plays a sequence of notes
	 */
	public void play(ArrayList<Note> notes)
	{
		for(int i = 0; i < notes.size(); i++)
		{
			play(notes.get(i));
		}
	}

	/**
	 * Plays a single note
	 */
	public void play(Note note)
	{
		if(note.type == NoteType.ON)
		{
			on(note.channel, note.printable(), note.velocity);
		}
		else
		{
			off(note.channel, note.printable(), note.velocity);
		}
	}
	
	/**
	 * Plays nothing for the given duration
	 */
	private void rest(int duration)
	{
		// try
		// {
		// 	Thread.sleep(duration);
		// }
		// catch(InterruptedException ex)
		// {
		// 	System.out.println(ex.getMessage());
		// }
		busyWaitMicros(duration * 1000);
	}

	public static void busyWaitMicros(long micros){
        long waitUntil = System.nanoTime() + (micros * 1_000);
        while(waitUntil > System.nanoTime()){
            ;
        }
    }
	
	private void progress(int p, boolean first, boolean last)
	{
		String str = "";
		for(int i = 0; i < 100; i++)
		{
			if(i < p)
				str += "=";
			else if(i == p && i != 100)
				str += ">";
			else
				str += " ";
		}
		if(first)
		{
			System.out.print("\n");
		}
		System.out.print("Playing [" + str + "] " + p + "%");
		if(last)
			System.out.print("\n");
		else
		System.out.print("\r");
	}
	
	/**
	 * Returns the MIDI id for a given note: eg. 4C -> 60
	 * @return
	 */
	private int id(String note)
	{
		int octave = Integer.parseInt(note.substring(0, 1));
		return NOTE_NAMES.indexOf(note.substring(1)) + 12 * octave + 12;	
	}
}