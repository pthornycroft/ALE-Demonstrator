package com.arubanetworks.aledemonstrator;

public class SurveyObject {

		PositionHistoryObject pho;
		String action = "add";
		boolean success = false;
		
		public SurveyObject(PositionHistoryObject pho, String action, boolean success){
			this.pho = pho;
			this.action = action;
			this.success = success;
		}

}
