
package supergame.character;

import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ControlMessage;

import javax.vecmath.Vector3f;

public class NPCController extends Controller {
    @Override
    public void control(double frameTime, ControlMessage control, ChatMessage chat) {
        control.x = (float) Math.sin(frameTime / 400.0) / 4;
        control.z = (float) Math.cos(frameTime / 400.0) / 4;
    }

    @Override
    public void response(Vector3f pos) {
    }
}
