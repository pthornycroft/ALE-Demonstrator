package com.arubanetworks.aledemonstrator;

public class SurveyObject {

		PositionHistoryObject pho;
		String action = "add";
		boolean success = false;
		int satisfactory = 0;
		
		public SurveyObject(PositionHistoryObject pho, String action, boolean success, int satisfactory){
			this.pho = pho;
			this.action = action;
			this.success = success;
			this.satisfactory = satisfactory;
		}

}
