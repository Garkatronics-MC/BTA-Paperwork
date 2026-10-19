package deus.paperwork.util;

import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;

import java.util.ArrayList;
import java.util.List;

public sealed interface PoscArea permits PoscArea.Area2D, PoscArea.Area3D {

	/// Enumerates every ground tile of a 2D area (one y level, from `a().y()`).
	/// Pure geometry: no world reads, so any caller can reuse it.
	static List<TilePosc> tiles(Area2D area) {
		List<TilePosc> tiles = new ArrayList<>();
		int min_x = Math.min(area.a().x(), area.b().x());
		int max_x = Math.max(area.a().x(), area.b().x());
		int min_z = Math.min(area.a().z(), area.b().z());
		int max_z = Math.max(area.a().z(), area.b().z());
		int y = area.a().y();

		for (int i = min_x; i <= max_x; i++) {
			for (int j = min_z; j <= max_z; j++) {
				tiles.add(new TilePos(i, y, j));
			}
		}

		return tiles;
	}

	boolean contains(TilePosc p);
	TilePosc center();

	record Area2D(TilePosc a, TilePosc b) implements PoscArea {
		@Override
		public boolean contains(TilePosc p) {
			int minX = Math.min(a.x(), b.x()), maxX = Math.max(a.x(), b.x());
			int minZ = Math.min(a.z(), b.z()), maxZ = Math.max(a.z(), b.z());
			return p.x() >= minX && p.x() <= maxX && p.z() >= minZ && p.z() <= maxZ;
		}

		@Override
		public TilePosc center() {
			int midX = (a.x() + b.x()) / 2;
			int midZ = (a.z() + b.z()) / 2;
			return new TilePos(midX, a.y(), midZ);
		}
	}

	record Area3D(TilePosc a, TilePosc b) implements PoscArea {
		@Override
		public boolean contains(TilePosc p) {
			int minX = Math.min(a.x(), b.x()), maxX = Math.max(a.x(), b.x());
			int minY = Math.min(a.y(), b.y()), maxY = Math.max(a.y(), b.y());
			int minZ = Math.min(a.z(), b.z()), maxZ = Math.max(a.z(), b.z());
			return p.x() >= minX && p.x() <= maxX
				&& p.y() >= minY && p.y() <= maxY
				&& p.z() >= minZ && p.z() <= maxZ;
		}

		@Override
		public TilePosc center() {
			int midX = (a.x() + b.x()) / 2;
			int midY = (a.y() + b.y()) / 2;
			int midZ = (a.z() + b.z()) / 2;
			return new TilePos(midX, midY, midZ);
		}
	}
}
