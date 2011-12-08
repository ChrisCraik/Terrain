package supergame;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.util.ResourceLoader;

import supergame.network.GameClient;
import supergame.network.GameEndPoint;
import supergame.network.GameServer;

@SuppressWarnings("deprecation")
public class InputProcessor {
	private TrueTypeFont mFont;

	private String mName = null;
	private String mServer = null;
	private String mCurrent = "";
	private final HashSet<Integer> mIgnoredKeys = new HashSet<Integer>();

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

		mIgnoredKeys.add(Keyboard.KEY_LEFT);
		mIgnoredKeys.add(Keyboard.KEY_RIGHT);
		mIgnoredKeys.add(Keyboard.KEY_UP);
		mIgnoredKeys.add(Keyboard.KEY_DOWN);

		mIgnoredKeys.add(Keyboard.KEY_LCONTROL);
		mIgnoredKeys.add(Keyboard.KEY_LMENU);
		mIgnoredKeys.add(Keyboard.KEY_LMETA);
		mIgnoredKeys.add(Keyboard.KEY_LSHIFT);

		mIgnoredKeys.add(Keyboard.KEY_RCONTROL);
		mIgnoredKeys.add(Keyboard.KEY_RMENU);
		mIgnoredKeys.add(Keyboard.KEY_RMETA);
		mIgnoredKeys.add(Keyboard.KEY_RSHIFT);

		mIgnoredKeys.add(Keyboard.KEY_HOME);
		mIgnoredKeys.add(Keyboard.KEY_END);
	}

	private final String DEFAULT_SERVER = "localhost:27000";

	private GameEndPoint getEndPoint() {
		int udp = 27000, tcp = 27000;
		String[] serverParts = mServer.split(":");

		if (serverParts.length == 2) {
			udp = tcp = Integer.parseInt(serverParts[1]);
		} else if (serverParts.length == 3) {
			udp = Integer.parseInt(serverParts[1]);
			tcp = Integer.parseInt(serverParts[2]);
		}
		try {
			if (serverParts[0].equalsIgnoreCase("me")) {
				System.out.println("creating server");
				GameServer s = new GameServer();
				s.bind(udp, tcp);
				return s;
			} else {
				GameClient c = new GameClient();
				c.connect(1000, serverParts[0], udp, tcp);
				return c;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public GameEndPoint processInput() {
		if (mName != null) {
			mFont.drawString(100, 100, "Name = " + mName);
			if (mServer != null) {
				mFont.drawString(100, 200, "Server = " + mServer);
			} else {
				mFont.drawString(100, 300, "ENTER SERVER: " + mCurrent, Color.green);
			}
		} else {
			mFont.drawString(100, 300, "ENTER NAME: " + mCurrent, Color.green);
		}
		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				if (Keyboard.getEventKey() == Keyboard.KEY_BACK
						&& !mCurrent.isEmpty()) {
					mCurrent = mCurrent.substring(0, mCurrent.length() - 1);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
					if (mName == null) {
						mName = mCurrent;
						mCurrent = DEFAULT_SERVER;
					} else if (mServer == null) {
						mServer = mCurrent;
						mCurrent = null;
					} else {
						return getEndPoint();
					}
				} else if (!mIgnoredKeys.contains(Keyboard.getEventKey())){
					mCurrent += Keyboard.getEventCharacter();
				}
			}
		}
		return null;
	}
}
