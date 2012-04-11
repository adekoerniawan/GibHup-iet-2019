
package org.aksw.sparql2nl.naturallanguagegeneration;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import simplenlg.aggregation.ForwardConjunctionReductionRule;
import simplenlg.features.Feature;
import simplenlg.features.NumberAgreement;
import simplenlg.framework.*;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 *
 * @author christina
 */
public class Postprocessor {
    
    Lexicon lexicon;
    NLGFactory nlg;
    Realiser realiser;
    
    List<NPPhraseSpec> selects;
    Set<String> primaries;
    Set<String> secondaries;
    Set<SPhraseSpec> sentences;
    Set<Set<SPhraseSpec>> unions;
    NLGElement output;
    Set<SPhraseSpec> optionalsentences;
    Set<Set<SPhraseSpec>> optionalunions;
    NLGElement optionaloutput;
    Set<NLGElement> filter;
    Set<SPhraseSpec> currentlystored;
    
    public Postprocessor() {
        lexicon = Lexicon.getDefaultLexicon();
        nlg = new NLGFactory(lexicon);
        realiser = new Realiser(lexicon);
        selects = new ArrayList<NPPhraseSpec>();
        primaries = new HashSet<String>();
        secondaries = new HashSet<String>();
        sentences = new HashSet<SPhraseSpec>();
        unions = new HashSet<Set<SPhraseSpec>>();
        output = null;
        optionalsentences = new HashSet<SPhraseSpec>();
        optionalunions = new HashSet<Set<SPhraseSpec>>();
        optionaloutput = null;    
        filter = new HashSet<NLGElement>();
        currentlystored = new HashSet<SPhraseSpec>();
    }
    
    public void flush() {
        selects = new ArrayList<NPPhraseSpec>();
        primaries = new HashSet<String>();
        secondaries = new HashSet<String>();
        sentences = new HashSet<SPhraseSpec>();
        unions = new HashSet<Set<SPhraseSpec>>();
        output = null;
        optionalsentences = new HashSet<SPhraseSpec>();
        optionalunions = new HashSet<Set<SPhraseSpec>>();
        optionaloutput = null;
        filter = new HashSet<NLGElement>();
        currentlystored = new HashSet<SPhraseSpec>();
    }
    
    public void addPrimary(String s) {
        primaries.add(s);
    }
    public void addSecondary(String s) {
        secondaries.add(s);
    }
    
    public void postprocess() {
      
        // 1. 
        fuseWithSelects();
                
        // 2. compose body
        Set<NLGElement> bodyparts = new HashSet<NLGElement>();
        // if it's an ASK query without variables, simply verbalise all sentences and unions
        CoordinatedPhraseElement body = nlg.createCoordinatedPhrase();
        body.setConjunction("and");
        if (primaries.isEmpty() && secondaries.isEmpty()) {
            bodyparts.add(verbalise(sentences,new HashSet<SPhraseSpec>(),unions));           
        }
        // otherwise verbalise properties of primaries and secondaries in order importance (i.e. of number of occurrences)
        else { 
          for (String var : primaries)   aggregateTypeAndLabelInformation("?"+var);
          for (String var : secondaries) aggregateTypeAndLabelInformation("?"+var);
          while (!primaries.isEmpty() || !secondaries.isEmpty()) {
              bodyparts.add(talkAboutMostImportant());
          }
        }
        
        // 3. verbalise optionals if there are any (is added to final output in SimpleNLG)
        if (!optionalsentences.isEmpty() || !optionalunions.isEmpty()) {
             optionaloutput = verbalise(optionalsentences,new HashSet<SPhraseSpec>(),optionalunions);
        }
        
        // 4. add filters (or what remains of them) to body
        // fuse
        List<NLGElement> filters = new ArrayList(filter);
        NLGElement currentf = null;
        for (int i = 0; i < filters.size(); i++) {            
            if (i == 0) currentf = filters.get(i);
            else {
                if (i < filters.size()-1) currentf = fuse(currentf,filters.get(i),",");
                else currentf = fuse(currentf,filters.get(i),"and");
            }
        }
        // add to body 
        if (currentf != null) {
            String fstring = realiser.realiseSentence(currentf);
            if (fstring.endsWith(".")) fstring = fstring.substring(0,fstring.length()-1);
            bodyparts.add(new StringElement(fstring));
        }
        
        // 5.
        integrateLabelInfoIntoSelects(bodyparts);
        fuseWithSelectsAgain(bodyparts);
        
        // 6. put it all together
        // TODO before fusing, test whether var occurs anywhere else (maybe with output+optionaloutput?)
        for (NLGElement bodypart : fuseObjectWithSubject(bodyparts)) body.addCoordinate(bodypart);
        output = coordinate(body);
    }
    
