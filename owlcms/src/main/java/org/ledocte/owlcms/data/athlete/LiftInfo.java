package org.ledocte.owlcms.data.athlete;

import org.ledocte.owlcms.data.athlete.LiftDefinition.Changes;
import org.ledocte.owlcms.data.athlete.LiftDefinition.Stage;

public class LiftInfo {

	public Stage getStage() {
		return stage;
	}

	public int getLiftNo() {
		return liftNo;
	}

	public int getChangeNo() {
		return changeNo;
	}

	public Integer getValue() {
		if (stringValue == null) return null;
		return Integer.parseInt(stringValue);
	}

	public String getStringValue() {
		return stringValue;
	}
	
	public String getChange() {
		Changes changes = LiftDefinition.Changes.values()[changeNo];
		return changes.name();
	}

	private Stage stage;
	int liftNo;
	private int changeNo;
	Integer value;
	private String stringValue;

	LiftInfo(Stage stage, int liftNo, int changeNo, String stringValue) {
		 this.stage = stage;
		 this.liftNo = liftNo;
		 this.changeNo = changeNo;
		 this.stringValue = stringValue;
	 }
}