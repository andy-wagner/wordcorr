import java.io.*;
import java.util.*;

class TermFreq implements Comparable<TermFreq> {
	int termId;
	int freq;
	
	TermFreq(int termId) { this.termId = termId; }

	public int compareTo(TermFreq that) {
		return Integer.compare(freq, that.freq);
	}
}

class Vocab {
	String fileName;
	int termId = 0;
	long collectionSize;
	HashMap<String, TermFreq> termIdMap;
	HashMap<Integer, String> idToStrMap;

	// Quick and dirty way to specify some more stopwords (if your preprocessor has missed some)
	static final String[] stopwords = {"br", "html", "http", "www", "htmlhttpwww", "linkshttpwww", "com]"};

	Vocab(String fileName) {
		this.fileName = fileName;
		termIdMap = new HashMap<>();
		idToStrMap = new HashMap<>();
		collectionSize = 0;
	}

	boolean isStopword(String word) {
		for (String stp: stopwords) {
			if (word.equals(stp))
				return true;
		}
		return false;
	}

	void buildVocab() throws Exception {
		FileReader fr = new FileReader(fileName);
		BufferedReader br = new BufferedReader(fr);
		TermFreq tf = null;
 
		String line;

		while ((line = br.readLine()) != null) {
			String[] tokens = line.split("\\s+");

			for (String token: tokens) {
				if (isStopword(token))
					continue;

				if (!termIdMap.containsKey(token)) {
					tf = new TermFreq(termId);
					tf.freq++;
					termIdMap.put(token, tf);	
					idToStrMap.put(new Integer(termId), token);	
					termId++;
				}
				else {
					tf = termIdMap.get(token);
					tf.freq++;  // collection freq
				}
			}
			collectionSize += tokens.length;
		}

		System.out.println(String.format("Initialized vocabulary comprising %d terms", termId));
		br.close();
		fr.close();
	}

	void pruneVocab(float headp, float tailp) {
		System.out.println("Pruning vocabulary...");

		int size = vocabSize();
		int maxTf = 0;
		for (TermFreq tf: termIdMap.values()) {
			if (tf.freq > maxTf)
				maxTf = tf.freq;
		}
		
		int minCutOff = (int)(maxTf * headp);
		int maxCutOff = (int)(maxTf * tailp);

		System.out.println("Removing words with freq lower than " + minCutOff + " and higher than " + maxCutOff);

		Iterator<Map.Entry<String, TermFreq>> iter = termIdMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, TermFreq> entry = iter.next();
			TermFreq tf = entry.getValue();	
			if (tf.freq <= minCutOff || tf.freq >= maxCutOff) {
				iter.remove();
				idToStrMap.remove(tf.termId);
			}
		}

		System.out.println("vocab reduced to size " + termIdMap.size());
	}

	int getTermId(String word) { return termIdMap.containsKey(word)? termIdMap.get(word).termId : -1; }
	TermFreq getTermFreq(String word) { return termIdMap.get(word); }
	String getTerm(int id) { return idToStrMap.get(id); }
	int vocabSize() { return termId; }
}

class CooccurStats {
	TermFreq a;
	TermFreq b; 
	float p;  // conditional probability

	static final float ALPHA = 0.6f;
	static final float ONE_MINUS_ALPHA = 1.0f - ALPHA;

	CooccurStats(TermFreq a, TermFreq b) {
		this.a = a;
		this.b = b;
	}

	String encode(Vocab v) {
		String astr = v.getTerm(a.termId);
		String bstr = v.getTerm(b.termId);
		return String.format("%s\t%s\t%.4f", astr, bstr, p);
	}

	void normalize(Vocab v) {

		TermFreq atf = v.getTermFreq(v.getTerm(a.termId));
		TermFreq btf = v.getTermFreq(v.getTerm(b.termId));
		int dfa = atf.freq;
		int dfb = btf.freq; // vocab gives coll freqs

		p = ALPHA*p;
		p = p + (ONE_MINUS_ALPHA) * (float)(Math.log(1 + (double)v.collectionSize/(double)(dfa + dfb)));
	}
}

