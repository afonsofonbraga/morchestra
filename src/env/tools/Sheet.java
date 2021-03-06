package tools;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;

import cartago.*;
import orchestra.*;

public class Sheet extends Artifact {

	Synth synth;
	Midi midi = null;
	ArrayList<Note> notes = null;
	LinkedHashMap<Integer, LinkedHashMap<Long, Note> > individualNotes = null;
	long startTime = 0, endTime = 0, elapsedTime = 0;
	boolean opened = false;
	long cTick = 0;										//current tick
	long maxTick = Long.MAX_VALUE;
	int neededIndex;

	void init() {
		synth = new Synth();
		opened = false;

		defineObsProperty("sheetName", "");
		defineObsProperty("hasTicks", false);
	}

	@Override
	protected void dispose() {
		if(opened)
			synth.close();
		super.dispose();
	}


	/**
	 * Agent request for a Midi object for the Music
	 */
	@OPERATION
	void readSheet(String fileName, OpFeedbackParam success) {
		midi = synth.parse(fileName);								//Obtain a Midi object with info of the music
		if(midi == null)
		{
			success.set(false);
			return;
		}

		ObsProperty sheetName = getObsProperty("sheetName");
		sheetName.updateValue(midi.name);							//Property with the sheet name

		ObsProperty hasTicks = getObsProperty("hasTicks");
		hasTicks.updateValue(midi.maxTick > 0);						//Property with the status of available ticks

		cTick = 0;
		
		individualNotes = new LinkedHashMap<Integer, LinkedHashMap<Long, Note> >();
		for(long i = 0; i < midi.maxTick; i++)
		{
			ArrayList<Note> notes = midi.getNotes(i);
			for(Note note : notes)
			{
				int index = synth.getInstrument(note.instrument);
				if(individualNotes.containsKey(index) == false)
					individualNotes.put(index, new LinkedHashMap<Long, Note>());
				individualNotes.get(index).put(i, note);
				//System.out.println("["+note.instrument+"]["+i+"] = "+note.toString());
			}
		}

		neededIndex = 0;

		success.set(true);
	}



	@OPERATION
	void getASong(OpFeedbackParam name)
	{
		try {
			File f = new File("./data");
			FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File f, String name) {
                    // We want to find only .c files
                    return name.endsWith(".mid");
                }
			};
			
			File[] files = f.listFiles(filter);

			if(files.length == 0)
			{
				name.set("invalid");
				return;
			}

