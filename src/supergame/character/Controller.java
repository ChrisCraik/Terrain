
package supergame.character;

import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ControlMessage;

import javax.vecmath.Vector3f;

public abstract class Controller {
    // TODO: pass info about where it's char, and other chars are
    public abstract void control(double frameTime, ControlMessage control, ChatMessage chat);

    // response - callback after physics step and game logic
    public abstract void response(Vector3f pos);
}
