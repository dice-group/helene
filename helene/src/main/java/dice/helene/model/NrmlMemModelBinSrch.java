package dice.helene.model;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dice_research.topicmodeling.commons.sort.AssociativeSort;


/**
 * Class to encapsulate Word-Embeddings in-memory model and expose methods to perform
 * search on the model. (Only works with Normalized Model)
 * 
 * This class selects {@link NrmlMemModelBinSrch#compareVecCount} vectors (1
 * mean vector and others on basis Map iterator) and then calculates the cosine
 * similarity of all words in model to those vectors.
 * 
 * It uses the knowledge about pre-processed similarities with
 * {@link NrmlMemModelBinSrch#comparisonVecs} to narrow down the search of
 * closest word for the user specified vector.
 * 
 * @author Nikit
 *
 */
public class NrmlMemModelBinSrch implements GenVecIndxModel {
	public static Logger LOG = LogManager.getLogger(GenVecIndxModel.class);

	protected Map<String, float[]> embdngMap;
	protected int vectorSize;
	protected float[][] comparisonVecs = null;
	protected String[] wordArr;
	protected float[][] vecArr;
	protected int[] indxArr;
	protected double[] simValArr;
	protected int compareVecCount = 150;
	protected int bucketCount = 10;
	protected BitSet[][] csBucketContainer;

	protected NrmlMemModelBinSrch() {
	}

	public NrmlMemModelBinSrch(final Map<String, float[]> embdngMap, final int vectorSize) throws IOException {
		this.embdngMap = embdngMap;
		this.vectorSize = vectorSize;
		initVars();
		// process();
	}

	public NrmlMemModelBinSrch(final Map<String, float[]> embdngMap, final int vectorSize, int compareVecCount,
			int bucketCount) throws IOException {
		this.bucketCount = bucketCount;
		this.compareVecCount = compareVecCount;
		this.embdngMap = embdngMap;
		this.vectorSize = vectorSize;
		initVars();
		// process();
	}

	public void initVars() {
		comparisonVecs = new float[compareVecCount][vectorSize];
		csBucketContainer = new BitSet[compareVecCount][bucketCount];
	}

	public void process() throws IOException {
		LOG.info("Process from BinSrch called");
		// Setting mean as comparison vec
		setMeanComparisonVec(embdngMap, vectorSize);
		// Initialize Arrays
		processCosineSim();
		// Set other comparison vecs
		setAllComparisonVecs();
	}

	private void setBucketVals(int compVecIndex, float[] comparisonVec) {
		BitSet[] comparisonVecBuckets = csBucketContainer[compVecIndex];
		double cosSimVal;
		int i = 0;
		for (String word : embdngMap.keySet()) {
			cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(comparisonVec, embdngMap.get(word));
			// Setting bitset for the comparison vec
			setValToBucket(i, cosSimVal, comparisonVecBuckets);
			i++;
		}
	}

	private void setAllComparisonVecs() {
		int diff = (embdngMap.size() / compareVecCount) - 1;
		int curIndx = diff;
		for (int i = 1; i < compareVecCount; i++) {
			comparisonVecs[i] = vecArr[indxArr[curIndx]];
			setBucketVals(i, comparisonVecs[i]);
			curIndx += diff;
		}
	}

	protected int getBucketIndex(double cosineSimVal) {
		Double dIndx = ((bucketCount - 1d) / 2d) * (cosineSimVal + 1d);
		return Math.round(dIndx.floatValue());
	}

//	protected int getBucketIndex(double cosineSimVal) {
//		double bucketSize = 2.0 / bucketCount;
//		int index = (int) Math.round((cosineSimVal + 1.0) / (bucketSize));
//		if (index == bucketCount) {
//			--index;
//		}
//		return index;
//	}

	private void processCosineSim() {
		this.wordArr = new String[embdngMap.size()];
		this.vecArr = new float[embdngMap.size()][vectorSize];
		this.indxArr = new int[embdngMap.size()];
		this.simValArr = new double[embdngMap.size()];
		int i = 0;
		BitSet[] meanComparisonVecBuckets = csBucketContainer[0];
		for (String word : embdngMap.keySet()) {
			wordArr[i] = word;
			float[] vec = embdngMap.get(word);
			vecArr[i] = vec;
			indxArr[i] = i;
			simValArr[i] = Word2VecMath.cosineSimilarityNormalizedVecs(comparisonVecs[0], vec);
			// Setting bitset for the first comparison vec
			setValToBucket(i, simValArr[i], meanComparisonVecBuckets);
			i++;
		}
		AssociativeSort.quickSort(simValArr, indxArr);
	}

	protected void setValToBucket(int wordIndex, double cosSimVal, BitSet[] meanComparisonVecBuckets) {
		int bucketIndex = getBucketIndex(cosSimVal);
		BitSet bitset = meanComparisonVecBuckets[bucketIndex];
		if (bitset == null) {
			bitset = new BitSet(embdngMap.size());
			meanComparisonVecBuckets[bucketIndex] = bitset;
		}
		bitset.set(wordIndex);
	}

