package edu.utexas.cycic;

import java.util.ArrayList;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class RegionCircle extends Rectangle {

	Object name;
	Text text = new Text();
	ArrayList<String> allowedFacilities = new ArrayList<String>();
	ArrayList<String> regionModel = new ArrayList<String>();
	ArrayList<String> containedInstitutions = new ArrayList<String>();
	{
		setId("thisRegion");
	}
}