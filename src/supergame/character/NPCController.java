
package supergame.character;

import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ControlMessage;

import javax.vecmath.Vector3f;

public class NPCController extends Controller {
    @Override
    public void control(double localTime, ControlMessage control, ChatMessage chat) {
        control.x = (float) Math.sin(localTime / 400.0) / 4;
        control.z = (float) Math.cos(localTime / 400.0) / 4;
    }

    @Override
    public void response(Vector3f pos) {
    }
}
