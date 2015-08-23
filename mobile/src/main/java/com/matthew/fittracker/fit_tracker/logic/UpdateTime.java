package com.matthew.fittracker.fit_tracker.logic;

/**
 * Created by dalton on 18/08/2015.
 *
 *	The procedure takes the time in milliseconds and
 *	splits in into hours, minutes, seconds
 *	Since we want the time in a consistent
 * 	format like 00:00:00 we check the hours, minutes and seconds to see
 * 	if they are less than 10, if they are we add an extra zero to the front of the number,
 * 	so it would display as 05 instead of just 5.
 *
 * 	The modulo function (%) is used to remove any remainders of the hour, minute or second.
 * 	If we have 2.5 minutes elapsed we want the minute portion of the string to be 2 not 2.5, the .5 will be converted to 30 seconds
 *  and represented in the seconds portion of the string.
 */
public class UpdateTime {

    private long secs, mins, hrs;
    private String seconds, minutes, hours;
    public UpdateTime() {}

    public void updateTimer(float time){
        this.secs = (long)(time/1000);
        this.mins = (long)((time/1000)/60);
        this.hrs = (long)(((time/1000)/60)/60);
    	/* Convert the seconds to String
		 * and format to ensure it has
		 * a leading zero when required
		 */
        secs = secs % 60;
        seconds=String.valueOf(secs);
        if(secs == 0){
            this.seconds = "00";
        }
        if(secs < 10 && secs > 0){
            this.seconds = "0"+seconds;
        }
    	/* Convert the minutes to String and format the String */
        mins = mins % 60;
        minutes=String.valueOf(mins);
        if(mins == 0){
            this.minutes = "00";
        }
        if(mins <10 && mins > 0){
            this.minutes = "0"+minutes;
        }
    	/* Convert the hours to String and format the String */
        hours=String.valueOf(hrs);
        if(hrs == 0){
            this.hours = "00";
        }
        if(hrs <10 && hrs > 0){
            this.hours = "0"+hours;
        }
    }
    public String getSeconds(){
        return seconds;
    }
    public String getMinutes(){
        return minutes;
    }
    public String getHours(){
        return hours;
    }
}
