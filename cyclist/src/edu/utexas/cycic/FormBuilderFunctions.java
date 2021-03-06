package edu.utexas.cycic;

import java.util.ArrayList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

/**
 * This class is built to handle all of the functions used in building the forms for CYCIC. 
 * @author Robert
 *
 */
public class FormBuilderFunctions {
	/**
	 * This method is used to read and create the forms associated with each facility. 
	 * @param facArray ArrayList<Object> that contains the facility prototype information structure for the facility opened with this view. 
	 * @param dataArray ArrayList<Object> containing all of the data associated with the facility opened with this view.
	 */
	@SuppressWarnings("unchecked")
	public static void formArrayBuilder(ArrayList<Object> facArray, ArrayList<Object> dataArray){
		if(facArray.size() == 0){
			return;
		}
		Boolean test = true;
		String defaultValue = "";
		for (int i = 0; i < facArray.size(); i++){
			if (facArray.get(i) instanceof ArrayList){
				test = false;
				dataArray.add(new ArrayList<Object>());
				formArrayBuilder((ArrayList<Object>)facArray.get(i), (ArrayList<Object>)dataArray.get(dataArray.size()-1));
			} else if (i == 0){
				defaultValue = (String) facArray.get(5);
			}
		}
		if (test == true) {
			if(facArray.get(5) != null){
				dataArray.add(defaultValue);
			} else {
				dataArray.add("");
			}
		}
	}
	
	/**
	 * A function used to copy the internal structure of an ArrayList<Object>.
	 * @param baseList The ArrayList<Object> the new copy list will be added to.
	 * @param copyList The ArrayList<Object> being copied to the baseList.
	 */
	@SuppressWarnings("unchecked")
	public static void arrayListCopy(ArrayList<Object> baseList, ArrayList<Object> copyList){
		ArrayList<Object> addedArray = new ArrayList<Object>();
		for(int i = 0; i < copyList.size(); i++){
			if(copyList.get(i) instanceof ArrayList){
				arrayListCopy(addedArray, (ArrayList<Object>)copyList.get(i));
			} else {
				addedArray.add(copyList.get(i));
			}
		}
		baseList.add(addedArray);
	}
	
	/**
	 * 
	 * @param facArray
	 * @param defaultValue
	 * @return
	 */
	static TextField textFieldBuilder(final ArrayList<Object> facArray, final ArrayList<Object> defaultValue){
		
		TextField textField = new TextField();
		textField.setText(defaultValue.get(0).toString());
		if(facArray.get(1) != null){
			textField.setPromptText(facArray.get(1).toString());
		}
		textField.textProperty().addListener(new ChangeListener<Object>(){         
			public void changed(ObservableValue<?> observable, Object oldValue, Object newValue){
				defaultValue.set(0, newValue);
			}
		});
		
		return textField;
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	static TextField nameFieldBuilder(final facilityNode node){
		TextField textField = new TextField();
		textField.setText((String)node.name);
		
		textField.textProperty().addListener(new ChangeListener<String>(){         
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				node.name = newValue;
				node.cycicCircle.text.setText(newValue);
				node.sorterCircle.text.setText(newValue);

				node.cycicCircle.tooltip.setText(newValue);
				node.sorterCircle.tooltip.setText(newValue);
			}
		});
		return textField;
	}
	

	/**
	 * Function used to create a TextField for the naming of a regionNode.
	 * @param node regionNode associated with the form that calls this function.
	 * @param dataArray ArrayList<Object> that contains the name field for this regionNode.
	 * @return TextField linked to the name of a regionNode.
	 */

