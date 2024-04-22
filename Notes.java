import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class AudioToNotes {

    public static void main(String[] args) {
        String audioPath = "C:/Users/SaahilG25/eclipse-workspace/ChatGPTMade/src/Test2.wav";
        ArrayList<Double> my_notes = new ArrayList<Double>();
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(audioPath));
            AudioFormat format = audioStream.getFormat();
            int sampleRate = (int) format.getSampleRate();
            int numChannels = format.getChannels();

            int bufferSize = sampleRate * numChannels;
            byte[] audioBuffer = new byte[bufferSize];

            int bytesRead;
            float[] audioData = new float[bufferSize];
            
            
            while ((bytesRead = audioStream.read(audioBuffer)) != -1) {
                for (int i = 0; i < bytesRead / 2; i++) {
                    // Convert byte pair to float value
                    float sample = (float) ((audioBuffer[2 * i] & 0xFF) | (audioBuffer[2 * i + 1] << 8)) / 32767.0f;
                    audioData[i] = sample;
                }

                double pitch = (double)calculatePitch(audioData, sampleRate);
                pitch = adjustHertz(pitch);
                System.out.println("Note: " + pitch + ", " + convertToNoteName(pitch));
                my_notes.add(pitch);
                
            }

            audioStream.close();
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
        
        for (Double pitch: my_notes) {
        	try{
        		playFrequency((double)pitch, 50);	
            }
            catch (Exception e) {
            	
            }
        }
    }
    


    private static float calculatePitch(float[] audioData, int sampleRate) {
        int minFrequency = 80;  // Minimum frequency (Hz) for pitch detection
        int maxFrequency = 1200;  // Maximum frequency (Hz) for pitch detection

        float[] autocorrelation = new float[maxFrequency + 1]; // Increased array size to accommodate maxFrequency

        for (int lag = minFrequency; lag <= maxFrequency; lag++) {
            float sum = 0.0f;
            for (int i = 0; i < audioData.length - lag; i++) {
                sum += audioData[i] * audioData[i + lag];
            }
            autocorrelation[lag] = sum;
        }

        // Find the lag with the highest autocorrelation value
        int maxCorrelationLag = minFrequency;
        for (int lag = minFrequency + 1; lag <= maxFrequency; lag++) {
            if (autocorrelation[lag] > autocorrelation[maxCorrelationLag]) {
                maxCorrelationLag = lag;
            }
        }

        // Calculate the pitch frequency in Hz
        float pitch = sampleRate / (float) maxCorrelationLag;
        return pitch;
    }

 

        public static String convertToNoteName(double frequency) {
            double referenceFrequency = 440.0; // A4 reference frequency in Hz

            // Calculate the note index
            double noteIndex = 12.0 * Math.log(frequency / referenceFrequency) / Math.log(2);

            // Calculate the fractional part for precise note determination
            double fractionalPart = noteIndex % 1;

            // Map the fractional part to a specific note name
            String noteName = getNoteName(fractionalPart);

            // Calculate the octave number based on the note index
            int octave = 0;
            if (frequency > 523) {
            	octave = 5;
            } else if (frequency > 260) {
            	octave = 4;
            } else if (frequency > 130) {
            	octave = 3;
            } else {
            	octave = 0;
            }
            
            return noteName + octave;
        }

        private static String getNoteName(double fractionalPart) {
            // Array of note names
            String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

            // Calculate the note index
            int noteIndex = (int) Math.round(fractionalPart * 12);

            // Calculate the octave based on the note index
            int octave = 4 + noteIndex / 12;
            

            // Adjust the note index to fall within the valid range
            int adjustedNoteIndex = noteIndex % 12;
            if (adjustedNoteIndex < 0) {
                adjustedNoteIndex += 12; // Handle negative values
            }

            // Get the note name from the note index
            String noteName = noteNames[adjustedNoteIndex];
            if (noteName.contains("#")) {
                noteName = noteName.replace("#", "♯");
            } else {
                noteName = noteName.replace("b", "♭");
            }
         
            return noteName;// + octave;
        }


        public static void playFrequency(double frequency, int duration) throws LineUnavailableException {
        	AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat);

            line.open(audioFormat);
            line.start();

            int numSamples = 44100 * duration / 1000;
            byte[] buffer = new byte[2];
            for (int i = 0; i < numSamples; i++) {
            	double angle = 2.0 * Math.PI * i * frequency / 44100;
                short sample = (short) (Short.MAX_VALUE * Math.sin(angle));
                buffer[0] = (byte) (sample & 0xFF);
                buffer[1] = (byte) (sample >> 8);
                line.write(buffer, 0, 2);
            }    
            line.drain();
            line.stop();
            line.close();
        }

        public static double adjustHertz(double hertz) {
            final double minHertz = 130.0;  // Minimum allowed hertz value
            final double maxHertz = 1047.0; // Maximum allowed hertz value

            if (hertz >= minHertz && hertz <= maxHertz) {
                // The hertz value is already within the desired range
                return hertz;
            } else {
                // Adjust the hertz value by shifting it up or down octaves
                while (hertz < minHertz || hertz > maxHertz) {
                    if (hertz < minHertz) {
                        // Shift up one octave (multiply by 2)
                        hertz *= 2.0;
                    } else if (hertz > maxHertz) {
                        // Shift down one octave (divide by 2)
                        hertz /= 2.0;
                    }
                }
                return hertz;
            }
        }
}
