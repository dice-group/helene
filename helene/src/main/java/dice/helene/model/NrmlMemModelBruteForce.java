package dice.helene.model;

import java.io.IOException;
import java.util.Map;

import org.aksw.word2vecrestful.subset.DataSubsetProvider;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Class to encapsulate Word-Embeddings in-memory model and expose methods to perform
 * search on the model
 * 
 * @author Nikit
 *
 */
public class NrmlMemModelBruteForce implements GenVecIndxModel {
	public static Logger LOG = LogManager.getLogger(GenVecIndxModel.class);

	private Map<String, float[]> embdngMap;
	private int vectorSize;
	// for future use
	@SuppressWarnings("unused")
	private DataSubsetProvider dataSubsetProvider;

	public NrmlMemModelBruteForce(final Map<String, float[]> embdngMap, final int vectorSize) {
		this.embdngMap = embdngMap;
		this.vectorSize = vectorSize;

	}

	@Override
	public void process() throws IOException {
		this.dataSubsetProvider = new DataSubsetProvider();
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * 
	 * @return closest word to the given vector alongwith it's vector
	 */
	@Override
	public String getClosestEntry(float[] vector) {
		return getClosestEntry(vector, null);
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * @param subKey
	 *            - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	@Override
	public String getClosestSubEntry(float[] vector, String subKey) {
		return getClosestEntry(vector, subKey);
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * @param subKey
	 *            - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	private String getClosestEntry(float[] vector, String subKey) {
		// Normalize incoming vector
		vector = Word2VecMath.normalize(vector);
		return Word2VecMath.findClosestNormalizedVec(embdngMap, vector);
	}

	/**
	 * Method to fetch vectorSize
	 * 
	 * @return - vectorSize
	 */
	@Override
	public int getVectorSize() {
		return this.vectorSize;
	}

	/**
	 * Method to fetch Embeddings map
	 * 
	 * @return - embdngMap map
	 */
	public Map<String, float[]> getEmbdngMap() {
		return this.embdngMap;
	}

}
