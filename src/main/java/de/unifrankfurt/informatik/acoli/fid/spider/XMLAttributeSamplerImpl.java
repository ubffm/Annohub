package de.unifrankfurt.informatik.acoli.fid.spider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.spider.XmlAttributeSamplerI;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.xml.Template;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateGuesser;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateXMLConverter;
import de.unifrankfurt.informatik.acoli.fid.xml.Utils;
import de.unifrankfurt.informatik.acoli.fid.xml.XMLSampler;


public class XMLAttributeSamplerImpl implements XmlAttributeSamplerI {


    HashMap<String, HashMap<String, Long>> attr2Lit = new HashMap<>();
    HashMap<String, HashMap<String, Long>> attr2Uri = new HashMap<>();
    File xmlFile;
    private final static Logger LOGGER =
            Logger.getLogger(XMLAttributeSamplerImpl.class.getName());

    @Override
    public void sample(ResourceInfo resourceInfo, int attributeValues) {
        this.xmlFile = resourceInfo.getFileInfo().getResourceFile();
        XMLSampler xs = new XMLSampler(true);
        xs.sample(this.xmlFile, attributeValues, this.attr2Lit, this.attr2Uri, attributeValues*1000);
    }

    @Override
    public boolean makeConll(ResourceInfo resourceInfo, GWriter writer, HashSet<String> allowedAttributes, int resultingNoSentences) {
        Template guessedTemplate;
        if (allowedAttributes.isEmpty()) {
            LOGGER.info("No allowed attributes, no Pseudo-CoNLL to produce.");
            return false;
        }
        //LOGGER.info("Making pseudo conll for file "+this.xmlFile.getAbsolutePath());
        LOGGER.info(allowedAttributes.size()+" allowed attributes.");
        try {

            HashMap<String, ArrayList<HashMap<String, String>>> overview = XMLSampler.collectOverview(XMLSampler.cutSample(this.xmlFile, 5000), true);
            HashMap<String, HashMap<String, Integer>> attributeFrequencies = XMLSampler.overview2AttributeFrequencies(overview);
            HashMap<String, HashMap<String, Integer>> prunedAttributeFrequencies = XMLSampler.pruneAttributeFrequencies(attributeFrequencies, allowedAttributes);
            guessedTemplate = TemplateGuesser.guessTemplateFromAttributeFrequencies(prunedAttributeFrequencies);
            LOGGER.info("Guessed Template: "+guessedTemplate);

        } catch (XMLStreamException e) {
            LOGGER.severe("Couldn't resample from conll for template guessing");
            return false;
        }

        File conllFile = new File(resourceInfo.getFileInfo().getResourceFile().getAbsolutePath() + ".conll");

        TemplateXMLConverter txc = new TemplateXMLConverter(new ArrayList<>(Arrays.asList(guessedTemplate)));

        try {
            txc.getFullCoNLL(this.xmlFile, Utils.convertFileToPrintStream(conllFile), guessedTemplate, false);
        } catch (FileNotFoundException e) {
            LOGGER.severe("Couldn't write conll.");
            return false;
        }
        resourceInfo.getFileInfo().setTemporaryFilePath(conllFile.getAbsolutePath());
        HashMap<Integer, String> columnMapping = generateColumnMapping(new ArrayList<>(guessedTemplate.columnPaths.keySet()));
        resourceInfo.getFileInfo().setConllcolumn2XMLAttr(columnMapping);
        return true;

    }
        public HashMap<Integer, String> generateColumnMapping(ArrayList<String> attributes){
            HashMap<Integer, String> mapping = new HashMap<>();
            mapping.put(0, "id");
            for (int i = 0;i<attributes.size(); i++) {
                mapping.put(i+1, attributes.get(i));
            }
            return mapping;
        }

    @Override
    public HashMap<String, HashMap<String, Long>> getAttributes2LitObjects() {
        return this.attr2Lit;
    }

    @Override
    public HashMap<String, HashMap<String, Long>> getAttributes2URIObjects() {
        return this.attr2Uri;
    }
}
