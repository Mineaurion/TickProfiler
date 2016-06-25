package nallar.tickprofiler.minecraft;

import me.nallar.modpatcher.ModPatcher;
import nallar.tickprofiler.Log;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.*;


@IFMLLoadingPlugin.MCVersion("@MC_VERSION@")
@IFMLLoadingPlugin.SortingIndex(1001)
public class CoreMod implements IFMLLoadingPlugin {
	static {
		ModPatcher.requireVersion("1.8.9.95");
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[0];
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return ModPatcher.getSetupClass();
	}

	private Boolean spongePresent;

	private boolean isSpongePresent() {
		if (spongePresent == null) {
			try {
				Class.forName("org.spongepowered.asm.mixin.MixinEnvironment", false, CoreMod.class.getClassLoader());
				spongePresent = true;
			} catch (ClassNotFoundException e) {
				spongePresent = false;
			}
		}

		return spongePresent;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		Log.info("TickProfiler v@MOD_VERSION@ coremod loading. Sponge present: " + isSpongePresent());

		if (isSpongePresent())
			ModPatcher.loadPatches(CoreMod.class.getResourceAsStream("/entityhook_sponge.xml"));
		else
			ModPatcher.loadPatches(CoreMod.class.getResourceAsStream("/entityhook.xml"));
		// TODO: Not implemented
		// ModPatcher.loadPatches(CoreMod.class.getResourceAsStream("/packethook.xml"));
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
