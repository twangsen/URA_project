# URA_project

- A sample output would be in the form of 
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
