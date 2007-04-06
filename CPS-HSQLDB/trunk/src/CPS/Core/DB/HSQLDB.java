/*
 * HSQLDB.java
 *
 * Created on January 16, 2007, 1:07 PM
 *
 *
 */

package CPS.Core.DB;

import CPS.Data.*;
import CPS.Module.CPSDataModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.hsqldb.*;
import resultsettablemodel.*;

/**
 *
 * @author Clayton
 */
public class HSQLDB extends CPSDataModel {
   
   private Connection con;
   private final String hsqlDriver = "org.hsqldb.jdbcDriver";
   private final String dbDir = System.getProperty("user.dir");
   private final String dbFile = "CPSdb";
   
   private ResultSet rsListCache = null;
   private ResultSet rsCropCache = null;
   public String state = null;
   
   private HSQLQuerier query;
   
   public HSQLDB() {

      con = HSQLConnect.getConnection( dbDir, dbFile, hsqlDriver );
      boolean newDB = false;
      
      if ( con == null ) { // db DNE
         con = HSQLConnect.createDB( dbDir, dbFile );
         newDB = true;
      }
         
      query = new HSQLQuerier( con );

      if ( newDB ) {
         this.importCropsAndVarieties( HSQLDBPopulator.loadDefaultCropList( dbDir )
                                                      .exportCropsAndVarieties() );
      }
      
   }
   
   
   public synchronized ArrayList<String> getListOfCropPlans() {
      
      try {
         Statement st = con.createStatement();
         ResultSet rs = st.executeQuery( "SELECT plan_name FROM CROP_PLANS" );
      
         System.out.println("Executed query: " + "SELECT plan_name FROM CROP_PLANS" );
         
         ArrayList<String> l = new ArrayList<String>();
         while ( rs.next() ) {
            System.out.println("Found table entry: " + (String) rs.getObject(1) );
            l.add( (String) rs.getObject(1) );
         }
      
         return l;
      } 
      catch ( SQLException e ) { 
         e.printStackTrace();
         return new ArrayList<String>();
      }
      
   }

   private String getMandatoryColumnNames() {
      return "id, crop_name";
   }
   private String getAbbreviatedColumnNames( boolean varieties ) {
      return getMandatoryColumnNames() + ", " +
             ( varieties ? "var_name, " : "" ) + "fam_name, maturity";
   }
   
   private String getCropsColumnNames() {
      return "*";
   }
   
   private String getVarietiesColumnNames() {
      return getCropsColumnNames();
   }
   
   
   /** Method to cache results of a query and then return those results as a table */
   private TableModel cachedListTableQuery( String t, String col, String cond, String sort ) {
      rsListCache = query.storeQuery( t, col, cond, sort );
      // return query.getCachedResultsAsTable();
      return query.tableResults( rsListCache );
   }

   /*
    * CROP LIST METHODS
    */
      
   public TableModel getAbbreviatedCropList() {
      return cachedListTableQuery( "CROPS_VARIETIES", 
                                   getAbbreviatedColumnNames( false ),
                                   "var_name IS NULL",
                                   null );
   }
   public TableModel getAbbreviatedCropList( String sortCol ) {
      return cachedListTableQuery( "CROPS_VARIETIES", 
                                   getAbbreviatedColumnNames( false ),
                                   "var_name IS NULL",
                                   sortCol );
   }
   
   public TableModel getCropList() { 
      return cachedListTableQuery( "CROPS_VARIETIES", getCropsColumnNames(), null, null );
   }
   
   public TableModel getCropList( String sortCol ) { 
      return cachedListTableQuery( "CROPS_VARIETIES", getCropsColumnNames(), null, sortCol );
   }   

   public TableModel getVarietyList() {
      return cachedListTableQuery( "CROPS_VARIETIES", getVarietiesColumnNames(), null, null );
   }
   
   public TableModel getVarietyList( String sortCol ) {
      return cachedListTableQuery( "CROPS_VARIETIES", getVarietiesColumnNames(), null, sortCol );
   }

   public TableModel getAbbreviatedVarietyList() {
      return cachedListTableQuery( "CROPS_VARIETIES", 
                                   getAbbreviatedColumnNames( true ),
                                   "var_name IS NOT NULL",
                                   null ); 
   }
   public TableModel getAbbreviatedVarietyList( String sortCol ) {
      return cachedListTableQuery( "CROPS_VARIETIES", 
                                   getAbbreviatedColumnNames( true ),
                                   "var_name IS NOT NULL",
                                   sortCol ); 
   }
   
   public TableModel getCropAndVarietyList() {
      return cachedListTableQuery( "CROPS_VARIETIES", "*", null, null );
   }
   public TableModel getCropAndVarietyList( String sortCol ) {
      return cachedListTableQuery( "CROPS_VARIETIES", "*", null, sortCol );
   }
   
   public TableModel getAbbreviatedCropAndVarietyList() {
      return cachedListTableQuery( "CROPS_VARIETIES", getAbbreviatedColumnNames( true ), null, null );
   }
   public TableModel getAbbreviatedCropAndVarietyList( String sortCol ) {
      return cachedListTableQuery( "CROPS_VARIETIES", getAbbreviatedColumnNames( true ), null, sortCol );
   }

   /*
    * CROP PLAN METHODS
    */
   public void createNewCropPlan( String plan_name ) {
      HSQLDBCreator.createCropPlan( con, plan_name );
   }

   public void retrieveCropPlan(String plan_name) {
   }

   public void filterCropPlan(String plan_name, String filter) {
   }

