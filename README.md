# URA_project

### Done:
- Scan under a directory with given code chunks changes. See TextNormalize.java for details.
- Normalize code chunks into two levels, normalize by code chunks and normalize by lines
- Compare code from code chunk changes and java source code.



### TO DO:
- Store the output in a database
- Read from csv, Replace rootPath, fileName, deletion, addition in TextNormalize.java for actual data from csv.
- The current csv file contains "\n" in those code chunks cells, makes it hard to be scanned and parsed by newlines with java.  
Maybe consider parse git commits into a database instead of csv files?


 
#### A sample output for now: 
- This test data is selected from one of the commit in the csv file
- This test file is stored in the directory named "dummy". 
```
File: ViewPaper.java found under root directory: dummy
Path = /Users/xxx/Desktop/URA_project/dummy/dummy2/ViewPaper.java 

Consider Change Code Chunk:
public static class SavedState extends BaseSavedState {
Into:
public static class SavedState extends AbsSavedState {

Consider Change Code Chunk:
public SavedState ( Parcel source ) {super ( source ) ;
Into:

Consider Change Code Chunk:
public static final Creator < SavedState > CREATOR = new Creator < SavedState > ( ) {
Into:
public static final Creator < SavedState > CREATOR = new ClassLoaderCreator < SavedState > ( ) {
@Override public SavedState createFromParcel ( Parcel $V0 , ClassLoader $V1 ) {
return new SavedState ( $V0 , $V1 ) ;
```
