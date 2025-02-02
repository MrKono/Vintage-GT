package gtclassic.common.event;

import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class GTEventPlayerLogin {

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		event.player.sendMessage(new TextComponentString("GregTech Classic Expansion (GTCX) alters the careful balancing and design of Vintage GregTech. Many of the features " +
				"and recipes that make Vintage GregTech unique have been modified or removed by GTCX. I am not responsible for any errors or bugs encountered while playing with " +
				"GTCX. This warning may be disabled by setting enableGTCXWarning to false in the GTC config. - e99999"));
	}
}
