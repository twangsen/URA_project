import token.Token;
import yoshikihigo.commentremover.CRConfig;
import yoshikihigo.commentremover.CommentRemover;
import yoshikihigo.commentremover.CommentRemoverJC;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TextNormalize {

    public static void main(String[] args) {

        //Comments remover settings, using prof.Yoshiki's comments removers
        final String[] args_remover = {"-q","-blankline","retain","-bracketline","retain","-indent","retain"};
        final CRConfig config = CRConfig.initialize(args_remover);
        final CommentRemover remover = new CommentRemoverJC(config);
        final LineLexer lexer = new JavaLineLexer();

        //TODO file path : check for all files or only file with matched file names?
        String rootPath = "dummy";
        //Filename to search
        String fileName = "ViewPaper.java";

        //List of all files
       Map<String,String> allFiles  = fileList(rootPath);

       //TODO: if file not found
       String filepath = allFiles.get(fileName);
       String content;


        //TODO: Some Dummy Test Data
        ArrayList<String> deletion = new ArrayList<>();
        ArrayList<String> addition = new ArrayList<>();

        String deletion1 = "public static class SavedState extends BaseSavedState {";
        String addition1=  "public static class SavedState extends AbsSavedState {";
        deletion.add(remover.perform(deletion1));
        addition.add(remover.perform(addition1));

        String deletion2 = "        public SavedState(Parcel source) {\n" +
                "            super(source);\n" +
                "        }\n";
        String addition2 = "";
        deletion.add(remover.perform(deletion2));
        addition.add(remover.perform(addition2));

        String deletion3 =  "public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {";
        String addition3 ="        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {\n" +
                "@Override\n" +
                "public SavedState createFromParcel(Parcel in, ClassLoader loader) {\n" +
                "return new SavedState(in, loader);\n" + "}";
        deletion.add(remover.perform(deletion3));
        addition.add(remover.perform(addition3));

        String deletion4 = "return new SavedState(in);";
        String addition4 = "return new SavedState(in, null);";
        deletion.add(remover.perform(deletion4));
        addition.add(remover.perform(addition4));

        String deletion5 = "super(in);";
        String addition5 = "super(in, loader);";
        deletion.add(remover.perform(deletion5));
        addition.add(remover.perform(addition5));


        //Tokenize & normalize by code chunks/lines
        List<List<Token>> deletionTokens = tokenize(deletion);
        List<List<Token>> additionTokens = tokenize(addition);
        List<List<Statement>> additionByChunk = allChunksStatements(additionTokens);
        List<List<Statement>> additionByLine = allLinesStatements(additionTokens);
        List<List<Statement>> deletionByChunk = allChunksStatements(deletionTokens);
        List<List<Statement>> deletionByLine = allLinesStatements(deletionTokens);


        try {
            content = new String ( Files.readAllBytes( Paths.get(filepath) ) );
            content = remover.perform(content);
           // List<String> contentList = new ArrayList<String>(Arrays.asList(content.split("\n")));
            //System.out.println(contentList.size());
            final List<Token> contentTokens = lexer.lexFile(content);
           // System.out.println(contentTokens.size());

            List<Statement> contentStatement =  Statement.getJCStatements(contentTokens,false);

            List<Integer> startPoint = new ArrayList<>();

            for (List<Statement> line: deletionByLine) {
                startPoint.add(possibleChunks(line,contentStatement));
            }



            if(checkChunks(deletionByChunk, contentStatement, startPoint)){
                System.out.println("File: " + fileName + "found under root directory: " + rootPath + "\nPath = " + filepath + " \n");
                for (int i = 0; i <  deletionByChunk.size(); i++) {
                    System.out.println("Consider Change Code Chunk:" );
                    System.out.println(getRText(deletionByChunk.get(i)));
                    System.out.println("Into:");
                    printByLine(additionByChunk.get(i));
                    System.out.println("");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private static int possibleChunks(List<Statement> lines , List<Statement> file){
        int size = lines.size();
        for(int i = 0; i < file.size() - size; i++){
            if(lines.equals(file.subList(i,i+size))){
                return i;
            }
        }
        return -1;
    }

    private static boolean checkChunks(List<List<Statement>> allChunks, List<Statement> file, List<Integer> startPoint){
        final LineLexer lexer = new JavaLineLexer();
        for(int i = 0; i < allChunks.size();i++){
            int start = startPoint.get(i);
            int size = allChunks.get(i).size();

            String rText =  getRText(file.subList(start, start+ size));
            List<Statement> s = Statement.getJCStatements(lexer.lexFile(rText), true);
            //System.out.println(s.equals(allChunks.get(i)));
            if(!s.equals(allChunks.get(i))){
                return false;
            }

        }
        return true;
    }

    private static String getRText(List<Statement> statements){
        String rText = "";
        for(Statement statement: statements){
            rText = rText + statement.rText ;
        }
        //System.out.println(rText);
        //System.out.println(rText);
        return rText;
    }

    private static void printByLine(List<Statement> statements){
        for(Statement statement: statements){
           System.out.println(statement);
        }
    }

    private static List<List<Token>> tokenize(List<String> chunks){
        final LineLexer lexer = new JavaLineLexer();
        List<List<Token>> allTokens = new ArrayList<>();
        for (String chunk: chunks) {
            allTokens.add(lexer.lexFile(chunk));
        }
        return allTokens;
    }



    private static List<List<Statement>> allChunksStatements(List<List<Token>> allTokens){
        List<List<Statement>> allStatements = new ArrayList<>();
        for (List<Token> tokens: allTokens) {
            allStatements.add(Statement.getJCStatements(tokens, true));
        }
        return allStatements;
    }

    private static List<List<Statement>> allLinesStatements(List<List<Token>> allTokens){
        List<List<Statement>> allStatements = new ArrayList<>();
        for (List<Token> tokens: allTokens) {
            allStatements.add(Statement.getJCStatements(tokens, false));
        }
        return allStatements;
    }


    public static Map<String, String> fileList (String rootPath) {

        File root = new File(rootPath);

        Map<String, String> resultMap = new HashMap<>();

        File[] files = root.listFiles();

        assert files != null;
        for (File f : files) {
            if (f.isFile()) {
                resultMap.put(f.getName(),f.getAbsolutePath());
            } else if (f.isDirectory()) {
                resultMap.putAll(fileList(f.getAbsolutePath()));
            }
        }
        return resultMap;
    }



}
