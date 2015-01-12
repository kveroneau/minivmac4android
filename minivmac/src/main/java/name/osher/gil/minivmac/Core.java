package name.osher.gil.minivmac;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

public class Core {
	private static final String TAG = "name.osher.gil.minivmac.Core";
	
	private static int numInsertedDisks = 0;
	private static final int frameSkip = 4;
	private static String[] diskPath;
	private static RandomAccessFile[] diskFile;
	private static Handler tickHandler = null;
	private static Runnable runTickTask = null;
	private static boolean initOk = false;
	
	static {
		System.loadLibrary("mnvmcore");
	}
	
	public static void nativeCrashed()
	{
		// TODO: Add Error handeling here.
		Log.e(TAG, "Native crashed!");
	}
	
	// initialization
	public native static boolean init(ByteBuffer rom);
	public native static boolean uninit();
	
	// emulation
	public native static void runTick();
	private native static void _resumeEmulation();
	private native static void _pauseEmulation();
	public native static boolean isPaused();
	public native static void setWantMacReset();
	public native static void setWantMacInterrupt();
	
	public static void startEmulation() {
		if (!initOk) return;
		// insert initial disks
		for(int i=0; i < getNumDrives(); i++) {
			File f = MiniVMac.getDataFile("disk"+i+".dsk");
			if (!insertDisk(f)) break;
		}
		resumeEmulation();
	}
	
	public static void resumeEmulation() {
		if (!initOk) return;
		if (!isPaused()) return;
		_resumeEmulation();
		// setup handler
		tickHandler = new Handler();
		runTickTask = new Runnable() {
			private int frame = 0;
			public void run() {
				int[] screenUpdate;
				Core.runTick();
				if (++frame == frameSkip) {
					frame = 0;
					screenUpdate = Core.getScreenUpdate();
					if (screenUpdate != null) MiniVMac.updateScreen(screenUpdate);
				}
				if (tickHandler == null) tickHandler = new Handler();
				tickHandler.post(runTickTask);
			}
		};
		tickHandler.post(runTickTask);
	}
	
	public static void pauseEmulation() {
		if (!initOk) return;
		if (isPaused()) return;
		_pauseEmulation();
		tickHandler.removeCallbacks(runTickTask);
		tickHandler = null;
		runTickTask = null;
	}
	
	// mouse
	public native static void moveMouse(int dx, int dy);
	public native static void setMousePos(int x, int y);
	public native static void setMouseButton(boolean down);
	public native static int getMouseX();
	public native static int getMouseY();
	public native static boolean getMouseButton();
	
	// keyboard
	public native static void setKeyDown(int scancode);
	public native static void setKeyUp(int scancode);
	
	// screen
	public native static int screenWidth();
	public native static int screenHeight();
	public native static int[] getScreenUpdate();
	
	// sound
	public native static byte[] soundBuf();
	public native static void setPlayOffset(int newValue);
	
	private static AudioTrack mAudioTrack;

    private static final int SOUND_SAMPLERATE = 22255;

	public static Boolean MySound_Init() {
        try {
            int minBufferSize = AudioTrack.getMinBufferSize(SOUND_SAMPLERATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_8BIT);
		    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SOUND_SAMPLERATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_8BIT, minBufferSize, AudioTrack.MODE_STREAM);

	    	mAudioTrack.pause();
	    	return true;
	    } catch (Exception e) {
	    	Log.e(TAG, "MySound_Init() can't init sound.", e);
	    	return false;
	    }
	}
	
	public static void playSound() {
		for (int i = 0 ; i < 32 ; i++) {
			byte[] buf = soundBuf();
			if (buf == null) break;
			if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
			{
				mAudioTrack.play();
			}
			int err = mAudioTrack.write(buf, 0, buf.length);
			if (err > 0) {
				setPlayOffset(err);
			} else {
				Log.w(TAG, "playSound() mAudioTrack.write() got error " + err);
				break;
			}
		}
		
		//mAudioTrack.flush();
		//mAudioTrack.pause();
	}

	public static void MySound_Start () {
		if (mAudioTrack != null) {
			mAudioTrack.play();
		}
	}

	public static void MySound_Stop () {
		if (mAudioTrack != null) {
		    mAudioTrack.stop();
		    mAudioTrack.release();
		}
	}
	
	// disks
	public native static void notifyDiskInserted(int driveNum, boolean locked);
	public native static void notifyDiskEjected(int driveNum);
	public native static int getFirstFreeDisk();
	public native static int getNumDrives();
	
	// disk driver callbacks
	public static int sonyTransfer(boolean isWrite, ByteBuffer buf, int driveNum, int start, int length) {
		if (diskFile[driveNum] == null) return -1;
		try {
			if (isWrite)
			{
				byte[] bytes = new byte[length];
				buf.rewind();
				buf.get(bytes);
				diskFile[driveNum].seek(start);
				diskFile[driveNum].write(bytes);
				return length;
			}
			else
			{
				byte[] bytes = new byte[length];
				diskFile[driveNum].seek(start);
				int actualLength = diskFile[driveNum].read(bytes);
				buf.rewind();
				buf.put(bytes);
				//Log.v(TAG, "Read " + actualLength + " bytrs from drive number " + driveNum + ".");
				return actualLength;
			}
		} catch (Exception x) {
			Log.e(TAG, "Failed to read " + length + " bytes from drive number " + driveNum + ".");
			return -1;
		}
	}
	
	public static int sonyGetSize(int driveNum) {
		if (diskFile[driveNum] == null) return 0;
		try {
			return (int)diskFile[driveNum].length();
		} catch (Exception x) {
			Log.e(TAG, "Failed to get disk size from drive number " + driveNum + ".");
			return -1;
		}
	}

	public static int sonyEject(int driveNum) {
		if (diskFile[driveNum] == null) return -1;
		int ret;
		try {
			diskFile[driveNum].close();
			ret = 0;
		} catch (Exception x) {
			ret = -1;
		}
		
		diskFile[driveNum] = null;
		diskPath[driveNum] = null;
		numInsertedDisks--;
		
		notifyDiskEjected(driveNum);
		return ret;
	}
	
	public static boolean isDiskInserted(File f) {
		if (numInsertedDisks == 0) return false;
		String path = f.getAbsolutePath();
		for(int i=0; i < diskFile.length; i++) {
			if (diskPath[i] == null) continue;
			if (path.equals(diskPath[i])) return true;
		}
		return false;
	}
	
	public static boolean insertDisk(File f) {
		int driveNum = getFirstFreeDisk();
		// check for free drive
		if (driveNum == -1) {
			MiniVMac.showAlert("I can not mount that many Disk Images. Try ejecting one.", false);
			return false;
		}
		
		// check for file
		if (!f.isFile()) return false;
		
		// check permissions
		String mode = "r";
		if (!f.canRead()) return false;
		if (f.canWrite()) mode = "rw";
		
		// open file
		try {
			diskFile[driveNum] = new RandomAccessFile(f, mode);
		} catch (Exception x) {
			diskFile[driveNum] = null;
			return false;
		}
		
		// insert disk
		notifyDiskInserted(driveNum, !f.canWrite());
		diskPath[driveNum] = f.getAbsolutePath();
		numInsertedDisks++;
		return true;
	}
	
	public static boolean insertDisk(String path) {
		return insertDisk(new File(path));
	}
	
	public static boolean hasDisksInserted() {
		return numInsertedDisks > 0;
	}
	
	// warnings
	public static void warnMsg(String shortMsg, String longMsg) {
		MiniVMac.showWarnMessage(shortMsg, longMsg, false);
	}
	
}
