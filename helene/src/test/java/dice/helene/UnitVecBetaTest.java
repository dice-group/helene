package dice.helene;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.Test;

import dice.helene.model.GenVecIndxModel;
import dice.helene.model.NrmlMemModelBruteForce;
import dice.helene.model.NrmlMemModelUnitVecBeta;

public class UnitVecBetaTest {

	public static final String CORRECT_WORD = "correct";
	public static final String WRONG_WORD = "wrong";

	static {
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	public static Logger LOG = LogManager.getLogger(UnitVecBetaTest.class);

	@Test
	public void testNbmTime() throws IOException {
		LOG.info("Starting InMemory Theta Model test!");
		Word2VecModel nbm = getNrmlzdTestModel();
		float queryVec[] = new float[100];
		for (int i = 0; i < 100; i++) {
			queryVec[i] = 1;
		}
		LOG.info("Initializing brute force model");
		GenVecIndxModel bruteForceModel = new NrmlMemModelBruteForce(nbm.word2vec, nbm.vectorSize);
		bruteForceModel.process();
		LOG.info("Initialization complete");
		String correctWord = bruteForceModel.getClosestEntry(queryVec);
		LOG.info("Closest Word from found through brute force: "+correctWord);
		int bucketSize = 100000;
		LOG.info("Initializing W2VNrmlMemModelUnitVecBeta Model");
		GenVecIndxModel memModel = new NrmlMemModelUnitVecBeta(nbm.word2vec, nbm.vectorSize, bucketSize);
		memModel.process();
		LOG.info("Initialization complete");
		String closestWord = memModel.getClosestEntry(queryVec);
		LOG.info("Closest Word from found through Beta model: "+closestWord);
		assertEquals(correctWord, memModel.getClosestEntry(queryVec));
	}

	public static Word2VecModel getNrmlzdTestModel() {
		int vectorSize = 100;
		Map<String, float[]> wordMap = new HashMap<>();
		float[] correctVec = new float[vectorSize];
		float[] wrongVec = new float[vectorSize];
		for (int i = 0; i < vectorSize; i++) {
			correctVec[i] = 1;
			if ((i + 1) % 2 == 0) {
				wrongVec[i] = 0.96f;
			} else {
				wrongVec[i] = 1.04f;
			}
		}
		// correctVec[0] = 1.4036378111f;
		correctVec[0] = 1.4f;
		wordMap.put(CORRECT_WORD, Word2VecMath.normalize(correctVec));
		wordMap.put(WRONG_WORD, Word2VecMath.normalize(wrongVec));
		return new Word2VecModel(wordMap, vectorSize);
	}

}