			Random rand = new Random();
			int i = rand.nextInt(files.length);
			String selected = files[i].getName().replaceFirst("[.][^.]+$", "");
			name.set(selected);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			name.set("invalid");
        }

	}

	@OPERATION
	void getNeededInstrument(OpFeedbackParam name, OpFeedbackParam isValid)
	{
		name.set("");
		isValid.set(false);

		Set<Entry<Integer, LinkedHashMap<Long, Note>>> entries = individualNotes.entrySet();
		int i = 0;
		for(Entry<Integer, LinkedHashMap<Long, Note>> entry : entries)
		{
			if(i == neededIndex)
			{
				String instrumentName = synth.getInstrument(i);
				name.set(instrumentName);
				isValid.set(true);
				neededIndex = i + 1;
				break;
			}
			i++;
		}
	}	

	@OPERATION
	void nextTick(OpFeedbackParam length, OpFeedbackParam types, OpFeedbackParam instruments, OpFeedbackParam notesProp, OpFeedbackParam volume)
	{
		if(midi == null)
			return;

		do{
			notes = midi.getNotes(cTick);
			cTick++;
			
			if(notes.isEmpty()){
				endTime = System.nanoTime();
				elapsedTime = (endTime - startTime) / 1000;
				Synth.busyWaitMicros(midi.tickDuration - elapsedTime);
				startTime = System.nanoTime();
			}
		}while(notes.isEmpty());	
		

		ObsProperty hasTicks = getObsProperty("hasTicks");
		hasTicks.updateValue(cTick < midi.maxTick && cTick < maxTick);
		
		/*
		if(midi == null || this.notes.isEmpty())
		{
			String[] def = new String[0];
			length.set(0);
			types.set(def);
			instruments.set(def);
			notesProp.set(def);
			volume.set(def);
			//startTime = System.nanoTime();
			return;
		}*/

		String[] typesData = new String[this.notes.size()];
		String[] instrumentsData = new String[this.notes.size()];
		String[] notesData = new String[this.notes.size()];
		int[] volumeData = new int[this.notes.size()];

		int i = 0;
		for(Note note : this.notes)
		{
			//typesData[i] = note.type == NoteType.ON ? "ON" : "OFF";
			typesData[i] = note.type.name();
			instrumentsData[i] = note.instrument;
			notesData[i] = note.printable();
			volumeData[i] = note.velocity;
			i++;
		}

		length.set(this.notes.size());
		types.set(typesData);
		instruments.set(instrumentsData);
		notesProp.set(notesData);
		volume.set(volumeData);

		startTime = System.nanoTime();
	}

	@OPERATION
	void setMaxTick(long maxTick)
	{
		this.maxTick = maxTick;
	}
	
	/*
	@OPERATION
	void playTick()
	{
		if(midi == null || notes == null)
		{
			return;
		}
		if(!opened)
		{
			synth.open();
			opened = true;
		}
		synth.play(notes);
		startTime = System.nanoTime();
	}*/

	@OPERATION
	void waitTick()
	{
		if(midi == null || notes == null)
		{
			return;
		}
		endTime = System.nanoTime();
		elapsedTime = (endTime - startTime) / 1000;
		Synth.busyWaitMicros(midi.tickDuration - elapsedTime);
		startTime = System.nanoTime();
	}

	/*
	@OPERATION
	void firstTick(String instrumentName, OpFeedbackParam tick)
	{
		int index = synth.getInstrument(instrumentName);
		if(individualNotes.containsKey(index) == false)
		{
			tick.set(Long.MAX_VALUE);
			return;
		}

		Set<Long> keys = individualNotes.get(index).keySet();
		for(Long key : keys)
		{
			tick.set(key);
			break;
		}
	}

	@OPERATION
	void proccessTick(String instrumentName, Long tick, OpFeedbackParam nextTick)
	{
		int index = synth.getInstrument(instrumentName);
		if(!individualNotes.containsKey(index))
		{
			nextTick.set(Long.MAX_VALUE);
			return;
		}

		Note note = individualNotes.get(index).get(tick);
		if(note != null)
		{
			if(!opened)
			{
				synth.open();
				opened = true;
			}
			synth.play(note);
		}

		Set<Entry<Long,Note>> entries = individualNotes.get(index).entrySet();
		nextTick.set(Long.MAX_VALUE);
		for(Entry<Long,Note> entry : entries)
		{
			if(entry.getKey() > tick)
			{
				nextTick.set(entry.getKey());
				break;
			}
			
		}
	}

	@OPERATION
	void startTick(OpFeedbackParam tick)
	{
		tick.set(cTick);
		startTime = System.nanoTime();
	}

	@OPERATION
	void tickNotes( OpFeedbackParam length, OpFeedbackParam types, OpFeedbackParam instruments, OpFeedbackParam notes, OpFeedbackParam volume) {
		if(midi == null || this.notes == null)
		{
			String[] def = new String[0];
			length.set(0);
			types.set(def);
			instruments.set(def);
			notes.set(def);
			volume.set(def);
			//startTime = System.nanoTime();
			return;
		}

		String[] typesData = new String[this.notes.size()];
		String[] instrumentsData = new String[this.notes.size()];
		String[] notesData = new String[this.notes.size()];
		int[] volumeData = new int[this.notes.size()];

		int i = 0;
		for(Note note : this.notes)
		{
			//typesData[i] = note.type == NoteType.ON ? "ON" : "OFF";
			typesData[i] = note.type.name();
			instrumentsData[i] = note.instrument;
			notesData[i] = note.printable();
			volumeData[i] = note.velocity;
			i++;
		}

		length.set(this.notes.size());
		types.set(typesData);
		instruments.set(instrumentsData);
		notes.set(notesData);
		volume.set(volumeData);
		//startTime = System.nanoTime();
	}*/
}

