/**
 * SortBasedOnName is a class that is used to compare lists of SCDefinition based on their name
 */

package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import servlets.OntologyQueryServlet;

/**
 * TranslationUtils is a class that is used to support the translation of results functionality 
 * 
 */

public class TranslationUtils {

    //public String translations_path = "C:\\Users\\hrysakis\\WORK\\3D-SYSTEK\\Developing\\3DS\\src\\lang\\gr.txt";
    public String default_translations_path = OntologyQueryServlet.contextPath + "lang/en.txt";
    private static Hashtable<String, String> translationsHashtable = new Hashtable<>();

    /**
    *Default constructor for TranslationUtils object
    *@param path the file path where the translations are located 
    *@throws java.io.IOException when path not found
    */
    public TranslationUtils(String path, boolean exactMatch) throws IOException {
        if(path != null){ //if null don't use translations
            if(path.equals("")){
            path = default_translations_path;
            }
            readTranslationFile(path);
        }
    }

    /**
    *This method parses the translation file
    *@param translations_path the file path where the translations are located 
    *@throws java.io.IOException when path not found
    */
    public void readTranslationFile(String translations_path) throws IOException {
        //System.out.println("READ TRANSLATIONS......." + translations_path);
        File fileDirs = new File(translations_path);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(fileDirs), "UTF-8"));

        String line = "";
        String word = "";
        String tranlation = "";
        int delim = 0;
        while ((line = in.readLine()) != null) {
            //System.out.println(line);
            //all_lines = all_lines + line;
            if (line != null) {
                delim = line.indexOf("=");
                if (delim > 0) {
                    word = line.substring(0, delim).trim();
                    tranlation = line.substring(delim + 1).trim();
                    translationsHashtable.put(word, tranlation);
                }
            }
        }
                System.out.println("Translations=================>" +translationsHashtable.toString());
		in.close();
        //return all_lines;
    }

   
    /*
    *This method returns the translated text of a given word. It is called on exact match translation.
    *@param word the specified word
    *@return the translation of the given word
    */
    public String getTranslation(String word) {
         String translation = null;
         //System.out.println("getTranslation for:"+word);
         translation = translationsHashtable.get(word);
          //System.out.println("Translation was:" +translation);
         if (translation == null){
            translation = word;
         }
         //System.out.println("Translation IS:" +translation);
        return translation;
    }
    
    /**
     * This method returns a translated HTMl string performing partial substitution according to
     * the specified tranlations file.
     * @param source_html the source html string
     * @return a translated HTMl string performing partial substitution
     */
    public String getTranslatedHTML(String source_html){
     //String html_str = html.replaceAll("http://www.ics.forth.gr/Ontology/IdeaGarden/SSIS/", "SSIS:");
     
     Enumeration<String> enkeys = translationsHashtable.keys();
         String termInFile ="";
         String translation = "";
         
         while (enkeys.hasMoreElements()){
           termInFile = enkeys.nextElement();
           translation = translationsHashtable.get(termInFile); 
           source_html = source_html.replaceAll(termInFile, translation);
           
         }
     return source_html;
    }
}
