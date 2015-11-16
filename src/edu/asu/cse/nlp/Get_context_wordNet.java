package edu.asu.cse.nlp;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class Get_context_wordNet {
	

	
	
	public static void get_context(String inp){
		
		System.setProperty("wordnet.database.dir", "C:\\Program Files (x86)\\WordNet\\2.1\\dict");
		
		NounSynset nounset;
		NounSynset hyponyms[];
		
		String word = "Establish";
		//Get_context_wordNet a = new Get_context_word
		
		//ver
		
		WordNetDatabase db = WordNetDatabase.getFileInstance();
		
		Synset[] synsets = db.getSynsets(inp, SynsetType.NOUN);
		for(int i=0; i<synsets.length; i++){
			
			nounset = (NounSynset)synsets[i];
			hyponyms = nounset.getHypernyms();
			
			for(i=0; i<hyponyms.length; i++){
				
				System.out.println(hyponyms[i]);
			}
		}
		
	}
	

}
