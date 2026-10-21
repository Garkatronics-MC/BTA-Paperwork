package deus.paperwork.entity.cardboard_box;

public enum BoxSize {
	SMALL(0.0064F*9F, 9, 64, 0.0425F, 0.50F,  0.7F, -1.15F, -0.95F),
	REGULAR( 0.0064F*27F, 27,64, 0.0625F, 0.8F, 0.0F, -1.61F, -0.75F),
	MEDIUM(0.0064F*36F, 36,64, 0.0825F, 1.1F, 0.0F, -1.61F, -0.75F),
	LARGE(0.0064F*48F, 48,64, 0.1125F, 1.5F, 0.0F, -1.61F, -0.75F);

	private final double maxWeight;
	private final int slots;
	private final int stackSize;
	private final float scale;
	private final float size;
	private final float x;
	private final float y;
	private final float z;


	BoxSize(double maxWeight, int slots, int stackSize, float scale, float size, float x, float y, float z) {
		this.maxWeight = maxWeight;
		this.slots = slots;
		this.stackSize = stackSize;
		this.scale = scale;
		this.size = size;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public double getMaxWeight() {
		return maxWeight;
	}

	public int getSlots() {
		return slots;
	}

	public int getMaxStack() {
		return stackSize;
	}
	public float getScale() {
		return scale;
	}

	public float getSize() {
		return size;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}
}