    private NLGElement talkAboutMostImportant() { 
        // most important means primary (or, if there are no primaries, secondary) with highest number of occurrences 
        
        String current = null;
        if (!primaries.isEmpty()) {
            current = max(primaries);
            primaries.remove(current);
        }
        else if (!secondaries.isEmpty()) {
            current = max(secondaries);
            secondaries.remove(current);
        }
         
        if (current == null) return null;
        else {
            Set<SPhraseSpec> activeStore = new HashSet<SPhraseSpec>();
            Set<SPhraseSpec> passiveStore = new HashSet<SPhraseSpec>();
            Set<Set<SPhraseSpec>> unionStore = new HashSet<Set<SPhraseSpec>>();
            collectAllPhrasesContaining(current,activeStore,passiveStore,unionStore);

            NLGElement phrase = verbalise(activeStore,passiveStore,unionStore);
            
            // remove all variables from primaries and secondaries that do not occur in remaining sentences/unions anymore
            cleanUp();
            
            return phrase;
        }
    }
    
    private NLGElement verbalise(Set<SPhraseSpec> activeStore,Set<SPhraseSpec> passiveStore,Set<Set<SPhraseSpec>> unionStore) {

        // verbalise sentences 
        Set<SPhraseSpec> sentences = new HashSet<SPhraseSpec>();
        for (SPhraseSpec s : activeStore) {
            sentences.add(s);
        }
        for (SPhraseSpec s : passiveStore) {
            sentences.add(s);
        }
        currentlystored.addAll(sentences);
        for (Set<SPhraseSpec> u : unionStore) currentlystored.addAll(u);
        
        CoordinatedPhraseElement coord = nlg.createCoordinatedPhrase();
        coord.setConjunction("and");
        
        Set<SPhraseSpec> fusedsentences = fuseSubjects(fuseObjects(sentences,"and"),"and");
        for (SPhraseSpec s : fusedsentences) {
            addFilterInformation(s);
            coord.addCoordinate(s);
        }
            
        // verbalise unions 
        for (Set<SPhraseSpec> union : unionStore) {
            
            Set<SPhraseSpec> unionsentences = new HashSet<SPhraseSpec>();
            unionsentences.addAll(union);
            CoordinatedPhraseElement unioncoord = nlg.createCoordinatedPhrase();
            unioncoord.setConjunction("or");
            removeDuplicateRealisations(unionsentences);
            Set<SPhraseSpec> fusedunions = fuseSubjects(fuseObjects(unionsentences,"or"),"or");
            for (SPhraseSpec s : fusedunions) {
                addFilterInformation(s);
                unioncoord.addCoordinate(s);
            }
            NLGElement unionphrase = coordinate(unioncoord);
            if (unionphrase != null) coord.addCoordinate(unionphrase);
        }
            
        return coordinate(coord);
    }
    
    private void removeDuplicateRealisations(Set<SPhraseSpec> sentences) {
        
        Set<SPhraseSpec> duplicates = new HashSet<SPhraseSpec>();
        Set<String> realisations = new HashSet<String>();
        
        String realisation;
        for (SPhraseSpec s : sentences) {
            realisation = realiser.realiseSentence(s);
            if (realisations.contains(realisation)) duplicates.add(s);
            else realisations.add(realisation);
        }
        
        sentences.removeAll(duplicates);
    }
    
    private NLGElement coordinate(CoordinatedPhraseElement coord) {
        
        NLGElement phrase; 
        ForwardConjunctionReductionRule fccr = new ForwardConjunctionReductionRule();        
        if (coord.getChildren().isEmpty()) phrase = null;
        else if (coord.getChildren().size() == 1) phrase = coord.getChildren().get(0);
        else phrase = fccr.apply(coord);
        return phrase;
    }
    
    private Set<SPhraseSpec> fuseObjects(Set<SPhraseSpec> sentences,String conjunction) {
        
        Hashtable<String,SPhraseSpec> memory = new Hashtable<String,SPhraseSpec>();
        HashSet<SPhraseSpec> failed = new HashSet<SPhraseSpec>();
        
        for (SPhraseSpec sentence : sentences) {
            
            NLGElement subj = sentence.getSubject();
            NLGElement obj  = sentence.getObject();
            String key = realiser.realiseSentence(sentence).replace(realiser.realise(obj).toString(),"");
            
            // if subject and verb are the same, fuse objects
            if (memory.containsKey(key)) { 
                SPhraseSpec memelement = memory.get(key);               
                NLGElement newobj = fuse(obj,memelement.getObject(),conjunction);                 
                if (newobj != null) memelement.setObject(newobj); // change memelement and don't add sentence
                else failed.add(sentence); // unless objects cannot be fused (i.e. newobj == null)
            }
            else memory.put(key,sentence); // otherwise add sentence
        }
        
        failed.addAll(memory.values());
        return failed;
    }
       private Set<SPhraseSpec> fuseSubjects(Set<SPhraseSpec> sentences,String conjunction) {

        Hashtable<String,SPhraseSpec> memory = new Hashtable<String,SPhraseSpec>();
        HashSet<SPhraseSpec> failed = new HashSet<SPhraseSpec>();
        
        for (SPhraseSpec sentence : sentences) {
            
            NLGElement subj = sentence.getSubject();
            NLGElement obj  = sentence.getObject();
            String key = realiser.realiseSentence(sentence).replace(realiser.realise(subj).toString(),"");
            
            // if verb+object of sentence is the same as one already encountered, fuse subjects
            if (memory.containsKey(key)) {
                SPhraseSpec memelement = memory.get(key);               
                NLGElement newsubj = fuse(subj,memelement.getSubject(),conjunction); 
                if (newsubj != null) memelement.setSubject(newsubj); // change memelement and don't add sentence
                else failed.add(sentence); // unless objects cannot be fused (i.e. newsubj == null)
            }
            else memory.put(key,sentence); // otherwise add sentence
        }
        
        failed.addAll(memory.values());
        return failed;
    }
       
