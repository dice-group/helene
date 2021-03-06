package dice.helene.model;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import dice.helene.util.MathUtil;



/**
 * Class to encapsulate Word-Embeddings in-memory model and expose methods to perform
 * search on the model. (Only works with Normalized Model)
 * 
 * This class selects {@link NrmlMemModelUnitVecBeta#compareVecCount} vectors
 * (centroids of the KMeans result on the model vectors) and then calculates the
 * cosine similarity of all words in model to those vectors.
 * 
 * It uses the knowledge about pre-processed similarities with
 * {@link NrmlMemModelUnitVecBeta#comparisonVecs} to narrow down the search of
 * closest word for the user specified vector.
 * 
 * @author Nikit
 *
 */
public class NrmlMemModelUnitVecBeta extends NrmlMemModelUnitVec {
	public static Logger LOG = LogManager.getLogger(GenVecIndxModel.class);

	public NrmlMemModelUnitVecBeta(final Map<String, float[]> embdngMap, final int vectorSize, int bucketCount) throws IOException {
		super(embdngMap, vectorSize, bucketCount);
		currentImpl+= " Beta";
	}
	
	protected int computeGamma(float[] qVec, int ringRad) {
		//Assuming normalized vector
		int gamma = 0;
		double lambda = 1d-(ringRad+1)*bucketSize;
		float minVal  = Word2VecMath.getMin(qVec);
		// double alpha = Word2VecMath.norm(qVec);
		double alpha = 1d;
		double lSq = lambda*lambda;
		
		double a = ((lSq*alpha)-(minVal*minVal));
		double b = 2d*minVal*alpha*(lSq-1d);
		double c = (lSq-1d)*alpha*alpha;
		
		double[] betaArr = MathUtil.quadraticEquationRoots(a, b, c);

		gamma = (int) Math.ceil(Math.max(Math.abs(betaArr[0]), Math.abs(betaArr[1]))/bucketSize);
		return gamma<1?1:gamma;
	}
	
	protected double calcDelta(double beta, double minVal) {
		return (1d+minVal*beta)/(Math.sqrt(1d+beta*beta+2*minVal*beta));
	}
	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector - vector to find closest word to
	 * @param subKey - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	public String getClosestEntry(float[] vector) {
		String closestWord = null;
		try {
			// Normalize incoming vector
			vector = Word2VecMath.normalize(vector);
			
			boolean wordNotFound = true;
			boolean midEmpty;
			int ringRad = -1;
			BitSet midBs;
			//New Addition
			boolean extraItr = true;
			while (wordNotFound) {
				midEmpty = false;
				ringRad++;
				//LOG.info("Ring Radius: " + ringRad);
				// calculate cosine similarity of all distances
				float[] curCompVec;
				midBs = new BitSet(embdngMap.size());
				BitSet finBitSet = null;
				for (int i = 0; i < compareVecCount; i++) {
					curCompVec = comparisonVecs[i];
					double cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(curCompVec, vector);
					int indx = getBucketIndex(cosSimVal);
					BitSet curBs = new BitSet(embdngMap.size());
					// calculate middle bitset
					if(csBucketContainer[i][indx]!=null) {
						curBs.or(csBucketContainer[i][indx]);
					}
					if (ringRad > 0) {
						orWithNeighbours(indx, ringRad, 0, csBucketContainer[i], curBs);
					}
					if (i == 0) {
						midBs.or(curBs);
						finBitSet = curBs;
					} else {
						midBs.and(curBs);
					}
					if (midBs.cardinality() == 0) {
						midEmpty = true;
						break;
					}
					orWithNeighbours(indx, 1, ringRad, csBucketContainer[i], curBs);
					if (i > 0) {
						finBitSet.and(curBs);
					}
				}
				if(!midEmpty && extraItr) {
					extraItr = false;
					// minus one to balance the ++ effect of next iteration
					int gamma = computeGamma(vector, ringRad);
					ringRad+=gamma-1;
				}
				else if (!midEmpty) {
					int nearbyWordsCount = finBitSet.cardinality();
					LOG.info("Number of nearby words: " + nearbyWordsCount);
					int[] nearbyIndexes = new int[nearbyWordsCount];
					int j = 0;
					for (int i = finBitSet.nextSetBit(0); i >= 0; i = finBitSet.nextSetBit(i + 1), j++) {
						// operate on index i here
						nearbyIndexes[j] = i;
						if (i == Integer.MAX_VALUE) {
							break; // or (i+1) would overflow
						}
					}
					closestWord = findClosestWord(nearbyIndexes, vector);
					wordNotFound = false;
				}

			}

		} catch (Exception e) {
			LOG.error("Exception has occured while finding closest word.");
			e.printStackTrace();
		}
		LOG.info("Closest word found is: " + closestWord);
		return closestWord;
	}

}
