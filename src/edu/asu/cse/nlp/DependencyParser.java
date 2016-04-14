package edu.asu.cse.nlp;

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class DependencyParser {

	public static void main(String[] args) {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, parse, lemma, ner, dcoref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        String text = "blue is the colour of the sky"; // Add your text here!
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        String[] myStringArray = {"SentencesAnnotation"};
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
            SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
            IndexedWord root = dependencies.getFirstRoot();
            System.out.printf("root(ROOT-0, %s-%d)%n", root.word(), root.index());
            for (SemanticGraphEdge e : dependencies.edgeIterable()) {
                System.out.printf ("%s(%s-%d, %s-%d)%n", e.getRelation().toString(), e.getGovernor().word(), e.getGovernor().index(), e.getDependent().word(), e.getDependent().index());
            }
        }
	}

}