    private Set<NLGElement> fuseObjectWithSubject(Set<NLGElement> sentences) {
        // SUBJ is ?x . ?x V  OBJ -> SUBJ V OBJ
        // SUBJ V  ?x . ?x is OBJ -> SUBJ V OBJ
        
//        if (sentences == null) return new HashSet<NLGElement>();
        
        NLGElement objsent = null;
        NLGElement subjsent = null;
        String object = null;
        String subject = null;
        boolean subjIs = false;
        boolean objIs = false;       
        
        loop:
        for (NLGElement s : sentences) {
            if (s == null) {}
            else {
                object = getObject(s);
                if (object != null) {
                    for (NLGElement sent : sentences) {  
                        subject = getSubject(sent);
                        if (subject != null && subject.equals(object)) {
                            if (getVerb(s) != null && getVerb(s).equals("be")) objIs = true;
                            if (getVerb(sent) != null && getVerb(sent).equals("be")) subjIs = true;
                            if ((objIs || subjIs) && !realiser.realiseSentence(sent).contains(" not ")) {
                                if (!isNeeded(object,sentences,s,sent)) {
                                    objsent = s;
                                    subjsent = sent;
                                    break loop;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (objsent == null || subjsent == null || object == null || subject == null) return sentences;
        
        sentences.remove(objsent);
        sentences.remove(subjsent);
        if (objIs) {
            String newsent = realiser.realiseSentence(subjsent).replace(object,realiser.realiseSentence(objsent).replace(object,"").replace(" is ","").replace(" are ","").trim());
            if (newsent.endsWith(".")) newsent = newsent.substring(0,newsent.length()-1).trim();
            sentences.add(nlg.createNLGElement(newsent));           
//            objsent.setVerb(subjsent.getVerb());
//            objsent.setObject(subjsent.getObject());
//            sentences.add(objsent);
        }
        else if (subjIs) {
            String newsent = realiser.realiseSentence(objsent).replace(subject,realiser.realiseSentence(subjsent).replace(subject,"").replace(" is ","").replace(" are ","").trim());
            if (newsent.endsWith(".")) newsent = newsent.substring(0,newsent.length()-1).trim();
            sentences.add(nlg.createNLGElement(newsent));      
//            subjsent.setVerb(objsent.getVerb());
//            subjsent.setObject(objsent.getObject());
//            sentences.add(subjsent);
        }
        return fuseObjectWithSubject(sentences);
    }
    
    private boolean isNeeded(String var,Set<NLGElement> sentences,NLGElement s1,NLGElement s2) {
        // var is needed if it occurs in sentences
        String o = realiser.realiseSentence(optionaloutput);
        if (o.contains(var+" ") || o.contains(var+"'") || o.contains(var+".")) return true;
        for (NLGElement el : sentences) {
            if (!el.equals(s1) && !el.equals(s2)) {
                String s = realiser.realiseSentence(el);
                if (s.contains(var+" ") || s.contains(var+"'") || s.contains(var+".")) {
                    return true;
                }
            }
        }
        return false;
    }
    private String getSubject(NLGElement el) {
        if (el == null) return null;
        if (el.getFeature("subjects") != null) {
            ArrayList<NLGElement> subjects = new ArrayList<NLGElement>(((Collection<NLGElement>) el.getFeature("subjects")));
            if (subjects != null && !subjects.isEmpty()) {
                if (subjects.get(0).getFeature("head") != null) {
                    return subjects.get(0).getFeature("head").toString();
                }
            }
        }
        else if (el.hasFeature("coordinates")) {
            for (NLGElement c : ((Collection<NLGElement>) el.getFeature("coordinates"))) {
               if (c.getFeature("subjects") != null) {
                    ArrayList<NLGElement> subjects = new ArrayList<NLGElement>(((Collection<NLGElement>) c.getFeature("subjects")));
                    if (subjects != null && !subjects.isEmpty()) {
                        if (subjects.get(0).getFeature("head") != null) {
                            return subjects.get(0).getFeature("head").toString();
                        }
                    }
                } 
            }
        }
        return null;
    }
    private String getObject(NLGElement el) {
        if (el == null) return null;
        if (el.hasFeature("verb_phrase") && ((NLGElement) el.getFeature("verb_phrase")).hasFeature("complements")) {
            ArrayList<NLGElement> objects = new ArrayList<NLGElement>(((Collection<NLGElement>) ((NLGElement) el.getFeature("verb_phrase")).getFeature("complements")));
            if (objects != null && !objects.isEmpty()) {
                if (objects.get(0).getFeature("head") != null) {
                    return objects.get(0).getFeature("head").toString();
                }
            }
        }
        else if (el.hasFeature("coordinates")) {
            for (NLGElement c : ((Collection<NLGElement>) el.getFeature("coordinates"))) {
                if (c.hasFeature("verb_phrase") && ((NLGElement) c.getFeature("verb_phrase")).hasFeature("complements")) {
                    ArrayList<NLGElement> objects = new ArrayList<NLGElement>(((Collection<NLGElement>) ((NLGElement) c.getFeature("verb_phrase")).getFeature("complements")));
                    if (objects != null && !objects.isEmpty()) {
                        if (objects.get(0).getFeature("head") != null) {
                            return objects.get(0).getFeature("head").toString();
                        }
                    }
                }
            }
        }
        return null;
    }
    private String getVerb(NLGElement el) {
        NLGElement vp = ((NLGElement) el.getFeature("verb_phrase")); 
        if (vp != null) {
            return ((WordElement) vp.getFeature("head")).getBaseForm();
        }
        return null;
    }
    
    private NLGElement fuse(NLGElement e1,NLGElement e2,String conjunction) {
                
        if (!e1.getCategory().equals(e2.getCategory())) return null; // cannot fuse elements of different category
        
        if (e1.getCategory().equals(PhraseCategory.NOUN_PHRASE)) {
            String[] real1 = e1.getFeatureAsString("head").split(" ");
            String[] real2 = e2.getFeatureAsString("head").split(" ");
                        
            // forwards
                String prefix = "";
                int lf = 0;
                for (int i = 0; i < Math.min(real1.length,real2.length); i++) {
                    if (real1[i].toLowerCase().equals(real2[i].toLowerCase())) prefix += " " + real1[i];
                    else { lf = i; break; }
                } 
                prefix = prefix.trim();

                if (lf != 0) {
                    String newhead1 = ""; 
                    String newhead2 = "";
                    String newhead = prefix + " ";
                    for (int i = lf; i < real1.length; i++) newhead1 += real1[i] + " ";
                    for (int i = lf; i < real2.length; i++) newhead2 += " " + real2[i];
                    if (!newhead1.trim().toLowerCase().equals(newhead2.trim().toLowerCase())) {
                        newhead += newhead1 + conjunction + newhead2;
                    } else newhead += newhead1;
                    e1.setFeature("head",newhead);
                    return e1;
                }        
            
            // backwards   
            if (real1.length == real2.length) {
                String postfix = "";
                int lb = 0;
                for (int i = real1.length-1; i >= 0; i--) {
                    if (real1[i].toLowerCase().equals(real2[i].toLowerCase())) {
                        postfix = real1[i] + " " + postfix;
                        lb++;
                    }
                    else break;
                } 
                postfix = postfix.trim();

                if (lb != 0) {
                    String newhead1 = "";
                    String newhead2 = "";
                    String newhead = "";
                    for (int i = 0; i < real1.length-lb; i++) newhead1 += real1[i] + " ";
                    for (int i = 0; i < real2.length-lb; i++) newhead2 += " " + real2[i];
                    if (!newhead1.trim().toLowerCase().equals(newhead2.trim().toLowerCase())) {
                        newhead += newhead1 + conjunction + newhead2;
                    } else newhead += newhead1;
                    newhead += " " + postfix;
                    e1.setFeature("head",newhead);
                    return e1;
                }        
            }
        }
        
        return null;
    }
    
    private void collectAllPhrasesContaining(String var,
            Set<SPhraseSpec> activeStore,Set<SPhraseSpec> passiveStore,Set<Set<SPhraseSpec>> unionStore) {
        
        Set<SPhraseSpec> sentencesLeft = new HashSet<SPhraseSpec>();
        Set<Set<SPhraseSpec>> unionsLeft = new HashSet<Set<SPhraseSpec>>();
        
        for (SPhraseSpec sentence : sentences) {
                if (sentence.getSubject().getFeatureAsString("head").contains("?"+var)) {
                    activeStore.add(sentence);
                }
                else if (sentence.getObject().getFeatureAsString("head").contains("?"+var)) {
                    if (((WordElement) sentence.getVerb()).getBaseForm().equals("be")) {
                        NLGElement obj = sentence.getObject();
                        sentence.setObject(sentence.getSubject());
                        sentence.setSubject(obj);
                        activeStore.add(sentence);
                    } 
                    else if (((WordElement) sentence.getVerb()).getBaseForm().equals("have")) {
                        sentencesLeft.add(sentence);
                    } 
                    else { 
                        sentence.setFeature(Feature.PASSIVE,true);
                        passiveStore.add(sentence);
                    }
                }
                else sentencesLeft.add(sentence);
        }
        for (Set<SPhraseSpec> union : unions) {
            boolean takeit = false;
            for (SPhraseSpec sentence : union) {
                takeit = false;
                if (sentence.getSubject().getFeatureAsString("head").contains("?"+var)) takeit = true;
                else if (sentence.getObject().getFeatureAsString("head").contains("?"+var)) {
                    if (((WordElement) sentence.getVerb()).getBaseForm().equals("be")) {
                        NLGElement obj = sentence.getObject();
                        sentence.setObject(sentence.getSubject());
                        sentence.setSubject(obj);
                    } 
                    else sentence.setFeature(Feature.PASSIVE,true);
                    takeit = true;
                }
            }
            if (takeit) unionStore.add(union);
            else unionsLeft.add(union);
        }
         
        sentences = sentencesLeft;
        unions = unionsLeft;
    }
    
    private void aggregateTypeAndLabelInformation(String var) {
        // for now ignoring unions and assuming there are not multiple type and label infos
        
        SPhraseSpec type = null; 
        SPhraseSpec label = null;
        SPhraseSpec name = null;
        Hashtable<String,Boolean> optionalMap = new Hashtable<String,Boolean>();       
        
        // collect the above SPhraseSpecs
        for (SPhraseSpec s : sentences) {
            String sstring = realiser.realiseSentence(s);
            if ((sstring.startsWith(var+"\'s type is ") || sstring.startsWith(var+"\' type is ")) && !sstring.startsWith(var+"\'s type is ?")) {
                type = s; optionalMap.put(sstring,false);
            }
            else if (sstring.startsWith(var+"\'s label is ") || sstring.startsWith(var+"\' label is ")) {
                label = s; optionalMap.put(sstring,false);
            }
            else if (sstring.startsWith(var+"\'s name is ") || sstring.startsWith(var+"\' name is ")) {
                name = s; optionalMap.put(sstring,false);
            }
        } 
        for (SPhraseSpec s : optionalsentences) {
            String sstring = realiser.realiseSentence(s);
            if ((sstring.startsWith(var+"\'s type is ") || sstring.startsWith(var+"\' type is ")) && !sstring.startsWith(var+"\'s type is ?")) {
                type = s; optionalMap.put(sstring,true);
            }
            else if (sstring.startsWith(var+"\'s label is ") || sstring.startsWith(var+"\' label is ")) {
                label = s; optionalMap.put(sstring,true);
            }
            else if (sstring.startsWith(var+"\'s name is ") || sstring.startsWith(var+"\' name is ")) {
                name = s; optionalMap.put(sstring,true);
            } 
        } 
        
        if (type !=null || label != null || name != null) {
        // build a single sentence containing all those infos
        SPhraseSpec newsentence = nlg.createClause();
        newsentence.setSubject(var);
        newsentence.setFeature(Feature.NUMBER,NumberAgreement.SINGULAR);
        boolean opt = false;
        NPPhraseSpec objnp = null;
        NPPhraseSpec np = null;
        if (label != null || name != null) {
            String noun = ""; 
            boolean already = false;
            if (label != null) { 
                String sstring = realiser.realiseSentence(label);
                String lang = checkLanguage(label.getObject().getFeatureAsString("head"));
                if (lang != null) noun += lang + " ";
                String pattern;
                if (var.endsWith("s")) pattern = var+"\' label is "; 
                else pattern = var+"\'s label is "; 
                noun += "label " + sstring.replace(pattern,"").replaceAll("\\.",""); 
                already = true; 
                if (optionalMap.get(sstring)) { optionalsentences.remove(label); opt = true; }
                else sentences.remove(label);
            }
            if (name != null) { 
                if (already) noun += " and ";
                String sstring = realiser.realiseSentence(name);
                String lang = checkLanguage(name.getObject().getFeatureAsString("head"));
                if (lang != null) noun += lang + " ";
                String pattern;
                if (var.endsWith("s")) pattern = var+"\' name is "; 
                else pattern = var+"\'s name is "; 
                noun += "name " + sstring.replace(pattern,"").replaceAll("\\.","");
                if (optionalMap.get(sstring)) { optionalsentences.remove(name); opt = true; }
                else sentences.remove(name);
                already = true;
            }
            
            np = nlg.createNounPhrase(); 
            np.setHead("the "+noun);
        }
        
        if (type != null) {
            String sstring = realiser.realiseSentence(type);
            String pattern;
            if (var.endsWith("s")) pattern = var+"\' type is "; 
            else pattern = var+"\'s type is "; 
            String classstring = sstring.replace(pattern,"").replaceAll("\\.","");
            String determiner;
            if (Pattern.matches("[a,i,e,u,o,A,I,E,U,O].*",classstring)) determiner = "an";
            else determiner = "a";
            objnp = nlg.createNounPhrase(determiner,classstring);
            if (np != null) objnp.addPostModifier(("with " + realiser.realise(np)).replaceAll("\\.",""));
            newsentence.setVerb("be");
            newsentence.setObject(objnp);
            // removal:
            if (optionalMap.get(sstring)) { optionalsentences.remove(type); opt = true; }
            else sentences.remove(type);  
        }
        else {
            newsentence.setVerb("have");
            newsentence.setObject(np);
        }
        if (opt) newsentence.setFeature("modal","may");
        
        sentences.add(newsentence); 
        }
    }
    private String checkLanguage(String var) {
        
         String out = null;

         NLGElement usedfilter = null;
         for (NLGElement f : filter) {
            String fstring = realiser.realiseSentence(f);
            if (fstring.startsWith(var+" is in English")) {
                out = "English";
                usedfilter = f;
                String remainder = fstring.replace(var+" is in English","").replaceAll("\\.","");
                if (remainder.trim().startsWith("and") || remainder.trim().startsWith("And")) remainder = remainder.replaceFirst("and","").replaceFirst("And","").trim();
                if (!remainder.isEmpty()) filter.add(nlg.createNLGElement(remainder));
                break;
            }
         }
         filter.remove(usedfilter);
         
         if (out != null) return out;
        
         Set<SPhraseSpec> language = new HashSet<SPhraseSpec>();
         Set<SPhraseSpec> usedlanguage = new HashSet<SPhraseSpec>();

            for (SPhraseSpec s : sentences) {
                if (realiser.realiseSentence(s).startsWith(var+" is in ")) language.add(s);
            }
            for (SPhraseSpec s : optionalsentences) {
                if (realiser.realiseSentence(s).startsWith(var+" is in ")) language.add(s);
            }

            for (SPhraseSpec lang : language) {
                String subj = lang.getSubject().getFeatureAsString("head");
                if (subj.equals(var)) {
                    out = realiser.realiseSentence(lang).replace(var+" is in ","").replaceAll("\\.","");
                    usedlanguage.add(lang);
                    break;
                }
            }
            sentences.removeAll(usedlanguage);
            optionalsentences.removeAll(usedlanguage);
         
            return out;
         
    }
    
    private void addFilterInformation(SPhraseSpec sentence) {
        
        String obj  = realiser.realise(sentence.getObject()).toString();
        Pattern p = Pattern.compile(".*(\\?([\\w]*))(\\s|\\z)");
        
        String[] comparison = {" is greater than or equal to ",
                                           " is greather than ",
                                           " is less than ",
                                           " is less than or equal to "};
        
        Set<NLGElement> usedFilters = new HashSet<NLGElement>();
        
        // attach filter information to object
        Matcher m = p.matcher(obj);
        if (m.find()) { // i.e. if the object is a variable at the end of the phrase
            String var = m.group(1);
            String newhead = obj;
            for (NLGElement f : filter) {
                String fstring = realiser.realiseSentence(f);
                if (fstring.trim().endsWith(".")) fstring = fstring.substring(0,fstring.length()-1);
                if (fstring.startsWith(var + " matches ")) {
                    String match = fstring.replace(var+" matches ","");
                    if (((WordElement) sentence.getVerb()).getBaseForm().equals("be")
                            && !occursAnyWhereElse(obj,sentence)) {
                        sentence.setVerb("match");
                        newhead = match;
                    } 
                    else newhead = obj.replace(m.group(1),m.group(1) + " matching " + match);
                    if (newhead.contains("ignorecase")) {
                        newhead = newhead.replace("ignorecase","");
                        newhead += " (ignoring case)";
                    }
                    usedFilters.add(f);
                }
                else if (fstring.startsWith(var + " does not exist")) {
                    if (((WordElement) sentence.getVerb()).getBaseForm().equals("be")) {
                        if (!occursAnyWhereElse(obj,sentence)) {
                            sentence.setVerb("do not exist");
                            newhead = "";
                        }
                        else {
                            newhead = "no "+obj;
                        }
                    }
                    else sentence.setFeature("negated",true);
                    usedFilters.add(f);
                }
                else {
                    for (String comp : comparison) {
                        if (fstring.startsWith(var + comp)) {
                            String match = fstring.replace(var+comp,"");
                            newhead = obj.replace(m.group(1),m.group(1) + comp.replace(" is ","") + match);
                            usedFilters.add(f);
                            break;
                        }
                    }
                    if (fstring.split(" ")[0].equals(obj)) {
                        // SUBJ is ?x . ?x V OBJ -> SUBJ V OBJ
                        if (((WordElement) sentence.getVerb()).getBaseForm().equals("be")
                                && !sentence.getFeatureAsBoolean("negated")
                                && fstring.startsWith(obj)
                                && !occursAnyWhereElse(obj,sentence)) {   
                            newhead = fstring.replace(obj,"").trim();
                        }
                        else newhead += " which " + fstring.replace(obj,"");
                        sentence.setVerb(nlg.createNLGElement("")); // verb is contained in object (newhead)
                        usedFilters.add(f);
                    }
                }
            }
            sentence.getObject().setFeature("head",newhead);
        }
        
        // attach filter information to subject
        String subj = realiser.realise(sentence.getSubject()).toString();
        p = Pattern.compile("(^|\\A)(\\?([\\w]*))((?!')||\\z)");
        m = p.matcher(subj);
        if (m.find()) { // i.e. if the subject is a variable at the beginning of the phrase
            String var = m.group(2);
            for (NLGElement f : filter) {
                String fstring = realiser.realiseSentence(f);
                if (fstring.endsWith(".")) fstring = fstring.substring(0,fstring.length()-1);
                if (fstring.startsWith(var+" does not exist")) {
                    usedFilters.add(f);
                    if (inSelects(subj)) sentence.setFeature("negated",true);
                    else sentence.setSubject("no " + realiser.realise(sentence.getSubject()).toString());
                }
                else if (fstring.startsWith(var)) {
                    sentence.addComplement("and " + fstring.replace(var,"").trim());
                    usedFilters.add(f);
                }
            }
        }
        
        filter.removeAll(usedFilters);
    }
    
    private boolean inSelects(String var) {
        for (NPPhraseSpec sel : selects) {
            if (realiser.realise(sel).toString().endsWith(var)) return true;
        }
        return false;
    }
    
    private void fuseWithSelects() {
        
        HashMap<NPPhraseSpec,NPPhraseSpec> replacements = new HashMap<NPPhraseSpec,NPPhraseSpec>();
        Set<SPhraseSpec> delete = new HashSet<SPhraseSpec>();
        String selstring;
        String var;
        int oc;
        for (NPPhraseSpec sel : selects) {
            selstring = realiser.realise(sel).toString();
            var = selstring.substring(selstring.indexOf("?")+1);
            oc = numberOfOccurrences(var);
                        
            if (oc == 0) sel.setFeature("head","");
            else if (oc == 1) {
                SPhraseSpec del = null;
                for (SPhraseSpec s : sentences) {
                    // selects TYPE ?x such that ?x is OBJECT -> selects OBJECT
                    if (((WordElement) s.getVerb()).getBaseForm().equals("be")) {
                        if (realiser.realise(s.getSubject()).toString().equals("?"+var)) {
                            replacements.put(sel,nlg.createNounPhrase(s.getObject()));
                            delete.add(s); break;
                        }
                        else if (realiser.realise(s.getObject()).toString().equals("?"+var)) {
                            replacements.put(sel,nlg.createNounPhrase(s.getSubject()));
                            delete.add(s); break;
                        }
                    }
                }
            }
        }
        for (NPPhraseSpec key : replacements.keySet()) {
            selects.remove(key);
            selects.add(replacements.get(key));
        }
        sentences.removeAll(delete);
    }
    
    private void integrateLabelInfoIntoSelects(Set<NLGElement> bodyparts) {
        Pattern p = Pattern.compile("(\\?([\\w]*)) ((.*)and\\s)?((has)||(may have)) the((\\s[\\w]*)? ((label)||(name))) (\\?[\\w]*)( and(.*))?\\.");
        NLGElement info = null;
        NLGElement rest = null;
        for (NLGElement bodypart : bodyparts) {
            String bstring = realiser.realiseSentence(bodypart);
            Matcher m = p.matcher(bstring);
            if (m.matches() && inSelects(m.group(1)) && inSelects(m.group(13))) {
                boolean labelvarfree = true;
                for (NLGElement b : bodyparts) {
                    if (!b.equals(bodypart) && realiser.realiseSentence(b).contains(m.group(13))) { // TODO "?string2" contains "?string"
                        labelvarfree = false;
                        break;
                    }
                }
                if (labelvarfree) {
                    info = bodypart; 
                    String restrealization = "";
                    if (m.group(4) != null) restrealization += m.group(1)+" "+m.group(4);
                    if (m.group(15) != null) { 
                        if (restrealization.isEmpty()) { 
                            if (m.group(15).trim().startsWith(m.group(1))) restrealization += m.group(15);
                            else restrealization += m.group(1) + m.group(15);
                        }
                        else restrealization += " and "+m.group(15);
                    }                                              
                    if (!restrealization.isEmpty()) rest = nlg.createNLGElement(restrealization);
                    removeFromSelects(m.group(13));
                    for (NPPhraseSpec sel : selects) {
                        if (sel.getFeatureAsString("head").equals(m.group(1))) {
                            String pron = "their";
                            if (sel.hasFeature("premodifiers")) {
                                List<NLGElement> premods = new ArrayList<NLGElement>((Collection) sel.getFeature("premodifiers"));
                                if (!premods.isEmpty() && premods.get(0).hasFeature("number")) {
                                    if (premods.get(0).getFeatureAsString("number").equals("SINGULAR")) pron = "its";
                                }
                            }
                            String postmodifier = "and " + pron + m.group(8);
                            if (m.group(5).equals("may have")) postmodifier += " (if it exists)";
                            sel.addPostModifier(postmodifier);
                        }
                    }
                }
            }
        }
        bodyparts.remove(info);
        if (rest != null) bodyparts.add(rest);
    }
    
    private void removeFromSelects(String var) {
        List<NPPhraseSpec> newselects = new ArrayList<NPPhraseSpec>();
        for (NPPhraseSpec sel : selects) {
            if (!sel.getFeatureAsString("head").equals(var)) newselects.add(sel);
        }
        selects = newselects;
    }
    
    private void fuseWithSelectsAgain(Set<NLGElement> bodyparts) {
        NLGElement bodypart = null;
        if (bodyparts.size() == 1) {
            NLGElement bp = new ArrayList<NLGElement>(bodyparts).get(0);
            String b = realiser.realiseSentence(bp);
            if (!b.contains(" and ") && !b.contains(" or ") && !b.contains(" not ")) {
                Pattern p = Pattern.compile("(\\?[\\w]*) is (.*)\\.");
                Matcher m = p.matcher(b);
                if (m.matches()) {
                    NPPhraseSpec oldspec = null;
                    NPPhraseSpec newspec = null;
                    for (NPPhraseSpec sel : selects) {
                        if (sel.getFeatureAsString("head").equals(m.group(1))) {
                            oldspec = sel;
                            newspec = nlg.createNounPhrase();
                            newspec.setFeature("head",m.group(2));
                            if (oldspec.hasFeature("postmodifiers")) {
                                newspec.addPostModifier(oldspec.getFeatureAsStringList("postmodifiers").get(0).replace("their","its"));
                            }
                        }
                    }
                    selects.remove(oldspec);
                    selects.add(newspec);
                    bodypart = bp;
                }
            }
        }
        if (bodypart != null) bodyparts.remove(bodypart);
    }
    
    public NLGElement returnSelect() {
        if (selects.size() == 1) {
            return selects.get(0);
        } else {
            CoordinatedPhraseElement cpe = nlg.createCoordinatedPhrase(selects.get(0), selects.get(1));
            if (selects.size() > 2) {
                for (int i = 2; i < selects.size(); i++) {
                    cpe.addCoordinate(selects.get(i));
                }
            }
            return cpe;
        } 
    }
    
    private boolean occursAnyWhereElse(String var,SPhraseSpec sent) { 
        // anywhere else than in sent, that is
        if (numberOfOccurrences(var.replace("?","")) > 1) return true; // > 1 because it does also occur in filter
        for (SPhraseSpec s : currentlystored) {
            if (!s.equals(sent) && realiser.realiseSentence(s).contains("?"+var)) 
                return true;
        }
        return false;
    }
    
    private int numberOfOccurrences(String var) {
        int n = 0;
        for (SPhraseSpec s : sentences) {
            if (realiser.realiseSentence(s).contains("?"+var)) n++;
        }
        for (SPhraseSpec s : optionalsentences) {
            if (realiser.realiseSentence(s).contains("?"+var)) n++;
        }
        for (Set<SPhraseSpec> union : unions) {
            for (SPhraseSpec s : union) {
                if (realiser.realiseSentence(s).contains("?"+var)) n++;
            }
        }
        for (Set<SPhraseSpec> union : optionalunions) {
            for (SPhraseSpec s : union) {
                if (realiser.realiseSentence(s).contains("?"+var)) n++;
            }
        }
        for (NLGElement f : filter) {
            if (realiser.realiseSentence(f).contains("?"+var)) n++;
        }
        return n;
    }
    
    private String max(Set<String> vars) {
        String out = null;
        int beatme = 0;
        for (String s : vars) {
            if (numberOfOccurrences(s) >= beatme) {
                out = s;
                beatme = numberOfOccurrences(s);
            }
        }
        return out;
    }
    
    private void cleanUp() { // very stupid Java programming
        
        Set<String> primariesLeft = new HashSet<String>();
        Set<String> secondariesLeft = new HashSet<String>();
        
        for (String var : primaries) {
            cleanUpVar(var,primariesLeft);
        }
        for (String var : secondaries) {
            cleanUpVar(var,secondariesLeft);
        }
        
        primaries = primariesLeft;
        secondaries = secondariesLeft;
    }
    private void cleanUpVar(String var,Set<String> varLeft) {
        boolean found = false;
            for (SPhraseSpec s : sentences) {
                if (realiser.realiseSentence(s).contains(var)) {
                    varLeft.add(var);
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (Set<SPhraseSpec> union : unions) {
                    for (SPhraseSpec s : union) {
                        if (realiser.realiseSentence(s).contains(var)) {
                            varLeft.add(var);
                            break;
                        }
                    }
                }
            }
    }
    
    
    public void print() {
        String maxP = max(primaries);
        String maxS = max(secondaries);
        System.out.print("\nPrimary variables: ");
        for (String s : primaries) {
            if (s.equals(maxP)) System.out.print("!");
            System.out.print(s + "("+numberOfOccurrences(s)+") ");
        }
        System.out.print("\nSecondary variables: ");
        for (String s : secondaries) {
            if (s.equals(maxS)) System.out.print("!");
            System.out.print(s + "("+numberOfOccurrences(s)+") ");
        }
        System.out.println("\nSentences: ");
        for (SPhraseSpec s : sentences) {
            System.out.println(" -- " + realiser.realiseSentence(s));
        }
        System.out.println("Unions: ");
        for (Set<SPhraseSpec> union : unions) {
            System.out.print(" -- ");
            for (SPhraseSpec s : union) {
                System.out.print(realiser.realiseSentence(s) + "\n    ");
            }
        }
        System.out.println("\nOptional sentences: ");
        for (SPhraseSpec s : optionalsentences) {
            System.out.println(" -- " + realiser.realiseSentence(s));
        }
        System.out.println("Optional unions: ");
        for (Set<SPhraseSpec> union : optionalunions) {
            System.out.print(" -- ");
            for (SPhraseSpec s : union) {
                System.out.print(realiser.realiseSentence(s) + "\n    ");
            }
        }
        System.out.println("Filters: ");
        for (NLGElement f : filter ) {
            System.out.print(" -- " + realiser.realiseSentence(f) + "\n");
        }
    }
}