	static TextField regionNameBuilder(final regionNode node){
		TextField textField = new TextField();
		RegionCorralView.workingRegion = node;
		textField.setText((String) node.name);
		
		textField.textProperty().addListener(new ChangeListener<String>(){         
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				node.name = (String) newValue;

				regionNode.regionCircle.name.equals(newValue);
				regionNode.regionCircle.text.setText(newValue);
			}
		});
		return textField;
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	static TextField institNameBuilder(final instituteNode node){
		TextField textName = new TextField();
		textName.setText(node.name);
		
		textName.textProperty().addListener(new ChangeListener<String>(){         
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				node.name = newValue;
			}
		});
		return textName;
	}
	/**
	 * This function builds the slider for continuous ranges given a minimum and maximum. 
	 * @param string The string containing a range in the following format "min...max"
	 * @param defaultValue This is the value that the slider should be set to upon being initiated. 
	 * @return Returns the slider created by the function.
	 */
	static Slider sliderBuilder(String string, String defaultValue){
		
		final Slider slider = new Slider();
		
		slider.setMin(Double.parseDouble(string.split("[...]")[0]));
		slider.setMax(Double.parseDouble(string.split("[...]")[3]));
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);
		slider.setMajorTickUnit(slider.getMax()/5);
		
		slider.setValue(Double.parseDouble(defaultValue));
		
		return slider;
	}
	
	/**
	 * A function to create a text field used to visualize a given slider. 
	 * @param slider The slider that the text field is used to describe.
	 * @param defaultValue The current value of the slider passed to the function. 
	 * @return A text field used to interact with the slider. 
	 */
	static TextField sliderTextFieldBuilder(final Slider slider, final ArrayList<Object> defaultValue){
		
		final TextField textField = new TextField();
		
		textField.setText((String) defaultValue.get(0));
		
		textField.setText(String.format("%.2f", slider.getValue()));
		textField.setOnAction(new EventHandler<ActionEvent>(){
			public void handle(ActionEvent action){
				if(Double.parseDouble(textField.getText()) <= slider.getMax() && Double.parseDouble(textField.getText()) >= slider.getMin()){
					slider.setValue(Double.parseDouble(textField.getText()));
				}
			}
		});
		
		textField.textProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				if (slider.getMax() < 9000 && Double.parseDouble(newValue) > 9000){
					textField.setText("IT'S OVER 9000!!!!!");
				} else {
					defaultValue.set(0, textField.getText());
				}
			}
		});
		
		slider.valueProperty().addListener(new ChangeListener<Number>(){
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				textField.setText(String.format("%.2f", new_val));
				defaultValue.set(0, textField.getText());
			}
		});
		return textField;
	}
	
	/**
	 * Creates a ComboBox used to build the drop down menus for discrete value ranges or lists.
	 * @param string The string containing the discrete values in the form of "v1,v2,v3,v4,v5,...,vN"
	 * @param defaultValue ArrayList<Object> that contains the default or current value of the comboBox being initiated. 
	 * @return Returns a ComboBox to be used in the creation of the form. 
	 */
	static ComboBox<String> comboBoxBuilder(String string, final ArrayList<Object> defaultValue){
		
		final ComboBox<String> cb = new ComboBox<String>();
		
		for(String value : string.split(",")){
			cb.getItems().add(value.trim());
		}
		cb.setPromptText("Select value");
		cb.setValue(defaultValue.get(0).toString());
		
		cb.valueProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				defaultValue.set(0, cb.getValue());
			}
		});
		return cb;
	}
	
	/**
	 * This function applies the units for the forms. 
	 * @param units A string that contains the unit "type". Ex. "kg", "MW", etc. 
	 * @return returns a label with this string
	 */
	static Label unitsBuilder(String units){
		Label unitsLabel = new Label();
		
		unitsLabel.setText(units);
		
		return unitsLabel; 
	}

	/**
	 * This function is used to instruct the CYCIC pane that a new link must be added to represent the linking of a facility to a market. 
	 * In this case material is flowing into the facility. This function is only used for the "incommodity" of a facilityCircle.
	 * @param facNode The facilityCircle that the current form is being used to represent. 
	 * @param defaultValue The ArrayList<Object> that contains the default value of this input field for the facilityCircle.
	 * @return ComboBox containing all of the commodities currently linked to markets, with the value shown being the current incommodity for the facNode.
	 */
	static ComboBox<String> comboBoxInCommod(final facilityNode facNode, final ArrayList<Object> defaultValue){
		// Create and fill the comboBox
		final ComboBox<String> cb = new ComboBox<String>();
		cb.setMinWidth(80);
		cb.setOnMousePressed(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent e){
				cb.getItems().clear();

				for (CommodityNode label: DataArrays.CommoditiesList){
					cb.getItems().add(label.name.getText());
				}
				//cb.getItems().add("New Commodity");
				
				if ( defaultValue.get(0) != "") {
					cb.setValue((String) defaultValue.get(0));
				}
			}
		});
		if ( defaultValue.get(0) != "") {
			cb.setValue((String) defaultValue.get(0));
		}
		cb.setPromptText("Select a commodity");
		cb.valueProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){

				if (newValue == "New Commodity"){
					// Tell Commodity Window to add a new commodity 
				} else {
					facNode.cycicCircle.incommods.add(newValue);
					defaultValue.set(0, newValue);
					for (int i = 0; i < facNode.cycicCircle.incommods.size(); i++) {
						if (facNode.cycicCircle.incommods.get(i) == (String) oldValue){
							facNode.cycicCircle.incommods.remove(i);
							i--;
						}
					}
					VisFunctions.marketHide();
				}
			}
		});

		return cb;
	}
	
	/**
	 * This function is used to instruct the CYCIC pane that a new link must be added to represent the linking of a facility to a market. 
	 * In this case material is flowing out of the facility. This function is only used for the "outcommodity" of a facilityCircle.
	 * @param facNode The facilityCircle that the current form is being used to represent. 
	 * @param defaultValue The ArrayList<Object> that contains the default value of this input field for the facilityCircle.
	 * @return ComboBox containing all of the commodities currently linked to markets, with the value shown being the current outcommodity for the facNode.
	 */
	static ComboBox<String> comboBoxOutCommod(final facilityNode facNode, final ArrayList<Object> defaultValue){
		// Create and fill the comboBox
		final ComboBox<String> cb = new ComboBox<String>();
		cb.setMinWidth(80);
				
		cb.setOnMousePressed(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent e){
				cb.getItems().clear();
				for (CommodityNode label: DataArrays.CommoditiesList){
					cb.getItems().add(label.name.getText());
				}
				//cb.getItems().add("New Commodity");
				
				if (defaultValue.get(0) != "") {
					cb.setValue((String) defaultValue.get(0));
				}
			}
		});
		if (defaultValue.get(0) != "") {
			cb.setValue((String) defaultValue.get(0));
		}
		cb.setPromptText("Select a commodity");
		cb.valueProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				if (newValue == "New Commodity"){
					// Tell Commodity Window to add a new commodity 
				} else {
					facNode.cycicCircle.outcommods.add(newValue);
					defaultValue.set(0, newValue);
					for (int i = 0; i < facNode.cycicCircle.outcommods.size(); i++) {
						if (facNode.cycicCircle.outcommods.get(i) == (String) oldValue){
							facNode.cycicCircle.outcommods.remove(i);
							i--;
						}
					}
					VisFunctions.marketHide();
				}
			}
		});

		return cb;
	}
	
	/**
	 * Function used to link a recipe to a facility. 
	 * @param facNode facilityCircle that was used to construct the form. 
	 * @param defaultValue ArrayList<Object> containing the data for a "recipe" field in the facilityCircle. 
	 * @return ComboBox containing all of the recipes currently available in the simulation, tied to the value of this recipe field. 
	 */
	static ComboBox<String> recipeComboBox(facilityNode facNode, final ArrayList<Object> defaultValue){
		final ComboBox<String> cb = new ComboBox<String>();
		
		cb.setValue((String) defaultValue.get(0));
		
		cb.setOnMousePressed(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent e){
				cb.getItems().clear();
				for (Nrecipe recipe: DataArrays.Recipes) {
					cb.getItems().add(recipe.Name);
				}
			}
		});
		
		cb.valueProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				defaultValue.set(0, newValue);
			}
		});
		
		
		return cb;
	}
	
	/**
	 * Creates a ComboBox that contains the list of commodities in the current working scenario.
	 * @param defaultValue ArrayList of containing the default value of this field.
	 * @return ComboBox containing the list of commodities in the scenario.
	 */
	static ComboBox<String> comboBoxCommod(final ArrayList<Object> defaultValue){
		// Create and fill the comboBox
		final ComboBox<String> cb = new ComboBox<String>();
		cb.setMinWidth(80);
		cb.setPromptText("Select a commodity");
		cb.setOnMousePressed(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent e){
				cb.getItems().clear();
				for (CommodityNode label: DataArrays.CommoditiesList){
					cb.getItems().add(label.name.getText());
				}
				//cb.getItems().add("New Commodity");
			}
		});
		
		cb.valueProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				defaultValue.set(0, newValue);
			}
		});
		return cb;
	}
	
	static ComboBox<String> comboBoxFac(final ArrayList<Object> defaultValue){
		final ComboBox<String> cb = new ComboBox<String>();
		cb.setMinWidth(80);
		cb.setPromptText("Select a Facility");
		cb.setValue((String) defaultValue.get(0));
		cb.setOnMousePressed(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent e){
				cb.getItems().clear();
				for (facilityNode facility: DataArrays.FacilityNodes){
					cb.getItems().add((String) facility.name);
				}
			}
		});
		
		cb.valueProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				defaultValue.set(0, newValue);
			}
		});
		return cb;
	}
	
	/**
	 * 
	 * @param grid
	 * @param formNode
	 * @param facArray
	 * @param dataArray
	 * @param col
	 * @param row
	 * @return
	 */
	static GridPane cycicTypeTest(GridPane grid, facilityNode formNode, ArrayList<Object> facArray, ArrayList<Object> dataArray, int col, int row){
		switch ((String) facArray.get(2).toString().toLowerCase()) {
		case "incommodity":
			grid.add(FormBuilderFunctions.comboBoxInCommod(formNode, dataArray), 1+col, row);
			break;
		case "outcommodity":
			grid.add(FormBuilderFunctions.comboBoxOutCommod(formNode, dataArray), 1+col, row);
			break;
		case "inrecipe": case "outrecipe": case "recipe":
			grid.add(FormBuilderFunctions.recipeComboBox(formNode, dataArray), 1+col, row);
			break;
		case "commodity":
			grid.add(FormBuilderFunctions.comboBoxCommod(dataArray), 1+col, row);
			break;
		case "prototype":
			grid.add(comboBoxFac(dataArray), 1+col, row);
			break;
		default:
			grid.add(FormBuilderFunctions.textFieldBuilder(facArray, (ArrayList<Object>)dataArray), 1+col, row);
			break;
		}
		return grid;
	}
}
