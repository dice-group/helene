package dice.helene.model;

import java.io.IOException;

public interface GenVecIndxModel {
	public int getVectorSize();
	public String getClosestEntry(float[] vector);
	public String getClosestSubEntry(float[] vector, String subKey);
	public void process() throws IOException;
}
