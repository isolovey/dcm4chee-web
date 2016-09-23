package org.dcm4chee.web.war.tc;



public class TCPatientAgeUtilities {

	public static String format(Integer ageInDays) {
		if (ageInDays!=null)
		{
			Integer years = toYears(ageInDays);
			Integer months = toRemainingMonths(ageInDays);
			
			StringBuilder sbuilder = new StringBuilder();
			if (years!=null && years>0) {
				sbuilder.append(years).append(" ").append(
						TCUtilities.getLocalizedString("tc.years.text"));
			}
			if (months!=null && (years==null || years<=0 || months>0)) {
				if (sbuilder.length()>0) {
					sbuilder.append(", ");
				}
				sbuilder.append(months).append(" ").append(
						TCUtilities.getLocalizedString("tc.months.text"));
			}
			
			return sbuilder.toString();
		}
		return "";
	}
	
	public static Integer toDays(Integer years, Integer months) {
		Integer days = null;
		if (years!=null) {
			days = years * 365;
		}
		if (months!=null) {
			if (days==null) {
				days = 0;
			}
			days += months*30;
		}
		return days;
	}
		
	public static Integer toYears(Integer ageInDays) {
		if (ageInDays!=null) {
			return Math.max(ageInDays / 365, 0);
		}
		return null;
	}
	
	public static Integer toRemainingMonths(Integer ageInDays) {
		Integer years = toYears(ageInDays);
		if (ageInDays!=null && years!=null) {
			return Math.max((ageInDays-365*years) / 30,0);
		}
		return null;
	}
	
}
