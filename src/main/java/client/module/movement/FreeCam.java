package client.module.movement;

import client.module.Category;
import client.module.Module;
import net.minecraft.util.math.Vec3d;

public class FreeCam extends Module {
   public FreeCam() {
      super("FreeCam", Category.MOVEMENT);
   }

   public Vec3d getVec3d() {
      return null;
   }
}