class CooccurMap {
	HashMap<String, CooccurStats> map;

	CooccurMap(Vocab v) {
		map = new HashMap<>((v.vocabSize()<<3));
	}

	void add(TermFreq tf1, TermFreq tf2, float delP) {
		String key = tf1.termId + ":" + tf2.termId;
		CooccurStats seenStats = map.get(key);
		if (seenStats == null) {
			seenStats = new CooccurStats(tf1, tf2);
			map.put(key, seenStats);
		}
		seenStats.p += delP;
	}

	int size() { return map.size(); }
}

class DocTermMatrix {
	CooccurMap map;
	String fileName;
	Vocab v;
	HashMap<Integer, TermFreq> tfvec;
	TermFreq[] buff;
	int buffSize;

	static final int MAX_DOC_SIZE = 2000;

	DocTermMatrix(String fileName, float headp, float tailp) {
		this.fileName = fileName;

		try {
			v = new Vocab(fileName);
			v.buildVocab();
			v.pruneVocab(headp, tailp);
		}
		catch (Exception ex) { ex.printStackTrace(); }

		map = new CooccurMap(v);
 		tfvec = new HashMap<>();
		buff = new TermFreq[MAX_DOC_SIZE];
	}

	void reinit() {
			buffSize = 0;
			tfvec.clear();
	}

	void vectorize(String line) {
			reinit();

			String[] tokens = line.split("\\s+");

			for (int i=0; i < tokens.length; i++) {
				int termId = v.getTermId(tokens[i]);
				if (termId == -1)
					continue;

				TermFreq seenTf = tfvec.get(termId);
				if (seenTf == null) {
					seenTf = new TermFreq(termId);
					tfvec.put(termId, seenTf);
				}
				seenTf.freq++;
			}

			for (TermFreq tf: tfvec.values()) {
				buff[buffSize++] = tf;
			}
	}

	void compute() throws Exception {
		FileReader fr = new FileReader(fileName);
		BufferedReader br = new BufferedReader(fr);
	
		String line;
		int termId;
		int docId = 0;
		float delP;

		while ((line = br.readLine()) != null) {

			vectorize(line);

			for (int i=0; i < buffSize-1; i++) {
				TermFreq a = buff[i];

				for (int j=i+1; j < buffSize; j++) {  // inner loop is only for pairwise stats
					TermFreq b = buff[j];

					delP = (float) (Math.log(1+a.freq/(float)buffSize) + Math.log(1+ b.freq/(float)buffSize));
					map.add(a, b, delP);
				}
			}
			docId++;
			if (docId % 10000 == 0)
				System.out.println("processed document " + docId);
		}	
		br.close();
		fr.close();
	}
}

class Cooccur {
	String fileName;
	DocTermMatrix dtmat;
	String outFile;
	float headp, tailp;

	Cooccur(String fileName, String outFile, float headp, float tailp) {
		this.fileName = fileName;
		this.outFile = outFile;
		this.headp = headp;
		this.tailp = tailp;
	}

	public void compute() {
		try {
			dtmat = new DocTermMatrix(fileName, headp, tailp);
			dtmat.compute();

			FileWriter fw = new FileWriter(outFile);
			BufferedWriter bw = new BufferedWriter(fw);

			for (CooccurStats stats : dtmat.map.map.values()) {
				stats.normalize(dtmat.v);
				bw.write(stats.encode(dtmat.v));
				bw.newLine();
			}

			bw.close();
			fw.close(); 
		}
		catch (Exception ex) { ex.printStackTrace(); }
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.println("usage: java Cooccur <input text file (each line a document)> <output file path> <head %-le> <tail %-le>");
			return;
		}
		String inputFile = args[0];
		String oFile = args[1];
		float headp = Float.parseFloat(args[2])/100;
		float tailp = Float.parseFloat(args[3])/100;

		Cooccur c = new Cooccur(inputFile, oFile, headp, tailp);
		c.compute();
	}
}
