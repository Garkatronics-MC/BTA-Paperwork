package deus.paperwork.core.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.renderer.BlendFactor;
import net.minecraft.client.render.renderer.DrawMode;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.renderer.State;
import net.minecraft.client.render.tessellator.TessellatorShader;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.core.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class RenderUtils {

	public record EmployeeRenderData(
		IconCoordinate alert,
		IconCoordinate state,
		IconCoordinate emotion,
		float progress
	) {}

	private static final Map<Entity, EmployeeRenderData> targets = new ConcurrentHashMap<>();

	public static void setTarget(Entity mob, EmployeeRenderData data) {
		targets.put(mob, data);
	}

	public static void clearTarget(Entity mob) { targets.remove(mob); }
	public static void clear()                 { targets.clear(); }

	public static void renderInfo(Minecraft mc, float partialTick, long systemTime) {
		targets.entrySet().removeIf(e -> e.getKey().removed);
		for (var entry : targets.entrySet()) {
			renderEmployee(mc, partialTick, entry.getKey(), entry.getValue());
		}
	}

	private static final double ICON_SIZE   = 0.35;
	private static final double BAR_WIDTH   = ICON_SIZE * 3;
	private static final double BAR_HEIGHT  = 0.025;
	private static final double BAR_PADDING = 0.015;

	private static void renderEmployee(Minecraft mc, float partialTick, Entity entity, EmployeeRenderData data) {
		float heightOffset = entity.getHeadHeight() + 0.7F;

		double ex = lerp(entity.xo, entity.x, partialTick);
		double ey = lerp(entity.yo, entity.y, partialTick) + heightOffset;
		double ez = lerp(entity.zo, entity.z, partialTick);

		double cx = mc.activeCamera.getX(partialTick);
		double cy = mc.activeCamera.getY(partialTick);
		double cz = mc.activeCamera.getZ(partialTick);

		float yaw   =  180 - (float) mc.activeCamera.getYRot(partialTick);
		float pitch = -(float) mc.activeCamera.getXRot(partialTick);

		GLRenderer.pushFrame();
		GLRenderer.enableState(State.BLEND);
		GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);

		GLRenderer.modelM4f()
			.translate((float)(ex - cx), (float)(ey - cy), (float)(ez - cz))
			.rotateY((float) Math.toRadians(yaw))
			.rotateX((float) Math.toRadians(pitch))
			.translate((float)(-BAR_WIDTH / 2), 0, 0.001f);

		renderProgressBar(data.progress());

		double iconY = BAR_HEIGHT + BAR_PADDING;
		renderIcon(data.alert(),   0,                iconY);
		renderIcon(data.state(),   ICON_SIZE,         iconY);
		renderIcon(data.emotion(), ICON_SIZE * 2,     iconY);

		GLRenderer.popFrame();
	}

	private static void renderProgressBar(float progress) {

		// bar_bg_texture.bind();
		TessellatorShader t = GLRenderer.getTessellator();
		GLRenderer.setColor4f(0.2f, 0.2f, 0.2f, 0.6f);
		t.startDrawing(DrawMode.QUADS);
		quad(t, 0, 0, BAR_WIDTH, BAR_HEIGHT);
		t.draw();

		GLRenderer.setColor4f(0.3f, 0.85f, 0.3f, 0.9f);
		t.startDrawing(DrawMode.QUADS);
		quad(t, 0, 0, BAR_WIDTH * progress, BAR_HEIGHT);
		t.draw();
	}

	private static void renderIcon(IconCoordinate icon, double x, double y) {
		if (icon == null) return;
		GLRenderer.setColor4f(1f, 1f, 1f, 1f);
		TessellatorShader t = GLRenderer.getTessellator();
		icon.parentAtlas.bind();
		t.startDrawing(DrawMode.QUADS);
		t.setTextureUV(icon.getIconUMin(), icon.getIconVMax()); t.addVertex(x,             y,              0);
		t.setTextureUV(icon.getIconUMax(), icon.getIconVMax()); t.addVertex(x + ICON_SIZE, y,              0);
		t.setTextureUV(icon.getIconUMax(), icon.getIconVMin()); t.addVertex(x + ICON_SIZE, y + ICON_SIZE,  0);
		t.setTextureUV(icon.getIconUMin(), icon.getIconVMin()); t.addVertex(x,             y + ICON_SIZE,  0);
		t.draw();
	}

	private static void quad(TessellatorShader t, double x, double y, double w, double h) {
		t.addVertex(x,     y,     0);
		t.addVertex(x + w, y,     0);
		t.addVertex(x + w, y + h, 0);
		t.addVertex(x,     y + h, 0);
	}

	private static double lerp(double old, double curr, float partial) {
		return old + (curr - old) * partial;
	}
}
