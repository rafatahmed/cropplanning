/* CropDBCropList.java - Created: March 14, 2007
 * Copyright (C) 2007, 2008 Clayton Carter
 * 
 * This file is part of the project "Crop Planning Software".  For more
 * information:
 *    website: http://cropplanning.googlecode.com
 *    email:   cropplanning@gmail.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package CPS.Core.CropPlans;

import CPS.Data.CPSCrop;
import CPS.Data.CPSRecord;
import CPS.UI.Modules.CPSMasterView;
import CPS.Module.*;
import CPS.Data.CPSPlanting;
import CPS.UI.Modules.CPSMasterDetailModule;
import CPS.UI.Swing.autocomplete.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;
import javax.swing.table.*;

// package access
class CropPlanList extends CPSMasterView implements ActionListener {
   
    private JLabel lblPlanName;
    private JComboBox cmbxPlanList, cmbxCropList;
    private JButton btnNewPlan;
    
    private ArrayList<String> listOfValidCropPlans, listOfValidCrops;
    
    public CropPlanList( CPSMasterDetailModule mdm ) {
        super(mdm);
        setSortColumn("date_plant");
    }
    
    /** 
     * Retrieve the details (as a Planting) for a given ID 
     * @param id The integer id of the planting to retrieve.
     * @return The CPSPlanting object representing the retrieved record.
     */
    protected CPSPlanting getDetailsForID( int id ) {
       return getDataSource().getPlanting( getDisplayedTableName(), id );
    }

    protected CPSPlanting getDetailsForIDs( ArrayList<Integer> ids ) {
       return getDataSource().getCommonInfoForPlantings( getDisplayedTableName(), ids );
    }
    
   
    public void setDataSource( CPSDataModel dm ) {
        super.setDataSource(dm);
        updateListOfPlans();
        updateListOfCrops();
   }
    
    
    /**
     * Populates the crop name combobox with the list of valid crop names from CropDB.
     */
    protected void updateListOfCrops() {
       if ( ! isDataAvailable() )
          return;
       
       listOfValidCrops = getDataSource().getCropNames();
       Collections.sort( listOfValidCrops, String.CASE_INSENSITIVE_ORDER);
       cmbxCropList.removeAllItems();
       cmbxCropList.addItem("");
       for ( String s : listOfValidCrops )
          cmbxCropList.addItem(s);
       
    }
    
    protected void updateListOfPlans() {
       if ( ! isDataAvailable() )
          return;
       
       String selected = getSelectedPlanName();
       listOfValidCropPlans = getDataSource().getListOfCropPlans();
       cmbxPlanList.removeAllItems();
       for ( String s : listOfValidCropPlans ) {
          // TODO think about this; possibly remove COMMON_PLANTINGS from list returned by
          // getListOfCropPlans()
          if ( s.equalsIgnoreCase("common_plantings") )
             continue;
          cmbxPlanList.addItem(s);
       }
       if ( selected != null && !selected.equals( "" ) )
          cmbxPlanList.setSelectedItem( selected );
       
       dataUpdated();
    }
    
    protected void updateMasterList() {
       super.updateMasterList();

       // find column "crop_name" and set it to use a combobox editor
       for ( int i = 0 ; i < masterTable.getColumnModel().getColumnCount() ; i++ )
          if ( masterTable.getColumnName(i).equalsIgnoreCase("crop_name") )
             break;
       // TODO: continue { (crop_name never found) i = -1 }
       int cropColNum = 0;
       if ( masterTable.getRowCount() > 0 ) // TODO && i != -1
          masterTable.getColumnModel().getColumn(cropColNum)
                     .setCellEditor( new ComboBoxCellEditor( cmbxCropList ) );
        
    }
    protected TableModel getMasterListData() {
        if ( !isDataAvailable() )
            return new DefaultTableModel();
         
        String selectedPlan = getSelectedPlanName();
        System.out.println( "Selected plan is: " + selectedPlan );
       
        if ( selectedPlan != null && listOfValidCropPlans.contains( selectedPlan ) )
            return getDataSource().getCropPlan( selectedPlan, getDisplayedColumnList(), getSortColumn(), getFilterString() );
//            return getDataSource().getCropPlan( selectedPlan, getSortColumn(), getFilterString() );
       else
          // TODO error checking fall through to following call when invalid plan is selected
          return new DefaultTableModel();
       
    }
    
    protected String getDisplayedTableName() { return (String) cmbxPlanList.getSelectedItem(); }
    String getSelectedPlanName() { return getDisplayedTableName(); }
    
    protected void buildAboveListPanel() {
        initAboveListPanel();
                
        lblPlanName = new JLabel( "Plan Name:");
        btnNewPlan = new JButton( "New Plan" );
        btnNewPlan.setActionCommand( "NewPlan" );
        btnNewPlan.setMargin( new Insets( 1, 1, 1, 1 ) );
        btnNewPlan.addActionListener( this );
        cmbxPlanList = new JComboBox();
        cmbxPlanList.setEditable( true );
        cmbxPlanList.addActionListener( new CropPlanBoxActionListener() );
       
        jplAboveList.add( lblPlanName );
        jplAboveList.add( cmbxPlanList );
        jplAboveList.add( btnNewPlan );
        
        // false ==> do not initialize panel
        super.buildAboveListPanel(false);
    }
    
    protected void buildListPanel() {
       super.buildListPanel();
       
       cmbxCropList = new JComboBox();
       cmbxCropList.addActionListener( new CropBoxInTableActionListener() );
       AutoCompleteDecorator.decorate( cmbxCropList );
       
    }
    
    public void actionPerformed(ActionEvent actionEvent) {
        String action = actionEvent.getActionCommand();

        System.out.println("DEBUG Action performed in CropPlanList: " + action);

       if ( action.equalsIgnoreCase( btnNewPlan.getActionCommand() ) ) {
            if ( ! isDataAvailable() ) {
                System.err.println("ERROR: cannot create new plan, no data available" );
                return;
            }
            createNewCropPlan( getSelectedPlanName() );
        }
        else
            super.actionPerformed( actionEvent );    
        
    }
    
    public class CropPlanBoxActionListener implements ActionListener {
      public void actionPerformed( ActionEvent actionEvent ) {
         String action = actionEvent.getActionCommand();

         System.out.println( "DEBUG Action performed in CropPlanList: " + action );

         if ( action.equalsIgnoreCase( "comboBoxEdited" ) ) {
         // What do we do here? create a new plan?
         }
         else if ( action.equalsIgnoreCase( "comboBoxChanged" ) ) {
            updateMasterList();
         }
       }   
    }
    public class CropBoxInTableActionListener implements ActionListener {
      public void actionPerformed( ActionEvent actionEvent ) {
         String action = actionEvent.getActionCommand();

         System.out.println( "DEBUG Action registered in CropBoxInTable: " + action );

         if ( action.equalsIgnoreCase( "comboBoxEdited" ) ) {
         // What do we do here? create a new crop?
         }
         else if ( action.equalsIgnoreCase( "comboBoxChanged" ) ) {
            String s = (String) cmbxCropList.getSelectedItem();
            // update mesh this planting w/ the crop selected, but for now just ...
            System.out.println( "Crop name: selected " + s );
            if ( listOfValidCrops.contains( s ) ) {
               CPSCrop crop = getDataSource().getCropInfo( (String) cmbxCropList.getSelectedItem() );
               System.out.println( "Retrieved info for crop " + crop.getCropName() );
            }
            else {
               // TODO handle selection of non-existing crop
               System.err.println("ERROR: Selected non existent crop, what to do?  Create?  For now, nothing." );
            }
         }
       }   
    }
    
    public void createNewCropPlan( String newPlanName ) {
        if ( newPlanName.equalsIgnoreCase( "" ) )
            System.err.println( "Cannot create crop plan with no name" );
        getDataSource().createCropPlan( newPlanName );
        updateListOfPlans();
    }
    
    @Override
    public CPSPlanting createNewRecord() {
       if ( getSelectedPlanName().equals("") ) {
          System.err.println("ERROR cannot create record unless a crop plan is selected");
          return null;
       }
       return getDataSource().createPlanting( getSelectedPlanName(), new CPSPlanting() );
    }
    
    @Override
    public CPSRecord duplicateRecord( int id ) {
        return getDataSource().createPlanting( getSelectedPlanName(),
                                         getDataSource().getPlanting( getSelectedPlanName(),
                                                                id ) );     
    }
    
    @Override
    public void deleteRecord( int id ) {
        getDataSource().deletePlanting( getSelectedPlanName(), id );
    }
    
   @Override
   protected ArrayList<String> getDisplayableColumnList() {
      return getDataSource().getPlantingDisplayableColumns();
   }
   
   @Override
   protected ArrayList<String> getDefaultDisplayableColumnList() {
       return getDataSource().getPlantingDefaultColumns();
   }
   
   protected ArrayList<String[]> getColumnPrettyNameMap() {
       return getDataSource().getPlantingPrettyNames();
   }

    @Override
    protected String getTableStatisticsString() {
        if ( ! isDataAvailable() || getSelectedPlanName() == null )
            return "";
        
       CPSPlanting p = getDataSource().getSumsForCropPlan( getSelectedPlanName(),
                                                           getFilterString() );
       String s = "";
       s += "Plantings:" + masterTable.getRowCount() + "/";
       s += "Beds:" + p.getBedsToPlantString() + "/";
       s += "RowFeet:" + p.getRowFtToPlantString() + "/";
//       s += "Plants:" + p.getPlantsNeededString() + " - ";
       s += "Flats:" + p.getFlatsNeededString();
       
       return s;
    }
    
    @Override
    public void dataUpdated() {
        super.dataUpdated();
        
        if ( getSelectedPlanName() == null ) {
            setStatus( "No plan selected.  Select a plan to display or use \"New Plan\" button to create a new one." );
        }
        
        
    }
}
