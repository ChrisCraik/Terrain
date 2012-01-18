
package supergame;

import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

import java.awt.Font;
import java.util.Iterator;
import java.util.LinkedList;

@SuppressWarnings("deprecation")
public class ChatDisplay {
    public static String current = null;

    private final TrueTypeFont mFont;

    private class Chat {
        public Chat(double time, String message, Color color) {
            this.time = time;
            this.message = message;
            this.color = color;
        }
        double time;
        String message;
        Color color;
    }

    private final LinkedList<Chat> chatList = new LinkedList<ChatDisplay.Chat>();

    public ChatDisplay() {
        Font awtFont = new Font("Times New Roman", Font.BOLD, 24);
        mFont = new TrueTypeFont(awtFont, true);
    }

    public void addChat(double timestamp, String text, Color color) {
        chatList.addFirst(new Chat(timestamp, text, color));

        if (chatList.size() > Config.CHAT_HISTORY) {
            chatList.removeLast();
        }
    }

    public void render(double time) {
        if (current != null) {
            mFont.drawString(25, 0, "SHOUT:" + current, Color.lightGray);
        }

        int chatCount = 0;
        Iterator<Chat> it = chatList.iterator();
        while (it.hasNext()) {
            Chat c = it.next();
            chatCount++;

            if (c.time < time - Config.CHAT_FADE) {
                // chat too old, don't show
                return;
            }

            // TODO: fade opacity based on age
            mFont.drawString(50, chatCount * Config.CHAT_SPACING, c.message, c.color);
        }
    }
}