	private void setMeanComparisonVec(Map<String, float[]> embdngMap, int vectorSize) {
		float[] meanArr = new float[vectorSize];
		int totSize = embdngMap.size();
		// loop all dimensions
		for (int i = 0; i < vectorSize; i++) {
			// loop through all the words
			float[] dimsnArr = new float[totSize];
			float sum = 0;
			for (float[] vecEntry : embdngMap.values()) {
				float val = vecEntry[i];
				sum += val;
			}
			// mean
			float mean = sum / dimsnArr.length;
			meanArr[i] = mean;
		}
		this.comparisonVecs[0] = meanArr;
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
			// calculate cosine similarity of all distances
			float[] curCompVec;
			BitSet finBitSet = null;
			for (int i = 0; i < compareVecCount; i++) {
				curCompVec = comparisonVecs[i];
				double cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(curCompVec, vector);
				int indx = getBucketIndex(cosSimVal);
				BitSet curBs = new BitSet(embdngMap.size());
				if (csBucketContainer[i][indx] != null) {
					curBs.or(csBucketContainer[i][indx]);
				}
				orWithNonNullNeighbour(indx, i, curBs);
				if (!curBs.get(32092)) {
					System.out.println(i);
				}
				if (i == 0) {
					finBitSet = curBs;
				} else {
					finBitSet.and(curBs);
				}
			}
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
		} catch (Exception e) {
			LOG.error("Exception has occured while finding closest word.");
			e.printStackTrace();
		}
		LOG.info("Closest word found is: " + closestWord);
		return closestWord;
	}
	// this might be the problem since it looks beyond null neighbours
	protected void orWithNonNullNeighbour(int bIndx, int vecIndx, BitSet curBs) {
		boolean wordsFound = false;
		int rNbr = bIndx + 1;
		int lNbr = bIndx - 1;
		int rLim = csBucketContainer[vecIndx].length;
		int lLim = 0;
		BitSet temp;
		while (true) {
			if (rNbr < rLim) {
				temp = csBucketContainer[vecIndx][rNbr];
				if (orOperation(temp, curBs)) {
					wordsFound = true;
				}
			}
			if (lNbr >= lLim) {
				temp = csBucketContainer[vecIndx][lNbr];
				if (orOperation(temp, curBs)) {
					wordsFound = true;
				}
			}
			rNbr++;
			lNbr--;
			if (wordsFound) {
				break;
			} else if (rNbr >= rLim && lNbr < lLim) {
				break;
			}
		}
	}

	protected boolean orOperation(BitSet temp, BitSet curBs) {
		if (temp == null) {
			return false;
		}
		curBs.or(temp);
		return true;
	}

	protected String findClosestWord(int[] nearbyIndexes, float[] vector) {
		double minDist = -2;
		String minWord = null;
		double tempDist;
		for (int indx : nearbyIndexes) {
			float[] wordvec = vecArr[indx];
			tempDist = getSqEucDist(vector, wordvec, minDist);
			if (tempDist != -1) {
				minWord = wordArr[indx];
				minDist = tempDist;
			}
		}
		return minWord;
	}

	/**
	 * Method to find the squared value of euclidean distance between two vectors if
	 * it is less than the provided minimum distance value, otherwise return -1
	 * 
	 * @param arr1    - first vector
	 * @param arr2    - second vector
	 * @param minDist - minimum distance constraint
	 * @return squared euclidean distance between two vector or -1
	 */
	protected double getSqEucDist(float[] arr1, float[] arr2, double minDist) {
		double dist = 0;
		for (int i = 0; i < vectorSize; i++) {
			dist += Math.pow(arr1[i] - arr2[i], 2);
			if (minDist != -2 && dist > minDist)
				return -1;
		}
		return dist;
	}

	protected void genAllCosineSim() {
		double cosSimVal;
		this.wordArr = new String[embdngMap.size()];
		this.vecArr = new float[embdngMap.size()][vectorSize];
		int i = 0;
		for (String word : embdngMap.keySet()) {
			wordArr[i] = word;
			float[] vec = embdngMap.get(word);
			vecArr[i] = vec;
			for (int j = 0; j < compareVecCount; j++) {
				BitSet[] comparisonVecBuckets = csBucketContainer[j];
				cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(comparisonVecs[j], vec);
				// Setting bitset for the comparison vec
				setValToBucket(i, cosSimVal, comparisonVecBuckets);
			}
			i++;
		}
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

	public int getCompareVecCount() {
		return compareVecCount;
	}

	public void setCompareVecCount(int compareVecCount) {
		this.compareVecCount = compareVecCount;
	}

	public int getBucketCount() {
		return bucketCount;
	}

	public void setBucketCount(int bucketCount) {
		this.bucketCount = bucketCount;
	}

}
