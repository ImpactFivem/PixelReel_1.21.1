package com.pixelreel.permissions;

import com.pixelreel.PixelReel;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Optional bridge to a permission manager such as LuckPerms, so an admin can grant a pixelReel permission
 * without handing out operator status.
 *
 * <p>Bound reflectively against fabric-permissions-api rather than declared as a dependency: servers that do
 * not run a permission manager keep working unchanged, and pixelReel gains no hard dependency for a feature
 * most single-player worlds never touch.
 */
public final class PermissionNodes {
	public static final String WATCH = "pixelreel.watch";
	public static final String BROWSE = "pixelreel.browse";
	public static final String PLAY_TUNARR = "pixelreel.play.tunarr";
	public static final String PLAY_MOVIES = "pixelreel.play.movies";
	public static final String PLAY_SHOWS = "pixelreel.play.shows";
	public static final String CONTROL = "pixelreel.control";
	public static final String POSTERS = "pixelreel.posters";
	public static final String POSTER_URLS = "pixelreel.posters.url";
	public static final String CONFIGURE_TUNARR = "pixelreel.configure.tunarr";
	public static final String CONFIGURE_JELLYFIN = "pixelreel.configure.jellyfin";
	public static final String CONFIGURE_EMBY = "pixelreel.configure.emby";
	public static final String CONFIGURE_PLEX = "pixelreel.configure.plex";
	public static final String REFRESH_LIBRARY = "pixelreel.library.refresh";
	public static final String AUDIO_ZONE = "pixelreel.audiozone";
	public static final String SEE_HOSTS = "pixelreel.hosts";

	private static boolean resolved;
	private static @Nullable Method checkMethod;

	private PermissionNodes() {
	}

	/**
	 * The explicitly configured value for a node, or empty when no permission manager is installed or the
	 * node has been left unset. An unset node falls through to the config rule rather than denying.
	 */
	public static Optional<Boolean> value(ServerPlayer player, String node) {
		Method check = method();
		if (check == null) {
			return Optional.empty();
		}
		try {
			// The API returns the supplied fallback for an unset node, so asking twice reveals whether it is
			// actually configured: the answers only agree when the node has a real value behind it.
			boolean whenDefaultTrue = (boolean) check.invoke(null, player, node, true);
			boolean whenDefaultFalse = (boolean) check.invoke(null, player, node, false);
			return whenDefaultTrue == whenDefaultFalse ? Optional.of(whenDefaultTrue) : Optional.empty();
		} catch (ReflectiveOperationException | ClassCastException e) {
			PixelReel.LOGGER.debug("Permission node lookup failed for {}: {}", node, e.toString());
			return Optional.empty();
		}
	}

	public static boolean isAvailable() {
		return method() != null;
	}

	private static synchronized @Nullable Method method() {
		if (resolved) {
			return checkMethod;
		}
		resolved = true;
		try {
			Class<?> permissions = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
			checkMethod = permissions.getMethod("check", Entity.class, String.class, boolean.class);
			PixelReel.LOGGER.info("pixelReel permission nodes are available (fabric-permissions-api detected)");
		} catch (ClassNotFoundException e) {
			checkMethod = null;
		} catch (NoSuchMethodException e) {
			PixelReel.LOGGER.warn("fabric-permissions-api is present but its API changed; falling back to config rules");
			checkMethod = null;
		}
		return checkMethod;
	}
}
