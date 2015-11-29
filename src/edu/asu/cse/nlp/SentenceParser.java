package edu.asu.cse.nlp;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class SentenceParser {
	
	private static final String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    LexicalizedParser lp;
    List<Tree> verbList = new ArrayList<Tree>();
    List<Tree> nounList = new ArrayList<Tree>();
    List<Tree> whList = new ArrayList<Tree>();
    List<Tree> nounPhrase = new ArrayList<Tree>();
    List<Tree> vbnList = new ArrayList<Tree>();
    List<Tree> npList = new ArrayList<Tree>();
    List<Tree> adjList = new ArrayList<Tree>();
    List<Tree> nnList = new ArrayList<Tree>();
    List<Tree> nnsList = new ArrayList<Tree>();
    List<Tree> vbdList = new ArrayList<Tree>();
   
    public List<Tree> getVbdList() {
		return vbdList;
	}

	public List<Tree> getAdjList() {
		return adjList;
	}

	public List<Tree> getNnList() {
		return nnList;
	}

	public List<Tree> getNnsList() {
		return nnsList;
	}

	public List<Tree> getNpList() {
		return npList;
	}

	public List<Tree> getVbnList() {
		return vbnList;
	}

	public List<Tree> getVerbList() {
		return verbList;
	}

	public List<Tree> getNounList() {
		return nounList;
	}

	public List<Tree> getWhList() {
		return whList;
	}

	public List<Tree> getNounPhrase() {
		return nounPhrase;
	}

	public LexicalizedParser getLexicalizedParser()
    {
    	return LexicalizedParser.loadModel(parserModel);
    }
    
    public void setProcessedTree(String phrase)
    {
        	TokenizerFactory<CoreLabel> tokenizerFactory =
	            PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
	        Tokenizer<CoreLabel> tok =
	            tokenizerFactory.getTokenizer(new StringReader(phrase));
	        List<CoreLabel> rawWords2 = tok.tokenize();
	        Tree tree = getLexicalizedParser().apply(rawWords2);
	        setAttributeLists(tree);
	        
    }
    
    public void setAttributeLists(Tree tree)
    {

        for (Tree subtree: tree){
            
          if(subtree.label().value().equals("VBN")){
            vbnList.add(subtree.lastChild());
          }
          if(subtree.label().value().equals("VB")){
        	  verbList.add(subtree.lastChild());
          }
          if(subtree.label().value().equals("NNP")){
              nounList.add(subtree.lastChild());
          }
          if(subtree.label().value().equals("NN")){
              nnList.add(subtree.lastChild());
          }
          
          if(subtree.label().value().equals("WRB")){
              whList.add(subtree.lastChild());
          }
          
          if(subtree.label().value().equals("NP")){
              npList.add(subtree.lastChild());
          }
          if(subtree.label().value().equals("JJ")){
              adjList.add(subtree.lastChild());
          }
          if(subtree.label().value().equals("NNS")){
              nnsList.add(subtree.lastChild());
          }
          if(subtree.label().value().equals("VBD")){
              vbdList.add(subtree.lastChild());
          }
          
        }
//          System.out.println(verbList);
//          System.out.println(nounList);
//          System.out.println(whList);
//          System.out.println(nounPhrase);
    }
    
   
}