   /* we make the assumption that we're zero-based, ResultSets are not */
   public CPSCrop getCropInfoForRow( int selectedRow ) {
      try {
         int id;
         // TODO figure out better way to handle result caching
         // we could just make this a string based query (ie getCropInfoForRow
         if ( false && rsListCache != null ) {
            rsListCache.absolute( selectedRow + 1 );
            id = rsListCache.getInt( "id" );
         } else {
            id = selectedRow;
         }
         rsCropCache = query.submitQuery( "CROPS_VARIETIES", "*", "id = " + id );
         return resultSetAsCrop( rsCropCache );
      }
      catch ( SQLException e ) { e.printStackTrace(); }

      return null;
   }
   
   

   private CPSCrop resultSetAsCrop( ResultSet rs ) throws SQLException {
      
      CPSCrop crop = new CPSCrop();
      
      // move to the first (and only) row
      // if there are no rows, return null
      // TODO return the empty crop; they can deal with it
      if ( rs.next() ) {
         try {
            crop.setID( rs.getInt( "ID" ));
            crop.setCropName( rs.getString( "crop_name" ));
            crop.setVarietyName( rs.getString( "var_name" ));

            String sim = rs.getString("similar_to");
            System.out.println("Retrieving crop info for similar crop: " + sim );
            crop.setSimilarCrop( this.getCropInfo( sim ));
         
            crop.setFamilyName( rs.getString( "fam_name" ));
         
            crop.setCropDescription( rs.getString("description") );
         
            crop.setMaturityDays( rs.getInt( "maturity" ));
            crop.setSuccessions( rs.getBoolean("successions") );
            crop.setGroups( rs.getString( "groups" ));
         
            crop.setOtherRequirements( rs.getString( "other_req" ));
            crop.setKeywords( rs.getString( "keywords" ));
            crop.setNotes( rs.getString( "notes" ));
         
         }  catch ( SQLException e ) { e.printStackTrace(); }
      }
      
      return crop;
   }

   public void shutdown() {
      try {
         Statement st = con.createStatement();
         st.execute("SHUTDOWN");
         con.close();
      }
      catch ( SQLException ex ) {
         ex.printStackTrace();
      }
   }

   public void updateCrop( CPSCrop crop ) {
      
      try {
         
         String sql = "UPDATE " + "CROPS_VARIETIES" + " SET ";
         
         Iterator<CropDatum> i = crop.iterator();
         CropDatum c;
         
         while ( i.hasNext() ) {
            c = i.next();
            if ( c != null && c.getDatum() != null ) {
               // System.out.println(" Processing datum: " + c.getColumnName() );
               sql += c.getColumnName() + " = " + escapeValue( c.getDatum() ) + ", ";
            }
         }
         
         sql = sql.substring( 0, sql.lastIndexOf( ", " ));
         
         sql += "WHERE id = " + crop.getID();
         
         System.out.println("Attempting to execute: " + sql );

         
         Statement st = con.createStatement();
         st.executeUpdate( sql );
         st.close();
         
      }
      catch ( SQLException ex ) { ex.printStackTrace(); }
   }

   public void createCrop(CPSCrop crop) {
      try {
         
         String cols = "";
         String vals = "";
         
         Iterator<CropDatum> i = crop.iterator();
         CropDatum c;
         
         while ( i.hasNext() ) {
            c = i.next();
            if ( c != null && c.getDatum() != null ) {
               // System.out.println(" Processing datum: " + c.getColumnName() );
               cols += c.getColumnName() + ", ";
               vals += escapeValue( c.getDatum() ) + ", ";
            }
         }
         
         cols += "similar_to";
         vals += HSQLDBCreator.escapeString( crop.getSimilarCrop().getCropName() );
         
         // cols = cols.substring( 0, cols.lastIndexOf( ", " ));
         // vals = vals.substring( 0, vals.lastIndexOf( ", " ));
         
         //"Fudge","mat_adjust","misc_adjust","seeds_sources","seeds_item_codes","seeds_unit_size"         
         
         String sql = "INSERT INTO CROPS_VARIETIES ( " + cols + " ) VALUES ( " + vals + " )";
         
         System.out.println("Attempting to execute: " + sql );
         
         Statement st = con.createStatement();
         if ( st.executeUpdate( sql ) == -1 )
            System.err.println( "Error creating crop " + crop.getCropName() );
         
         st.close();
         
      }
      catch ( SQLException ex ) { ex.printStackTrace(); }
   }
   
   
   public ArrayList<CPSCrop> exportCropsAndVarieties() { return null; }

   public CPSCrop getCropInfo(String cropName) {
      if ( ! cropName.equalsIgnoreCase("null") || 
           ! cropName.equals("") ) {
         try {
            return resultSetAsCrop( query.submitQuery( "CROPS_VARIETIES",
                                                       "*", 
                                                       "crop_name = " + 
                                                       HSQLDBCreator.escapeString( cropName ) + " AND " +
                                                       "var_name IS NULL " ));
         } catch ( SQLException e ) { e.printStackTrace(); }
      }
      
      return new CPSCrop();
   }

   private String escapeValue( Object o ) {
      // if the datum doesn't exist, use NULL
      if      ( o == null )
         return "NULL";
      // if the datum is a string and is only "", use NULL, else escape it
      else if ( o instanceof String )
         if ( o.equals("") || ((String) o).equalsIgnoreCase( "null" ) )
            return "NULL";
         else
            return HSQLDBCreator.escapeString( (String) o );
      // if the datum is an int whose value is -1, use NULL
      else if ( o instanceof Integer &&
                  ((Integer) o).intValue() == -1 )
         return "NULL";
      else
         return o.toString();
   }
   
}
