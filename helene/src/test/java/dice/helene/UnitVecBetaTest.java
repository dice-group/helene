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
	
	public static final float MAX_BETA = 0.4036378111f;

	static {
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	public static Logger LOG = LogManager.getLogger(UnitVecBetaTest.class);
	
	@Test
	public void testBetaModel() throws IOException {
		for(float i=0;i<=MAX_BETA;i+=0.01) {
			String closestWord = runSingleTest(i);
			assertEquals(closestWord, CORRECT_WORD);
		}
		for(float i=(MAX_BETA+.001f);i<=5;i+=0.01) {
			String closestWord = runSingleTest(i);
			assertEquals(closestWord, WRONG_WORD);
		}
	}

	
	public String runSingleTest(float betaValue) throws IOException {
		Word2VecModel nbm = getNrmlzdTestModel(betaValue);
		float queryVec[] = new float[100];
		for (int i = 0; i < 100; i++) {
			queryVec[i] = 1;
		}
		GenVecIndxModel bruteForceModel = new NrmlMemModelBruteForce(nbm.word2vec, nbm.vectorSize);
		bruteForceModel.process();
		String correctWord = bruteForceModel.getClosestEntry(queryVec);
		int bucketSize = 10000;
		GenVecIndxModel memModel = new NrmlMemModelUnitVecBeta(nbm.word2vec, nbm.vectorSize, bucketSize);
		memModel.process();
		String closestWord = memModel.getClosestEntry(queryVec);
		assertEquals(correctWord, memModel.getClosestEntry(queryVec));
		return closestWord;
	}

	public static Word2VecModel getNrmlzdTestModel(float betaValue) {
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
		correctVec[0] = 1f + betaValue;
		wordMap.put(CORRECT_WORD, Word2VecMath.normalize(correctVec));
		wordMap.put(WRONG_WORD, Word2VecMath.normalize(wrongVec));
		return new Word2VecModel(wordMap, vectorSize);
	}

}
