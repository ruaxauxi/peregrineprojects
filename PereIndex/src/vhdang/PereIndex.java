/**
 * 
 */
package vhdang;

/**
 * @author haidangvo
 *
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import mantraxml.AnnoType;
import mantraxml.Corpus;
import mantraxml.Document;
import mantraxml.E;
import mantraxml.Group;
import mantraxml.MantraXML;
import mantraxml.Unit;
import org.erasmusmc.data_mining.ontology.api.Concept;
import org.erasmusmc.data_mining.ontology.api.Language;
import org.erasmusmc.data_mining.ontology.api.Ontology;
import org.erasmusmc.data_mining.ontology.api.SemanticType;
import org.erasmusmc.data_mining.ontology.impl.file.SingleFileOntologyImpl;
import org.erasmusmc.data_mining.peregrine.api.IndexingResult;
import org.erasmusmc.data_mining.peregrine.api.Peregrine;
import org.erasmusmc.data_mining.peregrine.disambiguator.api.DisambiguationDecisionMaker;
import org.erasmusmc.data_mining.peregrine.disambiguator.api.Disambiguator;
import org.erasmusmc.data_mining.peregrine.disambiguator.api.RuleDisambiguator;
import org.erasmusmc.data_mining.peregrine.disambiguator.impl.ThresholdDisambiguationDecisionMakerImpl;
import org.erasmusmc.data_mining.peregrine.disambiguator.impl.rule_based.LooseDisambiguator;
import org.erasmusmc.data_mining.peregrine.disambiguator.impl.rule_based.StrictDisambiguator;
import org.erasmusmc.data_mining.peregrine.disambiguator.impl.rule_based.TypeDisambiguatorImpl;
import org.erasmusmc.data_mining.peregrine.impl.hash.PeregrineImpl;
import org.erasmusmc.data_mining.peregrine.normalizer.api.Normalizer;
import org.erasmusmc.data_mining.peregrine.normalizer.api.NormalizerFactory;
import org.erasmusmc.data_mining.peregrine.normalizer.impl.NormalizerFactoryImpl;
import org.erasmusmc.data_mining.peregrine.normalizer.impl.SnowballNormalizer;
import org.erasmusmc.data_mining.peregrine.tokenizer.api.TokenizerFactory;
import org.erasmusmc.data_mining.peregrine.tokenizer.impl.TokenizerFactoryImpl;
import org.erasmusmc.data_mining.peregrine.tokenizer.impl.UMLSGeneChemTokenizer;
import org.tartarus.snowball.ext.dutchStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class PereIndex {

    Peregrine peregrine = null;
    Ontology ontology;
    Map<String, String> map = new HashMap<>();
    MantraXML mantra = new MantraXML();
    Language lang;

    public void initPeregrine(String ontologyfile) {
        ontology = new SingleFileOntologyImpl(ontologyfile);
        TokenizerFactory tokenizerFactory = TokenizerFactoryImpl.createDefaultTokenizerFactory(new UMLSGeneChemTokenizer());

        // For English, uncomment this line
        //NormalizerFactory normalizerFactory = NormalizerFactoryImpl.createDefaultNormalizerFactory(new LVGNormalizer("D:/Peregrine/Peregrine/data/config/lvg.properties"));

        /**
         * Special code for Dutch stemmer
         */
        Map<Language, Normalizer> stemmerMap = new HashMap<Language, Normalizer>();
        stemmerMap.put(Language.NL, new SnowballNormalizer(new dutchStemmer()));
        stemmerMap.put(Language.EN, new SnowballNormalizer(new englishStemmer()));
        NormalizerFactory normalizerFactory = new NormalizerFactoryImpl(stemmerMap);

        // ------ end  here -------------->

        Disambiguator disambiguator = new TypeDisambiguatorImpl(new RuleDisambiguator[]{new StrictDisambiguator(), new LooseDisambiguator()});
        DisambiguationDecisionMaker disambiguationDecisionMaker = new ThresholdDisambiguationDecisionMakerImpl();

        // This parameter is used to define the set of languages in which the ontology should be loaded. Language code used is ISO639.		
        // String ontologyLanguageToLoad = "en, nl, de";

        // For now, this feature is only available for DBOntology. Thus, we can leave it as null or empty string in this sample code. 
        String ontologyLanguageToLoad = "";
        peregrine = new PeregrineImpl(ontology, tokenizerFactory, normalizerFactory, disambiguator, disambiguationDecisionMaker, ontologyLanguageToLoad);
        System.out.println("---> Loading ontology ... done! ");
    }

    /**
     * Load type mapping: Semantic types to Groups
     *
     * @param file: mapping definition (text file)
     */
    public void loadMap(String file) {
        try {
            FileReader r = new FileReader(file);
            BufferedReader reader = new BufferedReader(r);
            String text;
            String st[];
            // repeat until all lines is read
            map = new HashMap<>();
            while ((text = reader.readLine()) != null) {
                st = text.split("\\t", 3);
                map.put(st[0], st[2]);
            }
            reader.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    Map<String, String> stopWords = new HashMap<String, String>();

    /**
     * Load stop word list.
     *
     * @param file: text file contains manually defined list of stop words
     */
    public void loadStopWords(String file) {
        try {
            FileReader r = new FileReader(file);
            BufferedReader reader = new BufferedReader(r);
            String text;
            // repeat until all lines is read
            stopWords = new HashMap<>(500);
            while ((text = reader.readLine()) != null) {
                text = text.trim();
                stopWords.put(text, text);
            }
            reader.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Indexing concept: text and language
     *
     * @param text
     * @param lang
     * @return: list of Index
     */
    public List<IndexingResult> getIndex(String text, Language lang) {
        if (peregrine == null) {
            System.out.println("---Peregrine is not initialized ..");
            return null;
        }
        return peregrine.indexAndDisambiguate(text, lang);

    }

    /**
     * Helper method to remove two-charater concepts from a given ontology
     *
     * @param file: ontology file, output: file+filter
     */
    public void filterConcept(String file) {
        try {
            FileReader r = new FileReader(file);
            BufferedReader reader = new BufferedReader(r);
            FileWriter wt = new FileWriter(file + "filter");
            String text;
            String st[];
            // repeat until all lines is read
            boolean skip;
            while ((text = reader.readLine()) != null) {
                skip = false;
                st = text.split("\\t");
                if (st.length >= 2) {
                    if (st[0].startsWith("TM") && st[0].length() <= 5) {
                        skip = true;
                    }
                }
                if (!skip) {
                    wt.write(text + "\n");
                } else {
                    System.out.println("==> Skip : " + text);
                }
            }
            reader.close();
            wt.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Fill concept ID with 'zero' characters
     *
     * @param id: concept without format
     * @return: formatted concept ID
     */
    public String fillTypeID(String id) {
        int idx = 3 - id.length();
        return preFix[idx] + id;
    }
    static String preFix[] = {"T", "T0", "T00"};

    /**
     * Fill CUI with 'zero' character
     *
     * @param cui: unformatted cui
     * @return: formatted CUI
     */
    public String fillCUI(String cui) {
        int len = 7 - cui.length();
        StringBuilder CUI = new StringBuilder("C");
        for (int i = 0; i < len; i++) {
            CUI.append('0');
        }
        CUI.append(cui);
        return CUI.toString();
    }

    public void indexingConcept(String s) {
        try {
            List<IndexingResult> rs = getIndex(s, lang);
            writer.write("==> " + s + "\n");
            String cText;
            for (IndexingResult indexingResult : rs) {
                Serializable conceptId = indexingResult.getTermId().getConceptId();
                if (indexingResult.getEndPos() - indexingResult.getStartPos() <= 2) {
                    continue;
                }
                cText = s.substring(indexingResult.getStartPos(), indexingResult.getEndPos() + 1);
                writer.write("\tConceptId: " + conceptId + "\ttext: " + cText);
                Concept concept = ontology.getConcept(conceptId);
                if (!conceptCount.containsKey(cText)) {
                    Counters c = new Counters();
                    c.cID = conceptId.toString();
                    conceptCount.put(cText, c);
                } else {
                    Counters c = conceptCount.get(cText);
                    c.counter++;
                }
                Collection<SemanticType> is = concept.getSemanticTypes();
                String conceptType;
                for (SemanticType type : is) {
                    conceptType = type.getId().length() >= 3 ? "T" + type.getId() : "T0" + type.getId();
                    writer.write("\tType: " + conceptType + "\tGRP: " + map.get(conceptType) + "\n");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get list of concepts from a given text unit
     *
     * @param s: text unit
     * @param unitID: ID of unit
     * @return : list of e-elements
     */
    public List<E> getConcepts(String s, String unitID) {
        List<E> list = new ArrayList<>();
        List<IndexingResult> rs = getIndex(s, lang);
        String cText;
        int startPos, endPos;
        String CUI;
        int eIdx = 1;
        Map<String, List<String>> groups = new HashMap<>();
        for (IndexingResult indexingResult : rs) {
            groups.clear();
            Serializable conceptId = indexingResult.getTermId().getConceptId();
            startPos = indexingResult.getStartPos();
            endPos = indexingResult.getEndPos();
            if (endPos - startPos <= 2) {
                continue;
            }
            cText = s.substring(startPos, endPos + 1);
            if (stopWords.containsKey(cText)) {
                continue;
            }
            CUI = fillCUI(conceptId.toString());
            Concept concept = ontology.getConcept(conceptId);
            Collection<SemanticType> is = concept.getSemanticTypes();
            String conceptType;
            for (SemanticType type : is) {
                conceptType = type.getId();
                String typeID = fillTypeID(conceptType); // semantics type
                String grp = map.get(typeID); // get group
                if (groups.containsKey(grp)) { // group alread exists
                    List<String> ls = groups.get(grp);
                    ls.add(typeID); // add type to group
                } else { // new group
                    List<String> ls = new ArrayList<>();
                    ls.add(typeID);
                    groups.put(grp, ls);
                }
            }
            // setup concept list
            for (String key : groups.keySet()) {
                E e = new E();
                e.setId(unitID + ".e" + eIdx);
                eIdx++; // increasing index
                e.setCui(CUI);
                e.setGrp(Group.fromValue(key));
                // set off, len
                e.setOffset(startPos);
                e.setLen(endPos - startPos + 1);
                String txt = "";
                List<String> types = groups.get(key);
                for (String stype : types) {
                    txt = txt + " " + stype;
                }
                txt = txt.trim();
                e.setType(txt);
                e.setContent(cText);
                e.setSrc("UMLS");
                list.add(e);
            }
        }
        return list;
    }
    Map<String, Counters> conceptCount = new HashMap<>(500000);
    FileWriter writer = null;

    public void doAnnotate() {
        /**
         * English
         */
        /**
         * lang = Language.EN; String ontologyFile =
         * "D:/Peregrine/Peregrine/ontology/UMLS2012AA_MSH_MDR_SNOMEDCT_ENG_091012.ontologyfilter";
         *
         *
         * String mapGroup = "D:/Peregrine/Peregrine/data/concepmapping.txt";
         * String stopWordList = "D:/Peregrine/Peregrine/data/StopWords.txt";
         * loadStopWords(stopWordList); // load stop word list
         */
        /**
         * Dutch *
         */
    	 lang = Language.EN;
         String ontologyFile = "/Volumes/Data/ErasmusMC/Peregrine/Peregrine/ontology/UMLS2012AA_MSH_MDR_SNOMEDCT_ENG_291112.ontologyfilter";
         String mapGroup = "Resources/concepmapping.txt";
         String stopWordList = "Resources/StopWords.txt"; //
         loadStopWords(stopWordList); // load stop word list

         // Source path here

         String allsource[] = {//"D:/Corpora/Medline_nl_julielab_v1_man.XML",
                          "/Volumes/Data/ErasmusMC/Peregrine/Corpora/EMEA_en_121120_man.XML"
         };

         String dest_path = "./";

        initPeregrine(ontologyFile); // init Peregrine
        loadMap(mapGroup); // load sematic mapping
        for (String corName : allsource) {
            Corpus corpus;
            try {
                corpus = mantra.loadCorpus(corName);
                for (Document doc : corpus.getDocument()) {
                    for (Unit unit : doc.getUnit()) {
                        List ls = unit.getText().getContent();
                        String txt = getText(ls);
                        List<E> concepts = getConcepts(txt, unit.getId());
                        unit.setE(concepts);
                    }
                }
                corpus.setDescription("Annotated by ErasmusMC using Peregrine");
                corpus.setAnnotationType(AnnoType.STANDOFF);
                GregorianCalendar date = new GregorianCalendar();
                XMLGregorianCalendar annotationDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
                corpus.setAnnotationDate(annotationDate);
                String new_name = dest_path + splitFileName(corName) + "121121_emc-per_man.xml";
                mantra.writeXML(new_name, corpus);
                corpus = null;
            } catch (Exception ex) {
                System.out.println(ex.getLocalizedMessage());
            }
        }
    }

    public String splitFileName(String file_name) {
        File f = new File(file_name);
        String name = f.getName();
        if (name != null && name.length() > 5) {
            return name.substring(0, name.length() - ".xml".length());
        }
        return "";
    }

    public String getText(List ls) {
        StringBuilder sb = new StringBuilder();
        for (Object ob : ls) {
            if (ob instanceof String) {
                String txt = (String) ob; // text
                sb.append(txt);
            }
        }
        return sb.toString();
    }

    public void writeResult() {
        try {
            FileWriter cwriter = new FileWriter("English_concept.txt");
            for (String s : conceptCount.keySet()) {
                cwriter.write(conceptCount.get(s).cID + "\t" + s + "\t" + conceptCount.get(s).counter + "\n");
            }
            cwriter.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class Counters {

        public int counter = 1;
        public String cID = "";
    }

    public static void main(String[] args) {
        PereIndex pr = new PereIndex();
        String ontology = "/Volumes/Data/ErasmusMC/Peregrine/Peregrine/ontology/UMLS2012AA_MSH_MDR_SNOMEDCT_ENG_291112.ontologyfilter";
        pr.doAnnotate();
        pr.filterConcept(ontology);
    }
}