package ca.gauntlet.overlay;

import ca.gauntlet.TheGauntletConfig;
import ca.gauntlet.TheGauntletPlugin;
import ca.gauntlet.entity.Demiboss;
import ca.gauntlet.entity.Resource;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class SceneOverlay extends Overlay
{
	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	private final Client client;
	private final TheGauntletPlugin plugin;
	private final TheGauntletConfig config;

	private Player player;

	@Inject
	public SceneOverlay(final Client client, final TheGauntletPlugin plugin, final TheGauntletConfig config)
	{
		super(plugin);

		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	@Override
	public Dimension render(final Graphics2D graphics2D)
	{
		player = client.getLocalPlayer();

		if (player == null)
		{
			return null;
		}

		renderResources(graphics2D);
		renderUtilities(graphics2D);
		renderDemibosses(graphics2D);
		renderStrongNpcs(graphics2D);
		renderWeakNpcs(graphics2D);

		return null;
	}

	private void renderResources(final Graphics2D graphics2D)
	{
		if (!config.resourceOverlay() || plugin.getResources().isEmpty())
		{
			return;
		}

		final LocalPoint localPointPlayer = player.getLocalLocation();

		for (final Resource resource : plugin.getResources())
		{
			final GameObject gameObject = resource.getGameObject();

			final LocalPoint localPointGameObject = gameObject.getLocalLocation();

			if (isOutsideRenderDistance(localPointGameObject, localPointPlayer))
			{
				continue;
			}

			if (config.resourceOverlay())
			{
				final Polygon polygon = Perspective.getCanvasTilePoly(client, localPointGameObject);

				if (polygon == null)
				{
					continue;
				}

				drawOutlineAndFill(graphics2D, config.resourceTileOutlineColor(), config.resourceTileFillColor(),
					config.resourceTileOutlineWidth(), polygon);

				OverlayUtil.renderImageLocation(client, graphics2D, localPointGameObject, resource.getIcon(), 0);
			}
		}
	}

	private void renderUtilities(final Graphics2D graphics2D)
	{
		if (!config.utilitiesOutline() || plugin.getUtilities().isEmpty())
		{
			return;
		}

		final LocalPoint localPointPlayer = player.getLocalLocation();

		for (final GameObject gameObject : plugin.getUtilities())
		{
			if (isOutsideRenderDistance(gameObject.getLocalLocation(), localPointPlayer))
			{
				continue;
			}

			final Shape shape = gameObject.getConvexHull();

			if (shape == null)
			{
				continue;
			}

			drawOutlineAndFill(graphics2D, config.utilitiesOutlineColor(), TRANSPARENT, config.utilitiesOutlineWidth(), shape);
		}
	}

	private void renderDemibosses(final Graphics2D graphics2D)
	{
		if (!config.demibossOutline() || plugin.getDemibosses().isEmpty())
		{
			return;
		}

		final LocalPoint localPointPlayer = player.getLocalLocation();

		for (final Demiboss demiboss : plugin.getDemibosses())
		{
			final NPC npc = demiboss.getNpc();

			final LocalPoint localPointNpc = npc.getLocalLocation();

			if (localPointNpc == null || npc.isDead() || isOutsideRenderDistance(localPointNpc, localPointPlayer))
			{
				continue;
			}

			final Shape shape = npc.getConvexHull();

			if (shape == null)
			{
				continue;
			}

			drawOutlineAndFill(graphics2D, demiboss.getType().getOutlineColor(), TRANSPARENT, config.demibossOutlineWidth(), shape);
		}
	}

	private void renderStrongNpcs(final Graphics2D graphics2D)
	{
		if (!config.strongNpcOutline() || plugin.getStrongNpcs().isEmpty())
		{
			return;
		}

		final LocalPoint localPointPLayer = player.getLocalLocation();

		for (final NPC npc : plugin.getStrongNpcs())
		{
			final LocalPoint localPointNpc = npc.getLocalLocation();

			if (localPointNpc == null || npc.isDead() || isOutsideRenderDistance(localPointNpc, localPointPLayer))
			{
				continue;
			}

			final Shape shape = npc.getConvexHull();

			if (shape == null)
			{
				continue;
			}

			drawOutlineAndFill(graphics2D, config.strongNpcOutlineColor(), TRANSPARENT, config.strongNpcOutlineWidth(), shape);
		}
	}

	private void renderWeakNpcs(final Graphics2D graphics2D)
	{
		if (!config.weakNpcOutline() || plugin.getWeakNpcs().isEmpty())
		{
			return;
		}

		final LocalPoint localPointPlayer = player.getLocalLocation();

		for (final NPC npc : plugin.getWeakNpcs())
		{
			final LocalPoint localPointNpc = npc.getLocalLocation();

			if (localPointNpc == null || npc.isDead() || isOutsideRenderDistance(localPointNpc, localPointPlayer))
			{
				continue;
			}

			final Shape shape = npc.getConvexHull();

			if (shape == null)
			{
				continue;
			}

			drawOutlineAndFill(graphics2D, config.weakNpcOutlineColor(), TRANSPARENT, config.weakNpcOutlineWidth(), shape);
		}
	}

	private boolean isOutsideRenderDistance(final LocalPoint localPoint, final LocalPoint playerLocation)
	{
		final int maxDistance = config.resourceRenderDistance().getDistance();

		if (maxDistance == 0)
		{
			return false;
		}

		return localPoint.distanceTo(playerLocation) >= maxDistance;
	}

	private static void drawOutlineAndFill(final Graphics2D graphics2D, final Color outlineColor, final Color fillColor, final float strokeWidth, final Shape shape)
	{
		final Color originalColor = graphics2D.getColor();
		final Stroke originalStroke = graphics2D.getStroke();

		graphics2D.setStroke(new BasicStroke(strokeWidth));
		graphics2D.setColor(outlineColor);
		graphics2D.draw(shape);

		graphics2D.setColor(fillColor);
		graphics2D.fill(shape);

		graphics2D.setColor(originalColor);
		graphics2D.setStroke(originalStroke);
	}
}