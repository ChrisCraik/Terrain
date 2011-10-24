package supergame;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.util.ResourceLoader;

@SuppressWarnings("deprecation")
public class InputProcessor {
	private TrueTypeFont mFont;

	private String mName = null;
	private String mServer = null;
	private String mCurrent = "";

	public InputProcessor() {
		InputStream inputStream = ResourceLoader
				.getResourceAsStream("font.ttf");
		try {
			Font awtFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
			awtFont.deriveFont(12f);
			mFont = new TrueTypeFont(awtFont, true);
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Font awtFont = new Font("Times New Roman", Font.BOLD, 24);
		mFont = new TrueTypeFont(awtFont, true);
	}

	private final String DEFAULT_SERVER = "localhost:27000";

	public String getName() {
		return mName;
	}

	public String getServer() {
		return mServer;
	}

	public boolean processInput() {
		if (mCurrent != null)
			mFont.drawString(100, 300, ">" + mCurrent, Color.green);

		if (mName != null)
			mFont.drawString(100, 100, "Name = " + mName);
		if (mServer != null)
			mFont.drawString(100, 200, "Server = " + mServer);

		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				if (Keyboard.getEventKey() == Keyboard.KEY_BACK
						&& !mCurrent.isEmpty()) {
					mCurrent = mCurrent.substring(0, mCurrent.length() - 1);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
					if (mName == null) {
						mName = mCurrent;
						System.out.println("YOUR NAME IS " + mName);
						mCurrent = DEFAULT_SERVER;
					} else if (mServer == null) {
						mServer = mCurrent;
						System.out.println("YOU'RE CONNECTING TO " + mServer);
						mCurrent = null;
					} else {
						return true;
					}
				} else {
					mCurrent += Keyboard.getEventCharacter();
				}
			}
		}
		return false;
	}
}
